package org.lensfield.concurrent;

import org.lensfield.state.Operation;

/**
 * @author sea36
 */
public class Worker extends Thread {

    private Reactor reactor;

    public Worker(Reactor reactor) {
        this.reactor = reactor;
    }

    public void run() {

        while (!reactor.isFinished() && !isInterrupted()) {

            Operation task = null;
            try {
                task = reactor.poll();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
            if (task != null) {
                try {
                    if (reactor.getLensfield().isUpToDate(task)) {
                        System.err.println("up-to-date: "+task.getProcess().getId()+" / "+task.getParameters());
                    } else {
                        OperationRunner runner = task.getProcess().getRunner();
                        runner.runProcess(task);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    task.finished();
                }
            }

        }

    }

}
