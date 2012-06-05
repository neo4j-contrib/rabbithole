package org.neo4j.community.console;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.EmbeddedReadOnlyGraphDatabase;

public class Console
{

    private static final String WEBAPP_LOCATION = "src/main/webapp/";
    private Server server;
    private final DatabaseInfo databaseInfo;

    public Console(DatabaseInfo databaseInfo) {
        this.databaseInfo = databaseInfo;
    }

    public static void main(String[] args) throws Exception
    {
        int port = (args.length>0) ? Integer.parseInt(args[0]): getPort();
        boolean expose = args.length>2 && args[2].equalsIgnoreCase("expose");
        GraphDatabaseService database = (args.length>1) ? embeddedGraphDatabase(args[1],expose) : null;
        final Console console = expose ? Console.expose(database) : Console.sandbox(database);
        console.start(port);
        console.join();
    }

    private static GraphDatabaseService embeddedGraphDatabase(String path, boolean expose) {
        if (expose) return new EmbeddedGraphDatabase(path);
        return new EmbeddedReadOnlyGraphDatabase(path);
    }

    public static Console sandbox(GraphDatabaseService database) {
        return new Console(DatabaseInfo.sandbox(database));
    }

    public static Console expose(GraphDatabaseService database) {
        return new Console(DatabaseInfo.expose(database));
    }

    public void start(int port) throws Exception {
        System.err.println("Port used: "+ port +" location "+ WEBAPP_LOCATION+" "+databaseInfo.toString());
        server = new Server(port);
        WebAppContext root = new WebAppContext();
        root.setContextPath("/");
        root.setDescriptor(WEBAPP_LOCATION + "/WEB-INF/web.xml");
        root.setResourceBase(WEBAPP_LOCATION);
        root.setParentLoaderPriority(true);
        root.setAttribute(ConsoleFilter.DATABASE_ATTRIBUTE, databaseInfo);
        server.setHandler(root);
        server.start();
    }

    public void join() throws InterruptedException {
        server.join();
    }


    public void stop() throws Exception {
        server.stop();
    }

    private static int getPort() {
        String webPort = System.getenv("PORT");
        if(webPort == null || webPort.isEmpty()) {
            return 8080;
        }
        return Integer.parseInt(webPort);
    }
}

