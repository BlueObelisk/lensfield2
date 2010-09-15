/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import com.google.common.collect.Sets;
import org.lensfield.api.Logger;
import org.lensfield.build.FileList;
import org.lensfield.build.InputDescription;
import org.lensfield.build.OutputDescription;
import org.lensfield.build.ParameterDescription;
import org.lensfield.glob.Glob;
import org.lensfield.glob.GlobAnalyser;
import org.lensfield.log.BuildLogger;
import org.lensfield.log.BuildStateReader;
import org.lensfield.model.*;
import org.lensfield.model.Process;
import org.lensfield.process.ProcessRunner;
import org.lensfield.source.FileSource;
import org.lensfield.source.ISource;
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

    private boolean offline;

    private ArrayList<Process> buildOrder;


    public Lensfield(Model model, File root) {
        this.model = model;
        this.root = root;
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
                if (param.isRequired() && param.getValue() == null) {
                    throw new ConfigurationException("Missing parameter '"+param.getName()+"' on task '"+task.getId()+"'");
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
            ParameterDescription oldParam = old.getParameter(param.getName());
            if (oldParam == null || !param.getValue().equals(oldParam.getValue())) {
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
                task.setClassName(process.getClassName());
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
            configureOutputs(task, build);
        }
    }

    private void configureOutputs(TaskState task, Build build) {
        for (Output output : build.getOutputs()) {
            OutputDescription outputDescription;
            if (output.getName() == null) {
                outputDescription = task.getDefaultOutput();
            }
            else {
                outputDescription = task.getOutput(output.getName());
            }
            if (outputDescription == null) {
                // TODO
                throw new NullPointerException();
            }
            outputDescription.setGlob(new Glob(output.getValue()));
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

        Glob glob = new Glob(source.getTemplate());
        TaskState task = new TaskState(source.getName());
        OutputDescription output = new OutputDescription(task, "out");
        task.addOutput(output);

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            ISource finder;
            if (source.getClassName() == null) {
                finder = new FileSource();
            } else {
                DependencyResolver dr = new DependencyResolver(model.getRepositories(), getClass().getClassLoader());
                ClassLoader cloader = dr.createClassLoader(source.getDependencies());
                Thread.currentThread().setContextClassLoader(cloader);
                System.err.println("LOADING CLASS: "+source.getClassName());
                Class<?> c = cloader.loadClass(source.getClassName());
                System.err.println("----------");
                finder = (ISource) c.newInstance();
            }

            finder.setLogger(LOG);
            finder.setName(task.getId());
            finder.setRoot(root);
            finder.setGlob(glob);
            finder.configure(source.getParameters());
            List<FileState> fileList = finder.run();
            FileList files = new FileList(glob);
            files.addFiles(fileList);

            buildLog.process(source.getName(), files.getFiles());

            stateFileLists.put(source.getName(), files);
            buildState.addFiles(files.getFiles());

        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }


    private void processBuildStep(Build build) throws Exception {

        LOG.info("Processing build step: "+build.getName());

        checkInputsExist(build);

        TaskState task = buildState.getTask(build.getName());

        List<InputFileSet> inputSets ;
        Map<String, FileList> outputFileLists = getOutputFileLists(build, task);
        if (task.isKtoL() || task.isKtoN()) {
            // TODO no inputs sets; empty output
            Map<String, FileList> inputFileLists = getInputs(build, task);
            inputSets = GlobAnalyser.getInputFileSets(inputFileLists);
        } else {
            Map<String, FileList> inputFileLists = getInputs(build, task);
            Set<String> inputGroups = GlobAnalyser.getCommonGroups(getGlobs(inputFileLists.values()));
            Set<String> outputGroups = GlobAnalyser.getCommonGroups(getGlobs(outputFileLists.values()));
            Set<String> commonGroups = Sets.intersection(inputGroups, outputGroups);
            inputSets = GlobAnalyser.getInputFileSets(inputFileLists, commonGroups);
        }
        run(task, inputSets, outputFileLists);
    }

    private void checkInputsExist(Build step) throws LensfieldException {
        for (Input input : step.getInputs()) {
            if (!stateFileLists.containsKey(input.getStep())) {
                throw new LensfieldException("Dependency not run: "+input.getStep());
            }
        }
    }


    private boolean isUpToDate(Map<String,Collection<FileState>> input, Operation prevOp) {
        // Check input names match
        if (!input.keySet().equals(prevOp.getInputFiles().keySet())) {
            System.err.println("[DEBUG] input name mis-match. current:"+input.keySet()+"; prev:"+prevOp.getInputFiles().keySet());
            return false;
        }
        // Check input files up-to-date
        for (Map.Entry<String,Collection<FileState>> e : input.entrySet()) {
            Collection<FileState> files = e.getValue();
            List<FileState> prevFiles = prevOp.getInputFiles().get(e.getKey());
            if (files.size() != prevFiles.size()) {
                System.err.println("[DEBUG] input file count mis-match. current:"+files.size()+"; prev:"+prevFiles.size());
                return false;
            }
            Iterator<FileState> fit = files.iterator();
            Iterator<FileState> pit = prevFiles.iterator();
            while (fit.hasNext()) {
                FileState f = fit.next();
                FileState p = pit.next();
                if (!f.getPath().equals(p.getPath())) {
                    System.err.println("[DEBUG] input file name mis-match. current:"+f.getPath()+"; prev:"+p.getPath());
                    return false;
                }
                if (isChanged(f.getLastModified(), p.getLastModified())) {
                    System.err.println("[DEBUG] input file age mis-match. current:"+f.getLastModified()+"; prev:"+p.getLastModified());
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
                name = outd.getName();
            }
            outputFileLists.put(name, new FileList(new Glob(output.getValue())));
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
                name = inpd.getName();
            }
            map.put(name, stateFileLists.get(input.getStep()));
        }
        return map;
    }


    private Glob[] getGlobs(Collection<FileList> fileLists) {
        Glob[] globs = new Glob[fileLists.size()];
        int i = 0;
        for (FileList fl : fileLists) {
            globs[i++] = fl.getGlob();
        }
        return globs;
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
                String fn = inputs.getMap().values().iterator().next().iterator().next().getPath();
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
        resolver.setOffline(offline);
        resolver.configureDependencies(model, buildState);
    }


    private void processBuildSteps(List<Process> buildOrder) throws Exception {
        for (Process step: buildOrder) {
            build(step);
        }
    }


    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

}
