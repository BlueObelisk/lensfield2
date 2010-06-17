/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.lensfield.api.Logger;
import org.lensfield.build.FileList;
import org.lensfield.build.InputDescription;
import org.lensfield.build.OutputDescription;
import org.lensfield.build.ParameterDescription;
import org.lensfield.glob.GlobAnalyser;
import org.lensfield.glob.Template;
import org.lensfield.io.FileSource;
import org.lensfield.io.OutputFile;
import org.lensfield.log.BuildLogger;
import org.lensfield.log.BuildStateReader;
import org.lensfield.model.*;
import org.lensfield.model.Process;
import org.lensfield.process.ProcessRunner;
import org.lensfield.state.*;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author sea36
 */
public class Lensfield {

    private final Logger LOG = new LoggerImpl();

    private BuildState buildState;
    private BuildState prevBuildState;
    private BuildLogger buildLog;

    private Map<String, FileList> stateFileLists = new HashMap<String, FileList>();

    private Model model;
    private File root = new File(".");
    private File workspace, tmpdir;

    private ArrayList<Process> buildOrder;


    public Lensfield(Model model, File root) {
        this.model = model;
        this.root = root;
    }

//    public Lensfield(Model model, File root, ClassWorld classworld) {
//        this.model = model;
//        this.root = root;
//    }
//
//    private static ClassWorld initClassWorld() {
//        ClassWorld classWorld = new ClassWorld();
//        try {
//            ClassRealm core = classWorld.newRealm("lensfield.core", Thread.currentThread().getContextClassLoader());
//            classWorld.newRealm("lensfield.api", core);
//            classWorld.newRealm("plexus.core", core);
//        } catch (DuplicateRealmException e) {
//            // Impossible!
//            throw new RuntimeException(e);
//        }
//        return classWorld;
//    }


    public File getRoot() {
        return root;
    }

    private void init() throws Exception {
        checkBuildStepsExist();
        buildOrder = resolveBuildOrder();

        initBuildState();
        resolveDependencies();

        initWorkspace();

        analyseBuildState();
        checkBuildState();
        loadPreviousBuildState();
    }


    public synchronized void clean() throws Exception {
        init();
        
        if (prevBuildState == null) {
            throw new LensfieldException("No previous build state");
        }
        for (TaskState task : prevBuildState.getTasks()) {
            if (task.isSource()) {
                continue;
            }
            for (Operation op : task.getOperations()) {
                for (List<FileState> outs : op.getOutputFiles().values()) {
                    for (FileState out : outs) {
                        String path = out.getPath();
                        File f = new File(root, path);
                        if (f.isFile()) {
                            System.err.println("deleting "+path);
                            if (!f.delete()) {
                                System.err.println("Error deleting: "+path);
                            }
                        }
                        // TODO delete directories
                    }
                }
            }
        }
    }
    
    public synchronized void build() throws Exception {

        init();
        comparePreviousBuildState();

        try {
            initBuildLog();
            processBuildSteps(buildOrder);
            buildLog.finishBuild();

        } finally {
            if (buildLog != null) {
                buildLog.close();
            }
        }

    }

    private void checkBuildState() throws ConfigurationException {
        checkParameters();
    }

    private void checkParameters() throws ConfigurationException {
        for (TaskState task : buildState.getTasks()) {
            for (ParameterDescription param : task.getParameters()) {
                if (param.isRequired() && param.value == null) {
                    throw new ConfigurationException("Missing parameter '"+param.name+"' on task '"+task.getId()+"'");
                }
            }
        }
    }


    protected void initWorkspace() throws IOException {
        LOG.info("Initialising workspace");
        this.workspace = new File(root, ".lf");
        if (!workspace.isDirectory()) {
            if (!workspace.mkdirs()) {
                throw new IOException("Unable to create workspace directory: "+workspace.getPath());
            }
        }
        this.tmpdir = new File(workspace, "tmp");
        if (!tmpdir.isDirectory()) {
            if (!tmpdir.mkdirs()) {
                throw new IOException("Unable to create temp directory: "+tmpdir.getPath());
            }
        }
    }
    

    protected ArrayList<Process> resolveBuildOrder() throws LensfieldException {
        LOG.info("Resolving build order");
        LinkedHashSet<Process> order = new LinkedHashSet<Process>(model.getSources());
        if (order.isEmpty()) {
            throw new ConfigurationException("No source build steps");
        }

        for (Process proc : order) {
            LOG.debug(" - "+proc.getName());
        }

        int processCount = model.getProcesses().size();
        while (order.size() < processCount) {
            List<Process> nextSteps = findNextSteps(order);
            if (nextSteps.isEmpty()) {
                throw new ConfigurationException("Unable to resolve build order; cyclic dependencies");
            }
            for (Process proc : nextSteps) {
                LOG.debug(" - "+proc.getName());
            }
            order.addAll(nextSteps);
        }

        return new ArrayList<Process>(order);
    }

