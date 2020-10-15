package org.neo4j.community.console;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import spark.Request;
import spark.Response;
import spark.servlet.SparkApplication;

import java.util.Map;

import static spark.Spark.delete;
import static spark.Spark.post;

/**
 * @author mh
 * @since 21.06.16
 */


public class BackendApplication implements SparkApplication {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger("spark.Backend");

    private ConsoleService consoleService;

    @Override
    public void init() {
        consoleService = new ConsoleService();
        SessionService.setDatabaseInfo(ConsoleFilter.getDatabase());
        SessionService.setDriver(consoleService.getDriver());
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof Error) {
                Halt.halt(null);
            }
            SessionService.cleanSessions();
            System.gc();
        });

        post("/backend/cypher", new Route() {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                final String query = request.body();
                if (query!=null && !query.isEmpty()) {
                    LOG.warn( "cypher: "+query );
                }
                String result = gson().toJson(consoleService.execute(service, null, query, null, null,null));
                LOG.info(result);
                return result;
            }
        });
        post("/backend/cypher/:id", new Route() {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                String id = request.params("id");
                if (!service.isInitialized() || !service.hasId(id)) {
                    Map<String, Object> result = consoleService.init(service, id,MapUtil.map("initialize","true"));
                    if (result.containsKey("error")) {
                        return gson().toJson(result);
                    }
                }
                String query = request.body();
                if (query!=null && !query.isEmpty()) {
                    LOG.warn( "cypher: "+query );
                } else {
                    query = "none";
                }
                String result = gson().toJson(consoleService.execute(service, null, query, null,null,null));
                LOG.debug (result);
                return result;
            }
        });
        post("/backend/graph/:id", new Route() {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                String id = request.params("id");
                String init = request.body();
                final Map<String, Object> result = consoleService.save(id, init);
                return gson().toJson(result);
            }
        });
        delete("/backend/graph/:id", new Route() {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                String id = request.params("id");
                SessionService.cleanSession(id);
                return consoleService.delete(id);
            }
        });
        post("/backend/version", new Route()
        {
            protected Object doHandle( Request request, Response response, Neo4jService service )
            {
                final String version = request.body();
                service.setVersion( version );
                return gson().toJson(MapUtil.map("version", service.getVersion()));
            }
        } );
        delete("/backend", new Route() {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                reset(request);
                return "deleted";
            }
        });
    }

    private Gson gson() {
        return new GsonBuilder().disableHtmlEscaping().create();
    }
}
