package org.neo4j.community.console;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.webapp.WebAppContext;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.slf4j.Logger;

import java.io.File;
import java.util.concurrent.*;

public class Console
{

    private static final String WEBAPP_LOCATION = "src/main/webapp/";
    private Server server;
    private final DatabaseInfo databaseInfo;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(Console.class);
    private final ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();

    public Console(DatabaseInfo databaseInfo) {
        this.databaseInfo = databaseInfo;
    }

    public static void main(String[] args) throws Exception
    {
        System.setProperty("file.encoding","UTF-8");
        int port = (args.length>0) ? Integer.parseInt(args[0]): getPort();
        final Console console = Console.sandbox();
        console.start(port);
        console.join();
    }

    private static GraphDatabaseService embeddedGraphDatabase(String path, boolean expose) {
        GraphDatabaseBuilder builder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new File(path));
        if (!expose) {
            builder.setConfig(GraphDatabaseSettings.read_only,"true");
        }
        return builder.newGraphDatabase();
    }

    public static Console sandbox() {
        return new Console(DatabaseInfo.sandbox());
    }

    public void start(int port) throws Exception {
        LOG.warn("Port used: " + port + " location " + WEBAPP_LOCATION + " " + databaseInfo.toString());
        server = new Server(port);
        server.setStopAtShutdown(true);
        Halt.setServer(server);
        WebAppContext root = new WebAppContext();
        root.setContextPath("/");
        root.setDescriptor(WEBAPP_LOCATION + "/WEB-INF/web.xml");
        root.setResourceBase(WEBAPP_LOCATION);
        root.setParentLoaderPriority(true);
        root.setAttribute(ConsoleFilter.DATABASE_ATTRIBUTE, databaseInfo);
        final HandlerList handlers = new HandlerList();
        // don't remove, needed for neo4j.org proxy
        final Handler resourceHandler = createResourceHandler("/console_assets", WEBAPP_LOCATION);
        handlers.setHandlers(new Handler[]{resourceHandler, root});
        server.setHandler(handlers);
        pool.scheduleAtFixedRate(new CheckMemoryThread(),0,10, TimeUnit.SECONDS);
        server.start();
    }




    private Handler createResourceHandler(String context, String resourceBase) {
        WebAppContext ctx = new WebAppContext();
        ctx.setContextPath(context);
        ctx.setResourceBase(resourceBase);
        ctx.setParentLoaderPriority(true);
        return ctx;
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

