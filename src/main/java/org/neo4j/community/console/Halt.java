package org.neo4j.community.console;

import org.eclipse.jetty.server.Server;

/**
 * @author mh
 * @since 21.01.15
 */
public class Halt {


    private static Server server;

    public static void halt(String message) {
        try {
            if (message!=null) System.err.println(message);
            System.err.println("Stopping Server and Console");System.err.flush();
        } catch(Throwable t) {
            // ignore
        }
        try {
            server.stop();
        } catch (Exception e) {
            System.err.println("Error during stopping the server");
        } finally {
            System.exit(1);
        }
    }

    public static void setServer(Server server) {
        if (Halt.server != null && server != Halt.server) {
            halt("Trying to set Halt.server to a different instance. Stopping.");
        }
        Halt.server = server;
    }
}
