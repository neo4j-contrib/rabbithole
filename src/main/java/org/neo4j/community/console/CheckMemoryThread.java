package org.neo4j.community.console;

import org.eclipse.jetty.server.Server;

import java.util.concurrent.TimeUnit;

/**
 * @author mh
 * @since 21.01.15
 */
public class CheckMemoryThread implements Runnable {

    public static final int THRESHOLD_PERCENT = 15;
    private static final Runtime runtime = Runtime.getRuntime();

    @Override
    public void run() {
        long percentFree = runtime.freeMemory() * 100 / runtime.totalMemory();
        System.err.printf("memory %d percent %d free %d available %d max%n", percentFree, runtime.freeMemory(), runtime.totalMemory(), runtime.maxMemory());
        if (percentFree < THRESHOLD_PERCENT) {
            System.err.flush();
            Halt.halt("Running low on memory.");
        }
    }
}
