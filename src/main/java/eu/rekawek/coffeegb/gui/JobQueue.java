package eu.rekawek.coffeegb.gui;

import java.lang.management.ManagementFactory;
import java.util.LinkedList;
import java.util.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

interface IntReq {

    void callBack();
}

class IntBox {

    IntReq item;

    void clear() {
        this.item = null;
    }
}

public class JobQueue {

    private Queue<IntReq> queue;
   /* private Logger logger = LoggerFactory.getLogger(this.getClass());

    {
        // Get the process id
        String pid = ManagementFactory.getRuntimeMXBean().getName().replaceAll("@.*", "");
        // MDC
        MDC.put("PID", pid);
    }*/

    private JobQueue() {
        queue = new LinkedList<IntReq>();
    }

    public static JobQueue getInstace() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {

        private static final JobQueue INSTANCE = new JobQueue();
    }

    public void add(IntReq input) {
        synchronized (this) {
            //logger.debug("add");
            queue.offer(input);
            notifyAll();
        }
    }

    public void get(IntBox output) {
        synchronized (this) {
            while (queue.isEmpty()) {
                try {
                    //logger.debug("get blocked");
                    wait();
                } catch (InterruptedException e) {
                    System.out.println("getting from queue is interrupted");
                }
            }
        }

        //logger.debug("get");
        output.item = queue.poll();
    }

}
