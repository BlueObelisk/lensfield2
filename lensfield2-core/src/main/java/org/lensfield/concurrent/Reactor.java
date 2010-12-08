package org.lensfield.concurrent;

import org.apache.log4j.Logger;
import org.lensfield.Lensfield;
import org.lensfield.state.Operation;
import org.lensfield.state.Process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author sea36
 */
public class Reactor {

    private static final Logger LOG = Logger.getLogger(Reactor.class);

    private Lensfield lensfield;
    private ResourceManager resourceManager = new ResourceManager();

    private Queue<Process> processQueue = new ConcurrentLinkedQueue<Process>();

    private List<Worker> workers = Collections.synchronizedList(new ArrayList<Worker>());

    private int nworker = 1;    // Runtime.getRuntime().availableProcessors();

    public Reactor(Lensfield lensfield) {
        this.lensfield = lensfield;
    }

    public void run() {

        startWorkers();
        while (!isFinished()) {
            try {
                Thread.yield();
                Thread.sleep(100);
            } catch (InterruptedException e) {
                for (Worker worker : workers) {
                    worker.interrupt();
                    break;
                }
            }
        }
        
    }

    private void startWorkers() {
        for (int i = 0; i < nworker; i++) {
            Worker worker = new Worker(this);
            workers.add(worker);
            worker.start();
        }
    }

    public Operation poll() throws InterruptedException {

        while (!isFinished()) {
            for (Process process : processQueue) {
                if (process.isFinished()) {
                    LOG.info("Process finished: "+process.getId());
                    process.close();
                    processQueue.remove(process);
                } else {
                    Operation task = process.poll();
                    if (task != null) {
                        return task;
                    }
                }
            }
            synchronized (this) {
                wait(10);
            }
        }

        return null;

    }

    public boolean isFinished() {
        return processQueue.isEmpty();
    }

    public void queue(Process process) {
        LOG.info("Process queued: "+process.getId());
        processQueue.add(process);
    }

    public OperationRunner getOperationRunner(Process process) throws Exception {
        OperationRunner runner = new OperationRunner(process, lensfield.getBuildLogger());
        runner.setRoot(lensfield.getRootDir());
        runner.setTmpdir(lensfield.getTmpDir());
        return runner;
    }


    public ResourceManager getResourceManager() {
        return resourceManager;
    }
}
