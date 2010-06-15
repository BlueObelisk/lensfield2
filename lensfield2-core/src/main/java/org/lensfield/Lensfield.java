/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import org.lensfield.build.FileList;
import org.lensfield.build.InputDescription;
import org.lensfield.build.OutputDescription;
import org.lensfield.build.ParameterDescription;
import org.lensfield.glob.GlobAnalyser;
import org.lensfield.glob.Template;
import org.lensfield.log.BuildLogger;
import org.lensfield.log.BuildStateReader;
import org.lensfield.model.*;
import org.lensfield.model.Process;
import org.lensfield.ops.FileSource;
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


    public Lensfield(Model model) {
        this.model = model;
    }

    public Lensfield(Model model, File root) {
        this.model = model;
        this.root = root;
    }


    public File getRoot() {
        return root;
    }

    public void setRoot(File root) {
        this.root = root;
    }


    public synchronized void build() throws Exception {

        checkBuildStepsExist();
        List<Process> buildOrder = resolveBuildOrder();

        initBuildState();
        resolveDependencies();

        initWorkspace();

        analyseBuildState();
        loadPreviousBuildState();
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


    protected void initWorkspace() throws IOException {
        LOG.info("Initialising workspace");
        this.workspace = new File(root, ".lf");
        if (!workspace.isDirectory()) {
            if (!workspace.mkdirs()) {
                throw new IOException("Unable to create workspace: "+workspace);
            }
        }
        this.tmpdir = new File(workspace, "tmp");
        if (!tmpdir.isDirectory()) {
            if (!tmpdir.mkdirs()) {
                throw new IOException("Unable to create tmpdir: "+tmpdir);
            }
        }
    }
    

    protected ArrayList<Process> resolveBuildOrder() throws LensfieldException {
        LOG.info("Resolving build order");
        LinkedHashSet<Process> order = new LinkedHashSet<Process>(model.getSources());
        if (order.isEmpty()) {
            throw new LensfieldException("No source build steps");
        }

        for (Process proc : order) {
            LOG.debug(" - "+proc.getName());
        }

        int processCount = model.getProcesses().size();
        while (order.size() < processCount) {
            List<Process> nextSteps = findNextSteps(order);
            if (nextSteps.isEmpty()) {
                throw new LensfieldException("Unable to resolve build order; cyclic dependencies");
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


    private void initBuildLog() throws FileNotFoundException, LensfieldException {
        LOG.info("Starting build log");
        File logFile = new File(workspace, "log.txt");
        if (logFile.exists()) {
            String ts = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(logFile.lastModified()));
            File backup = new File(workspace, "log-"+ts+".txt");
            if (!logFile.renameTo(backup)) {
                throw new LensfieldException("Unable to move previous log"+backup);
            }
        }
        buildLog = new BuildLogger(new FileOutputStream(logFile));
        buildLog.startBuild();
        buildLog.recordTasks(buildState);
    }


    protected synchronized void build(Process step) throws Exception {

        LOG.info("Running process: "+step.getName());
        
        if (step instanceof Source) {
            processSource((Source)step);
        }
        else if (step instanceof Build) {
            build((Build)step);
        }
        else {
            throw new RuntimeException();
        }

    }


    private void processSource(Source source) throws Exception {

        if (source.getParameters().isEmpty()) {
            throw new LensfieldException("Source filter not defined");
        }

        String filter;
        if (source.getParameters().size() == 1) {
            Parameter param = source.getParameters().get(0);
            if (param.getName() == null || "filter".equals(param.getName())) {
                filter = param.getValue();
            } else {
                throw new LensfieldException();
            }
        } else {
            // TODO
            throw new LensfieldException();
        }

        FileSource finder = new FileSource(source.getName(), root, filter);
        finder.setLogger(LOG);
        FileList files = finder.run();

        buildLog.process(source.getName(), files.getFiles());

        stateFileLists.put(source.getName(), files);
        buildState.addFiles(files.getFiles());
        
    }


    private void build(Build build) throws Exception {
        checkInputsExist(build);

        TaskState task = buildState.getTask(build.getName());

        if (task.isKtoL()) {
            runKtoLStep(build, task);
        } else if (task.isNtoK()) {
            runNtoKStep(build, task);
        } else if (task.isKtoN()) {
            runKtoNStep(build, task);
        } else {
            throw new LensfieldException("Unsupported operation");
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

        Map<String,FileList> inputFileLists = getKtoLInputs(build, task);
        List<InputFileSet> inputSets = GlobAnalyser.getInputFileSets(inputFileLists);

        Map<String, FileList> outputFileLists = getOutputFileLists(build, task);

        TaskState prevTask = null;
        if (!task.isUpdated()) {
            if (prevBuildState != null) {
                prevTask = prevBuildState.getTask(task.getId());
            }
        }

        ProcessRunner procBuilder = new ProcessRunner(task);
        procBuilder.setTmpdir(tmpdir);
        procBuilder.setRoot(root);
        procBuilder.setLogger(new TaskLogger(task.getId(), LOG));
        procBuilder.setBuildLog(buildLog);
        
        for (InputFileSet input : inputSets) {

            Operation op = new Operation(build.getName(), input.getMap(), null);

            // Check if up-to-date
            if (!task.isUpdated() && prevTask != null) {
                String fn = input.getMap().values().iterator().next().get(0).getPath();
                Operation prevOp = prevTask.getInputOperationMap().get(fn);
                if (prevOp != null) {
                    if (isUpToDate(input.getMap(), prevOp)) {
                        buildLog.process(task.getId(), input.getMap(), prevOp.getOutputFiles());
                        for (Map.Entry<String,FileList> e : outputFileLists.entrySet()) {
                            String name = e.getKey();
                            FileList output = e.getValue();
                            for (FileState fs : prevOp.getOutputFiles().get(name)) {
                                output.addFile(fs);
                            }
                        }
                        continue;
                    }
                }
            }

            procBuilder.runProcess(input, outputFileLists);
        }

        for (Map.Entry<String,FileList> e : outputFileLists.entrySet()) {
            String name = e.getKey();
            FileList files = e.getValue();
            stateFileLists.put(build.getName() + (outputFileLists.size()==1?"":"/"+name), files);
        }

    }

    private boolean isUpToDate(Map<String,List<FileState>> input, Operation prevOp) {
        // Check input names match
        if (!input.keySet().equals(prevOp.getInputFiles().keySet())) {
//            System.err.println("input name mis-match. current:"+input.keySet()+"; prev:"+prevOp.getInputFiles().keySet());
            return false;
        }
        // Check input files up-to-date
        for (Map.Entry<String,List<FileState>> e : input.entrySet()) {
            List<FileState> files = e.getValue();
            List<FileState> prevFiles = prevOp.getInputFiles().get(e.getKey());
            if (files.size() != prevFiles.size()) {
//                System.err.println("input file count mis-match. current:"+files.size()+"; prev:"+prevFiles.size());
                return false;
            }
            for (int i = 0; i < files.size(); i++) {
                if (!files.get(i).getPath().equals(prevFiles.get(i).getPath())) {
//                    System.err.println("input file name mis-match. current:"+files.get(i).getPath()+"; prev:"+prevFiles.get(i).getPath());
                    return false;
                }
                if (isChanged(files.get(i).getLastModified(), prevFiles.get(i).getLastModified())) {
//                    System.err.println("input file age mis-match. current:"+files.get(i).getLastModified()+"; prev:"+prevFiles.get(i).getLastModified());
                    return false;
                }
            }
        }
        // Check output files
        for (List<FileState> fileStates : prevOp.getOutputFiles().values()) {
            for (FileState fs : fileStates) {
                File f = new File(root, fs.getPath());
                if (!f.isFile()) {
//                    System.err.println("output file missing: "+fs.getPath());
                    return false;
                }
                if (isChanged(fs.getLastModified(), f.lastModified())) {
//                    System.err.println("output file age mismatch. existing:"+f.lastModified()+"; prev:"+fs.getLastModified());
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


    private Map<String, FileList> getKtoLInputs(Build build, TaskState task) throws LensfieldException {
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


    private void runNtoKStep(Build build, TaskState task) throws Exception {

        Map<String, List<FileState>> inputs = getNtoKInputStates(build, task);
        Map<String, FileList> outputFileLists = getOutputFileLists(build, task);

        // TODO grouped input/outputs
        if (!task.isUpdated() && prevBuildState != null) {
            TaskState prevTask = prevBuildState.getTask(task.getId());
            if (prevTask != null) {
                String fn = inputs.values().iterator().next().get(0).getPath();
                Operation prevOp = prevTask.getInputOperationMap().get(fn);
                if (prevOp != null) {
                    if (isUpToDate(inputs, prevOp)) {
                        buildLog.process(task.getId(), inputs, prevOp.getOutputFiles());
                        for (Map.Entry<String, List<FileState>> e : prevOp.getOutputFiles().entrySet()) {
                            String name = e.getKey();
                            List<FileState> files = e.getValue();
                            FileList fileList = outputFileLists.get(name);
                            for (FileState f : files) {
                                fileList.addFile(f);
                            }
                            stateFileLists.put(build.getName()+(outputFileLists.size()==1?"":"/"+name), fileList);
                        }
                        return;
                    }
                }
            }
        }

        ProcessRunner procBuilder = new ProcessRunner(task);
        procBuilder.setTmpdir(tmpdir);
        procBuilder.setRoot(root);
        procBuilder.setLogger(new TaskLogger(task.getId(), LOG));
        procBuilder.setBuildLog(buildLog);

        InputFileSet inputSet = new InputFileSet(Collections.<String,String>emptyMap(), inputs);
        procBuilder.runProcess(inputSet, outputFileLists);

        for (Map.Entry<String,FileList> e : outputFileLists.entrySet()) {
            String name = e.getKey();
            FileList files = e.getValue();
            stateFileLists.put(build.getName() + (outputFileLists.size()==1?"":"/"+name), files);
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



    private void runKtoNStep(Build build, TaskState task) throws Exception {

        Map<String, FileList> inputFileLists = getKtoLInputs(build, task);
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

        TaskState prevTask = null;
        if (!task.isUpdated()) {
            if (prevBuildState != null) {
                prevTask = prevBuildState.getTask(task.getId());
                // Check if up-to-date
            }
        }

        ProcessRunner procBuilder = new ProcessRunner(task);
        procBuilder.setTmpdir(tmpdir);
        procBuilder.setRoot(root);
        procBuilder.setLogger(new TaskLogger(task.getId(), LOG));
        procBuilder.setBuildLog(buildLog);

        for (InputFileSet input : inputSets) {

            if (!task.isUpdated() && prevTask != null) {
                String fn = input.getMap().values().iterator().next().get(0).getPath();
                Operation prevOp = prevTask.getInputOperationMap().get(fn);
                if (prevOp != null) {
                    if (isUpToDate(input.getMap(), prevOp)) {
                        buildLog.process(task.getId(), input.getMap(), prevOp.getOutputFiles());
                        for (Map.Entry<String,FileList> e : outputs.entrySet()) {
                            String name = e.getKey();
                            FileList output = e.getValue();
                            for (FileState fs : prevOp.getOutputFiles().get(name)) {
                                output.addFile(fs);
                            }
                        }
                        continue;
                    }
                }
            }

            procBuilder.runProcess(input, outputs);

        }

        for (Map.Entry<String,FileList> e : outputs.entrySet()) {
            String name = e.getKey();
            FileList files = e.getValue();
            stateFileLists.put(build.getName() + (outputs.size()==1?"":"/"+name), files);
        }

    }


    protected void checkBuildStepsExist() throws LensfieldException {
        LOG.info("Checking build steps");
        for (Build step : model.getBuilds()) {
            for (Input input : step.getInputs()) {
                if (model.getProcess(input.getStep()) == null) {
                    throw new LensfieldException("Missing build step: "+step.getName()+"("+input.getStep()+")");
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