    private List<Process> findNextSteps(Collection<Process> order) {
        List<Process> nextSteps = new ArrayList<Process>();
        for (Build step : model.getBuilds()) {
            if (!order.contains(step) && containsAllSteps(order, step.getInputs())) {
                nextSteps.add(step);
            }
        }
        return nextSteps;
    }

    private boolean containsAllSteps(Collection<Process> order, List<Input> inputs) {
        for (Input input : inputs) {
            if (!order.contains(model.getProcess(input.getStep()))) {
                return false;
            }
        }
        return true;
    }


    private void loadPreviousBuildState() throws IOException, ParseException {
        File logFile = new File(workspace, "log.txt");
        if (logFile.isFile()) {
            LOG.info("Loading last build log");
            BuildStateReader in = new BuildStateReader();
            Reader r = new BufferedReader(new FileReader(logFile));
            try {
                prevBuildState = in.parseBuildState(r);
            } finally {
                r.close();
            }
        } else {
            LOG.info("Previous build log not found");
        }
    }

    private void comparePreviousBuildState() {
        if (prevBuildState == null) {
            return;
        }
        LOG.info("Comparing tasks to previous state");
        for (TaskState current : buildState.getTasks()) {
            TaskState old = prevBuildState.getTask(current.getId());
            if (old != null) {
                if (isTaskUnchanged(current, old)) {
                    LOG.debug("Task "+current.getId()+": up-to-date");
                    current.setUpdated(false);
                    current.setLastModified(old.getLastModified());
                }
            } else {
                LOG.debug("Task "+current.getId()+": new");
            }
        }
    }

    private boolean isTaskUnchanged(TaskState current, TaskState old) {
        if (areDependenciesChanged(current,old)) {
            LOG.debug("Task "+current.getId()+ ": dependencies updated");
            return false;
        }
        if (areParametersChanged(current, old)) {
            LOG.debug("Task "+current.getId()+ ": parameters updated");
            return false;
        }
        return true;
    }

    private boolean areParametersChanged(TaskState current, TaskState old) {
        for (ParameterDescription param : current.getParameters()) {
            ParameterDescription oldParam = old.getParameter(param.name);
            if (oldParam == null || !param.value.equals(oldParam.value)) {
                return true;
            }
        }
        return false;
    }

    private boolean areDependenciesChanged(TaskState current, TaskState old) {
        List<DependencyState> currentDependencies = current.getDependencyList();
        List<DependencyState> oldDependencies = old.getDependencyList();
        if (currentDependencies.size() != oldDependencies.size()) {
            return true;
        }
        for (int i = 0; i < currentDependencies.size(); i++) {
            if (!currentDependencies.get(i).getId().equals(oldDependencies.get(i).getId())) {
                return true;
            }
            if (currentDependencies.get(i).getLastModified() != oldDependencies.get(i).getLastModified()) {
                return true;
            }
        }
        return false;
    }


    /**
     * Registers task for each build step.
     */
    private void initBuildState() {
        buildState = new BuildState();
        for (Process process : model.getProcesses()) {
            TaskState task = new TaskState(process.getName());
            if (process instanceof Build) {
                task.setClassName(((Build)process).getClassName());
            }
            buildState.addTask(task);
        }
    }

    /**
     * Analyses build state classes
     */
    private void analyseBuildState() throws Exception {
        for (Build build : model.getBuilds()) {
            TaskState task = buildState.getTask(build.getName());
            ClassAnalyser.analyseClass(build, task);
        }
    }


