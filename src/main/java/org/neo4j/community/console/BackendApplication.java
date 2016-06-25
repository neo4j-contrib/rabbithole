package org.neo4j.community.console;

import static org.neo4j.helpers.collection.MapUtil.map;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;

import java.util.Map;

import com.google.gson.GsonBuilder;
import org.neo4j.kernel.lifecycle.LifecycleException;
import org.slf4j.Logger;

import spark.Request;
import spark.Response;
import spark.servlet.SparkApplication;

import com.google.gson.Gson;

/**
 * @author mh
 * @since 21.06.16
 */


public class BackendApplication implements SparkApplication {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger("spark.Backend");

    private ConsoleService consoleService;

    @Override
    public void init() {
        SessionService.setDatabaseInfo(ConsoleFilter.getDatabase());
        consoleService = new ConsoleService();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                if (throwable instanceof Error || throwable instanceof LifecycleException) {
                    Halt.halt(null);
                }
                SessionService.cleanSessions();
                System.gc();
            }
        });

        post(new Route("/backend/cypher") {
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
        post(new Route("/backend/cypher/:id") {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                String id = request.params("id");
                if (!service.isInitialized() || !service.hasId(id)) {
                    Map<String, Object> result = consoleService.init(service, id,map("initialize","true"));
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
        post(new Route("/backend/graph/:id") {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                String id = request.params("id");
                String init = request.body();
                final Map<String, Object> result = consoleService.save(id, init);
                return gson().toJson(result);
            }
        });
        delete(new Route("/backend/graph/:id") {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                String id = request.params("id");
                SessionService.cleanSession(id);
                return consoleService.delete(id);
            }
        });
        post( new Route( "/backend/version" )
        {
            protected Object doHandle( Request request, Response response, Neo4jService service )
            {
                final String version = request.body();
                service.setVersion( version );
                return gson().toJson(map("version", service.getVersion()));
            }
        } );
        delete(new Route("/backend") {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                reset(request);
                return "deleted";
            }
        });
    }

    private Gson gson() {
        return new GsonBuilder().disableHtmlEscaping().create();
    }

    private Map requestBodyToMap(Request request) {
        Map result = gson().fromJson(request.body(), Map.class);
        return result!=null ? result : map();
    }
}
