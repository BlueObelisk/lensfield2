/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import org.lensfield.api.Logger;
import org.lensfield.concurrent.Reactor;
import org.lensfield.glob.Glob;
import org.lensfield.log.BuildLogger;
import org.lensfield.log.BuildStateReader;
import org.lensfield.model.Build;
import org.lensfield.model.Model;
import org.lensfield.model.Source;
import org.lensfield.source.FileSource;
import org.lensfield.state.BuildState;
import org.lensfield.state.Dependency;
import org.lensfield.state.Input;
import org.lensfield.state.Output;
import org.lensfield.state.Parameter;
import org.lensfield.state.Process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author sea36
 */
public class Lensfield {

    private final Logger LOG = new LoggerImpl();

    private BuildState buildState;
    private BuildState prevBuildState;
    private BuildLogger buildLog;

    private Model model;
    private File root = new File(".");
    private File workspace, tmpdir;

    private boolean offline = true;

    private ArrayList<org.lensfield.model.Process> buildOrder;

    private Reactor reactor = new Reactor(this);


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
        configurePipes();
        checkBuildState();
        loadPreviousBuildState();
    }

    private void configurePipes() {
        for (Build build : model.getBuilds()) {
            Process process = buildState.getTask(build.getName());
            for (org.lensfield.model.Input input : build.getInputs()) {
                Input i;
                if (input.getName() == null) {
                    i = process.getDefaultInput();
                } else {
                    i = process.getInput(input.getName());
                }
                Process source = buildState.getTask(input.getStep());
                System.err.print("PIPE "+source.getId()+"/"+source.getDefaultOutput().getName());
                System.err.print(" >> ");
                System.err.print(process.getId()+"/"+i.getName());
                System.err.println();
                source.getDefaultOutput().addPipe(i);
            }
            process.initGlobNames();
        }
    }


    public synchronized void clean() throws Exception {
        init();
        
        if (prevBuildState == null) {
            throw new LensfieldException("No previous build state");
        }
        for (Process task : prevBuildState.getTasks()) {
            if (task.isSource()) {
                continue;
            }
//            for (Operation op : task.getOperations()) {
//                for (List<FileState> outs : op.getOutputFiles().values()) {
//                    for (FileState out : outs) {
//                        String path = out.getPath();
//                        File f = new File(root, path);
//                        if (f.isFile()) {
//                            System.err.println("deleting "+path);
//                            if (!f.delete()) {
//                                System.err.println("Error deleting: "+path);
//                            }
//                        }
//                        // TODO delete directories
//                    }
//                }
//            }
        }
    }
    
    public synchronized void build() throws Exception {

        init();
        comparePreviousBuildState();

        try {
            initBuildLog();
            processBuildSteps(buildOrder);

            reactor.run();

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
        for (Process task : buildState.getTasks()) {
            for (Parameter param : task.getParameters()) {
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
    

    protected ArrayList<org.lensfield.model.Process> resolveBuildOrder() throws LensfieldException {
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
        for (Build step : model.getBuilds()) {
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


    private void loadPreviousBuildState() throws IOException, ParseException {
        File logFile = new File(workspace, "log.txt");
        if (logFile.isFile() && false) {
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
        for (Process current : buildState.getTasks()) {
            Process old = prevBuildState.getTask(current.getId());
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

    private boolean isTaskUnchanged(Process current, Process old) {
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

    private boolean areParametersChanged(Process current, Process old) {
        for (Parameter param : current.getParameters()) {
            Parameter oldParam = old.getParameter(param.getName());
            if (oldParam == null || !param.getValue().equals(oldParam.getValue())) {
                return true;
            }
        }
        return false;
    }

    private boolean areDependenciesChanged(Process current, Process old) {
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
        for (Source source : model.getSources()) {
            Process task = buildState.getTask(source.getName());
            configureOutputs(task, source);
        }
        for (Build build : model.getBuilds()) {
            Process task = buildState.getTask(build.getName());
            ClassAnalyser.analyseClass(build, task);
            configureOutputs(task, build);
        }
    }

    private void configureOutputs(Process task, Source source) {
        Output output = new Output(task);
        output.setGlob(new Glob(source.getTemplate()));
        task.addOutput(output);
    }

    private void configureOutputs(Process task, Build build) {
        for (org.lensfield.model.Output output : build.getOutputs()) {
            Output outputDescription;
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
        buildLog = new BuildLogger(new FileOutputStream(logFile));
        buildLog.startBuild();
        buildLog.recordTasks(buildState);
    }


    protected synchronized void build(org.lensfield.model.Process step) throws Exception {

        if (step instanceof Source) {
            processSource((Source)step);
        }
//        else if (step instanceof Build) {
//            processBuildStep((Build)step);
//        }
//        else {
//            throw new LensfieldException("Unknown process: "+step.getName()+" ["+step.getClass().getName()+"]");
//        }

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
            finder.run();
        } finally {
            task.getDefaultOutput().close();
        }
        
    }


    protected void checkBuildStepsExist() throws LensfieldException {
        LOG.info("Checking build steps");
        for (Build step : model.getBuilds()) {
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


    private void resolveDependencies() throws Exception {
        LOG.info("Resolving dependencies");

        System.setProperty("maven.artifact.threads", "1");  // Prevents hanging threads

        DependencyResolver resolver = new DependencyResolver(model.getRepositories());
        resolver.setOffline(offline);
        resolver.configureDependencies(model, buildState);
    }


    private void processBuildSteps(List<org.lensfield.model.Process> buildOrder) throws Exception {

        for (org.lensfield.model.Process step: buildOrder) {
            build(step);
        }

//        reactor.run();

    }


    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }


    public static void main(String[] args) throws Exception {

        Model model = new Model();
        model.addSource(new Source("files", "n/**/*.n"));
        model.addBuild(new Build("copy1", "org.apache.commons.io.IOUtils/copy", "files", "x/**/*.txt"));
        model.addBuild(new Build("copy2", "org.apache.commons.io.IOUtils/copy", "files", "y/**/*.txt"));
        Build b = new Build("merge", "org.lensfield.testing.ops.file.Joiner", "copy1", "z/**/x.txt");
        b.addDependency("org.lensfield.testing", "lensfield2-testops1", "0.2-SNAPSHOT");
        model.addBuild(b);

        File root = new File("./workspace/");
        Lensfield lf = new Lensfield(model, root);
        lf.build();

    }


    public File getRootDir() {
        return root;
    }

    public File getTmpDir() {
        return tmpdir;
    }

}