    private void initBuildLog() throws IOException, LensfieldException {
        LOG.info("Starting build log");
        File logFile = new File(workspace, "log.txt");
        if (logFile.isFile()) {
            String ts = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(logFile.lastModified()));
            File backup = new File(workspace, "log-"+ts+".txt");
            if (!logFile.renameTo(backup)) {
                throw new IOException("Unable to move previous log"+backup);
            }
        }
        buildLog = new BuildLogger(new FileOutputStream(logFile));
        buildLog.startBuild();
        buildLog.recordTasks(buildState);
    }


    protected synchronized void build(Process step) throws Exception {

        if (step instanceof Source) {
            processSource((Source)step);
        }
        else if (step instanceof Build) {
            processBuildStep((Build)step);
        }
        else {
            throw new LensfieldException("Unknown process: "+step.getName()+" ["+step.getClass().getName()+"]");
        }

    }


    private void processSource(Source source) throws Exception {

        LOG.info("Processing source: "+source.getName());

        if (source.getParameters().isEmpty()) {
            throw new ConfigurationException("Source filter not defined");
        }

        String filter;
        if (source.getParameters().size() == 1) {
            Parameter param = source.getParameters().get(0);
            if (param.getName() == null || "filter".equals(param.getName())) {
                filter = param.getValue();
            } else {
                throw new ConfigurationException("Source '"+source.getName()+"' has unknown parameter: "+param.getName());
            }
        } else {
            // TODO
            throw new ConfigurationException("Source '"+source.getName()+"'; expected 1 parameter, found "+source.getParameters().size());
        }

        FileSource finder = new FileSource(source.getName(), root, filter);
        finder.setLogger(LOG);
        FileList files = finder.run();

        buildLog.process(source.getName(), files.getFiles());

        stateFileLists.put(source.getName(), files);
        buildState.addFiles(files.getFiles());
        
    }


    private void processBuildStep(Build build) throws Exception {

        LOG.info("Processing build step: "+build.getName());

        checkInputsExist(build);

        TaskState task = buildState.getTask(build.getName());

        if (task.isKtoL()) {
            runKtoLStep(build, task);
        } else if (task.isNtoK()) {
            runNtoKStep(build, task);
        } else if (task.isKtoN()) {
            runKtoNStep(build, task);
        } else {
            throw new ConfigurationException("Unsupported operation: "+task.getId());
        }
        
    }

    private void checkInputsExist(Build step) throws LensfieldException {
        for (Input input : step.getInputs()) {
            if (!stateFileLists.containsKey(input.getStep())) {
                throw new LensfieldException("Dependency not run: "+input.getStep());
            }
        }
    }


    private void runKtoLStep(Build build, TaskState task) throws Exception {

        Map<String,FileList> inputFileLists = getInputs(build, task);
        List<InputFileSet> inputSets = GlobAnalyser.getInputFileSets(inputFileLists);

        Map<String, FileList> outputFileLists = getOutputFileLists(build, task);

        run(task, inputSets, outputFileLists);

    }



    private boolean isUpToDate(Map<String,List<FileState>> input, Operation prevOp) {
        // Check input names match
        if (!input.keySet().equals(prevOp.getInputFiles().keySet())) {
            System.err.println("[DEBUG] input name mis-match. current:"+input.keySet()+"; prev:"+prevOp.getInputFiles().keySet());
            return false;
        }
        // Check input files up-to-date
        for (Map.Entry<String,List<FileState>> e : input.entrySet()) {
            List<FileState> files = e.getValue();
            List<FileState> prevFiles = prevOp.getInputFiles().get(e.getKey());
            if (files.size() != prevFiles.size()) {
                System.err.println("[DEBUG] input file count mis-match. current:"+files.size()+"; prev:"+prevFiles.size());
                return false;
            }
            for (int i = 0; i < files.size(); i++) {
                if (!files.get(i).getPath().equals(prevFiles.get(i).getPath())) {
                    System.err.println("[DEBUG] input file name mis-match. current:"+files.get(i).getPath()+"; prev:"+prevFiles.get(i).getPath());
                    return false;
                }
                if (isChanged(files.get(i).getLastModified(), prevFiles.get(i).getLastModified())) {
                    System.err.println("[DEBUG] input file age mis-match. current:"+files.get(i).getLastModified()+"; prev:"+prevFiles.get(i).getLastModified());
                    return false;
                }
            }
        }
        // Check output files
        for (List<FileState> fileStates : prevOp.getOutputFiles().values()) {
            for (FileState fs : fileStates) {
                File f = new File(root, fs.getPath());
                if (!f.isFile()) {
                    System.err.println("[DEBUG] output file missing: "+fs.getPath());
                    return false;
                }
                if (isChanged(fs.getLastModified(), f.lastModified())) {
                    System.err.println("[DEBUG] output file age mismatch. existing:"+f.lastModified()+"; prev:"+fs.getLastModified());
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isChanged(long lastModified0, long lastModified1) {
        return Math.abs(lastModified0-lastModified1) >= 1000;
    }

    private Map<String, FileList> getOutputFileLists(Build build, TaskState task) throws LensfieldException {
        Map<String,FileList> outputFileLists = new HashMap<String, FileList>();
        for (Output output : build.getOutputs()) {
            String name = output.getName();
            if (name == null) {
                OutputDescription outd = task.getDefaultOutput();
                if (outd == null) {
                    throw new LensfieldException();
                }
                name = outd.name;
            }
            outputFileLists.put(name, new FileList(new Template(output.getValue())));
        }
        return outputFileLists;
    }


    private Map<String, FileList> getInputs(Build build, TaskState task) throws LensfieldException {
        Map<String,FileList> map = new HashMap<String, FileList>();
        for (Input input : build.getInputs()) {
            String name = input.getName();
            if (name == null) {
                InputDescription inpd = task.getDefaultInput();
                if (inpd == null) {
                    throw new LensfieldException();
                }
                name = inpd.name;
            }
            map.put(name, stateFileLists.get(input.getStep()));
        }
        return map;
    }


    private void runKtoNStep(Build build, TaskState task) throws Exception {

        Map<String, FileList> inputFileLists = getInputs(build, task);
        List<InputFileSet> inputSets = GlobAnalyser.getInputFileSets(inputFileLists);

        // TODO no inputs sets; empty output

        Map<String,FileList> outputs = new HashMap<String,FileList>();
        for (Output output : build.getOutputs()) {
            String name = output.getName();
            if (name == null) {
                OutputDescription outd = task.getDefaultOutput();
                if (outd == null) {
                    throw new LensfieldException();
                }
                name = outd.name;
            }
            outputs.put(name, new FileList(new Template(output.getValue())));
        }

        run(task, inputSets, outputs);

    }


    private void runNtoKStep(Build build, TaskState task) throws Exception {

        Map<String, FileList> inputFileLists = getInputs(build, task);
        Map<String, FileList> outputFileLists = getOutputFileLists(build, task);

        Map<String,List<FileState>> inputMap = new HashMap<String, List<FileState>>();
        for (Map.Entry<String,FileList> e : inputFileLists.entrySet()) {
            inputMap.put(e.getKey(), e.getValue().getFiles());
        }

        InputFileSet inputs = new InputFileSet(Collections.<String, String>emptyMap(), inputMap);

        run(task, Collections.singletonList(inputs), outputFileLists);
    }

    private void run(TaskState task, List<InputFileSet> inputList, Map<String, FileList> outputFileLists) throws Exception {

        ProcessRunner procBuilder = new ProcessRunner(task);
        procBuilder.setTmpdir(tmpdir);
        procBuilder.setRoot(root);
        procBuilder.setLogger(new TaskLogger(task.getId(), LOG));
        procBuilder.setBuildLog(buildLog);

        TaskState prevTask = null;
        if (!task.isUpdated()) {
            if (prevBuildState != null) {
                prevTask = prevBuildState.getTask(task.getId());
            }
        }

        for (InputFileSet inputs : inputList) {
            // TODO grouped input/outputs
            if (prevTask != null) {
                // Get sample filename
                String fn = inputs.getMap().values().iterator().next().get(0).getPath();
                // Get relevant operation
                Operation prevOp = prevTask.getInputOperationMap().get(fn);
                if (prevOp != null) {
                    if (isUpToDate(inputs.getMap(), prevOp)) {
                        buildLog.process(task.getId(), inputs.getMap(), prevOp.getOutputFiles());
                        for (Map.Entry<String, List<FileState>> e : prevOp.getOutputFiles().entrySet()) {
                            String name = e.getKey();
                            List<FileState> files = e.getValue();
                            FileList fileList = outputFileLists.get(name);
                            for (FileState f : files) {
                                fileList.addFile(f);
                            }
                            stateFileLists.put(task.getId()+(outputFileLists.size()==1?"":"/"+name), fileList);
                        }
                        continue;
                    }
                }
            }

            procBuilder.runProcess(inputs, outputFileLists);

        }



        for (Map.Entry<String,FileList> e : outputFileLists.entrySet()) {
            String name = e.getKey();
            FileList files = e.getValue();
            stateFileLists.put(task.getId() + (outputFileLists.size()==1?"":"/"+name), files);
        }

    }

    private Map<String, List<FileState>> getNtoKInputStates(Build build, TaskState description) throws LensfieldException {
        Map<String,List<FileState>> inputs = new HashMap<String, List<FileState>>();
        for (Input input : build.getInputs()) {
            String name = input.getName();
            if (name == null) {
                InputDescription inpd = description.getDefaultInput();
                if (inpd == null) {
                    throw new LensfieldException();
                }
                name = inpd.name;
            }
            inputs.put(name, stateFileLists.get(input.getStep()).getFiles());
        }
        return inputs;
    }



    protected void checkBuildStepsExist() throws LensfieldException {
        LOG.info("Checking build steps");
        for (Build step : model.getBuilds()) {
            for (Input input : step.getInputs()) {
                if (model.getProcess(input.getStep()) == null) {
                    throw new ConfigurationException("Undefined input '"+input.getName()+"' on step '"+step.getName()+"'");
                }
            }
        }
    }


    private void resolveDependencies() throws Exception {
        LOG.info("Resolving dependencies");

        System.setProperty("maven.artifact.threads", "1");  // Prevents hanging threads

        DependencyResolver resolver = new DependencyResolver(model.getRepositories());
        resolver.configureDependencies(model, buildState);
    }


    private void processBuildSteps(List<Process> buildOrder) throws Exception {
        for (Process step: buildOrder) {
            build(step);
        }
    }
    
}
