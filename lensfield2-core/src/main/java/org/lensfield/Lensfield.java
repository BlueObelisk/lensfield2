/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import org.apache.log4j.Logger;
import org.lensfield.concurrent.Reactor;
import org.lensfield.concurrent.Resource;
import org.lensfield.glob.Glob;
import org.lensfield.log.*;
import org.lensfield.model.BuildStep;
import org.lensfield.model.Model;
import org.lensfield.model.Source;
import org.lensfield.source.FileSource;
import org.lensfield.state.*;
import org.lensfield.state.Process;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author sea36
 */
public class Lensfield {

    private static final Logger LOG = Logger.getLogger(Lensfield.class);

    private BuildState buildState;
    private BuildLog prevBuildState;
    private BuildLogger buildLogger;

    private final Model model;
    private final File root;
    
    private File workspace, tmpdir;

    private boolean offline = false;

    private List<org.lensfield.model.Process> buildOrder;

    private final Reactor reactor = new Reactor(this);


    public Lensfield(Model model, File root) {
        this.model = model;
        this.root = root;
    }


    private void init() throws LensfieldException, IOException {
        checkBuildStepsExist();
        buildOrder = resolveBuildOrder();

        initBuildState();
        resolveDependencies();

        initWorkspace();

        analyseBuildState();
        configurePipes();
        checkBuildState();
        loadPreviousBuildState();
    }

    private void configurePipes() {
        for (BuildStep build : model.getBuildSteps()) {
            Process process = buildState.getTask(build.getName());
            for (org.lensfield.model.Input input : build.getInputs()) {
                InputPipe i;
                if (input.getName() == null) {
                    i = process.getDefaultInput();
                } else {
                    i = process.getInput(input.getName());
                }
                Process source = buildState.getTask(input.getStep());
                source.getDefaultOutput().addPipe(i);
            }
            process.initGlobNames();
        }
    }


    public synchronized void clean() throws LensfieldException, IOException {
        init();
        if (prevBuildState == null) {
            throw new LensfieldException("No previous build state");
        }
        for (TaskLog task : prevBuildState.getTaskList()) {
            LOG.info("cleaning task: "+task.getName());
            if (task.isSource()) {
                LOG.info("skipping source task: "+task.getName());
                continue;
            }
            for (OperationLog op : task.getOperations()) {
                for (List<Resource> resources : op.getOutputSets()) {
                    for (Resource resource : resources) {
                        String path = resource.getPath();
                        File f = new File(root, path);
                        if (f.isFile()) {
                            LOG.info("deleting "+path);
                            if (!f.delete()) {
                                LOG.warn("Error deleting: "+path);
                            }
                            cleanEmptyDir(f.getParentFile());
                        }
                    }
                }
            }
        }
    }

    private void cleanEmptyDir(File dir) {
        if (dir != null && dir.list().length == 0) {
            if (!dir.delete()) {
                System.err.println("Error deleting: "+dir);
            } else {
                cleanEmptyDir(dir.getParentFile());
            }
        }
    }

    public synchronized void build() throws Exception {
        init();
        comparePreviousBuildState();
        try {
            initBuildLog();
            processSources();
            reactor.run();
            buildLogger.finishBuild();
        } finally {
            if (buildLogger != null) {
                buildLogger.close();
            }
        }
    }

    private void processSources() throws Exception {
        for (org.lensfield.model.Process process : buildOrder) {
            if (process instanceof Source) {
                processSource((Source)process);
            }
        }
    }

    private void checkBuildState() throws ConfigurationException {
        checkParameters();
    }

