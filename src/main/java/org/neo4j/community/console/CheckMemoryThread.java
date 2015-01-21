package org.neo4j.community.console;

import org.eclipse.jetty.server.Server;

import java.util.concurrent.TimeUnit;

/**
 * @author mh
 * @since 21.01.15
 */
public class CheckMemoryThread extends Thread {

    private final Runtime runtime = Runtime.getRuntime();

    public CheckMemoryThread() {
        setDaemon(true);
    }

    @Override
    public void run() {
        while (true) {
            try {
                long percentFree = runtime.freeMemory()*100 / runtime.maxMemory();
                System.err.printf("memory %d percent %d free %d available %d max%n", percentFree, runtime.freeMemory(), runtime.totalMemory(), runtime.maxMemory());
                if (percentFree < 30) {
                    System.err.flush();
                    break;
                }
                sleep(TimeUnit.MINUTES.toMillis(1));
            } catch (InterruptedException e) {
                interrupt();
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        Halt.halt("Running low on memory.");
    }
}
