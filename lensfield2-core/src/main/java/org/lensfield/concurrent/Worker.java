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

                System.err.println("RUNNING "+task.getProcess().getId()+" / "+task.getParameters());

                try {

                    ProcessRunner runner = task.getProcess().getRunner();
                    runner.runProcess(task);


//                    task.
                    // execute task

                    // record task complete

                    // catch/handle exception

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    task.finished();
                }

            }

        }

    }

}
