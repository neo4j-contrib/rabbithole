package org.neo4j.community.console;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.webapp.WebAppContext;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.EmbeddedReadOnlyGraphDatabase;
import org.slf4j.Logger;

public class Console
{

    private static final String WEBAPP_LOCATION = "src/main/webapp/";
    public static final int REQUEST_TIME_LIMIT = 10 * 1000;
    public static final int MAX_OPS_LIMIT = 100000;
    private Server server;
    private final DatabaseInfo databaseInfo;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(Console.class);

    public Console(DatabaseInfo databaseInfo) {
        this.databaseInfo = databaseInfo;
    }

    public static void main(String[] args) throws Exception
    {
        System.setProperty(GraphDatabaseSettings.udc_source.name(),"console");
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
        LOG.warn("Port used: " + port + " location " + WEBAPP_LOCATION + " " + databaseInfo.toString());
        server = new Server(port);
        WebAppContext root = new WebAppContext();
        root.setContextPath("/");
        root.setDescriptor(WEBAPP_LOCATION + "/WEB-INF/web.xml");
        root.setResourceBase(WEBAPP_LOCATION);
        root.setParentLoaderPriority(true);
        root.setAttribute(ConsoleFilter.DATABASE_ATTRIBUTE, databaseInfo);
        setupRequestLimits(root, REQUEST_TIME_LIMIT, MAX_OPS_LIMIT);
        server.setHandler(root);
        server.start();
    }

    private void setupRequestLimits(WebAppContext root, Integer limit, int maxOps) {
        if (limit == null) return;
        GuardingRequestFilter requestTimeLimitFilter = new GuardingRequestFilter(limit, maxOps);
        root.addFilter(new FilterHolder(requestTimeLimitFilter), "/*", FilterMapping.REQUEST);
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