    private void checkParameters() throws ConfigurationException {
        for (Process task : buildState.getTasks()) {
            for (Parameter parameter : task.getParameters()) {
                if (parameter.isRequired() && parameter.getValue() == null) {
                    throw new ConfigurationException("Missing parameter '"+parameter.getName()+"' on task '"+task.getId()+"'");
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
    

    protected List<org.lensfield.model.Process> resolveBuildOrder() throws LensfieldException {
        LOG.info("Resolving build order");
        LinkedHashSet<org.lensfield.model.Process> order = new LinkedHashSet<org.lensfield.model.Process>(model.getSources());
        if (order.isEmpty()) {
            throw new ConfigurationException("No source build steps");
        }

        for (org.lensfield.model.Process proc : order) {
            LOG.debug(" - "+proc.getName());
        }

        int processCount = model.getProcesses().size();
        while (order.size() < processCount) {
            List<org.lensfield.model.Process> nextSteps = findNextSteps(order);
            if (nextSteps.isEmpty()) {
                throw new ConfigurationException("Unable to resolve build order; cyclic dependencies");
            }
            for (org.lensfield.model.Process proc : nextSteps) {
                LOG.debug(" - "+proc.getName());
            }
            order.addAll(nextSteps);
        }

        return new ArrayList<org.lensfield.model.Process>(order);
    }

    private List<org.lensfield.model.Process> findNextSteps(Collection<org.lensfield.model.Process> order) {
        List<org.lensfield.model.Process> nextSteps = new ArrayList<org.lensfield.model.Process>();
        for (BuildStep step : model.getBuildSteps()) {
            if (!order.contains(step) && containsAllSteps(order, step.getInputs())) {
                nextSteps.add(step);
            }
        }
        return nextSteps;
    }

    private boolean containsAllSteps(Collection<org.lensfield.model.Process> order, List<org.lensfield.model.Input> inputs) {
        for (org.lensfield.model.Input input : inputs) {
            if (!order.contains(model.getProcess(input.getStep()))) {
                return false;
            }
        }
        return true;
    }


    private void loadPreviousBuildState() throws IOException, LensfieldException {
        File logFile = new File(workspace, "log.txt");
        if (logFile.isFile()) {
            LOG.info("Loading last build log");
            Reader r = new BufferedReader(new FileReader(logFile));
            try {
                BuildStateReader in = new BuildStateReader();
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
        for (Process current : buildState.getTasks()) {
            TaskLog old = prevBuildState.getTask(current.getId());
            if (old != null) {
                if (isTaskUnchanged(current, old)) {
                    LOG.debug("Task "+current.getId()+": up-to-date");
                    current.setUpdated(false);
                    current.setLastModified(old.getLastModified());
                } else {
                    LOG.debug("Task "+current.getId()+": updated");
                }
            } else {
                LOG.debug("Task "+current.getId()+": new");
            }
        }
    }

    private boolean isTaskUnchanged(Process current, TaskLog old) {
        boolean unchanged = true;
        if (areDependenciesChanged(current,old)) {
            LOG.debug("Task "+current.getId()+ ": dependencies updated");
            unchanged = false;
        }
        if (areParametersChanged(current, old)) {
            LOG.debug("Task "+current.getId()+ ": parameters updated");
            unchanged = false;
        }
        return unchanged;
    }

    private boolean areParametersChanged(Process current, TaskLog old) {
        for (Parameter param : current.getParameters()) {
            Parameter oldParam = old.getParameter(param.getName());
            if (oldParam == null || !param.getValue().equals(oldParam.getValue())) {
                return true;
            }
        }
        return false;
    }

    private boolean areDependenciesChanged(Process current, TaskLog old) {
        List<Dependency> currentDependencies = current.getDependencyList();
        List<Dependency> oldDependencies = old.getDependencyList();
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
        for (org.lensfield.model.Process process : model.getProcesses()) {
            Process task = new Process(process.getName(), reactor);
            if (process instanceof BuildStep) {
                task.setClassName(process.getClassName());
            }
            buildState.addTask(task);
        }
    }

    /**
     * Analyses build state classes
     */
    private void analyseBuildState() throws LensfieldException {
        for (Source source : model.getSources()) {
            Process task = buildState.getTask(source.getName());
            configureOutputs(task, source);
        }
        for (BuildStep build : model.getBuildSteps()) {
            Process task = buildState.getTask(build.getName());
            ClassAnalyser.analyseClass(build, task);
            configureOutputs(task, build);
        }
    }

    private void configureOutputs(Process task, Source source) {
        OutputPipe output = new OutputPipe(task);
        output.setGlob(new Glob(source.getTemplate()));
        task.addOutput(output);
    }

    private void configureOutputs(Process task, BuildStep build) {
        for (org.lensfield.model.Output output : build.getOutputs()) {
            OutputPipe outputDescription;
            if (output.getName() == null) {
                outputDescription = task.getDefaultOutput();
            }
            else {
                outputDescription = task.getOutput(output.getName());
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
        buildLogger = new BuildLogger(new FileOutputStream(logFile));
        buildLogger.startBuild();
        buildLogger.recordTasks(buildState);
    }

    private void processSource(Source source) throws Exception {

        LOG.info("Processing source: "+source.getName());

        Process task = buildState.getTask(source.getName());

        FileSource finder = new FileSource();
        finder.setRoot(root.getPath());
        finder.setGlob(source.getTemplate());
        finder.setOutput(task.getDefaultOutput());
        finder.configure(source.getParameters());

        try {
            List<Resource> resources = finder.run();
            buildLogger.logSource(task, resources);
        } finally {
            task.getDefaultOutput().close();
        }
        
    }


    protected void checkBuildStepsExist() throws LensfieldException {
        LOG.info("Checking build steps");
        for (BuildStep step : model.getBuildSteps()) {
            for (org.lensfield.model.Input input : step.getInputs()) {
                String proc = input.getStep();
                if (proc.indexOf('/') != -1) {
                    int i = proc.indexOf('/');
                    proc = proc.substring(0, i);
                }
                if (model.getProcess(proc) == null) {
                    throw new ConfigurationException("Undefined input '"+input.getName()+"' on step '"+step.getName()+"'");
                }
            }
        }
    }


    private void resolveDependencies() throws LensfieldException {
        LOG.info("Resolving dependencies");

        try {
            System.setProperty("maven.artifact.threads", "1");  // Prevents hanging threads
            DependencyResolver resolver = new DependencyResolver(model.getRepositories());
            resolver.setOffline(isOffline());
            resolver.configureDependencies(model, buildState);
        } catch (Exception e) {
            throw new LensfieldException("Error resolving build dependencies", e);
        }
    }

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public File getRootDir() {
        return root;
    }

    public File getTmpDir() {
        return tmpdir;
    }

    public BuildLogger getBuildLogger() {
        return buildLogger;
    }

}
