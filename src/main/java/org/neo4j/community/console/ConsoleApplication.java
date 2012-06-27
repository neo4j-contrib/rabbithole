package org.neo4j.community.console;

import static org.neo4j.helpers.collection.MapUtil.map;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

import org.slf4j.Logger;
import org.neo4j.geoff.except.SubgraphError;
import org.neo4j.geoff.except.SyntaxError;

import spark.Request;
import spark.Response;
import spark.servlet.SparkApplication;

import com.google.gson.Gson;

public class ConsoleApplication implements SparkApplication {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ConsoleApplication.class);

    private ConsoleService consoleService;

    @Override
    public void init() {
        SessionService.setDatabaseInfo(ConsoleFilter.getDatabase());
        consoleService = new ConsoleService();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                SessionHoldingListener.cleanSessions();
                System.gc();
            }
        });

        post(new Route("/console/cypher") {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                final String query = request.body();
                if (query!=null && !query.isEmpty()) {
                    LOG.warn( "cypher: "+query );
                }
                return new Gson().toJson(consoleService.execute(service, null, query, null));
            }
        });
        post( new Route( "/console/version" )
        {
            protected Object doHandle( Request request, Response response, Neo4jService service )
            {
                final String version = request.body();
                service.setVersion( version );
                return new Gson().toJson( map("version", service.getVersion()) );
            }
        } );
        get(new Route("/console/cypher") {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                String query = param(request, "query", "");
                return service.cypherQueryResults(query).toString();
            }
        });
        post(new Route("/console/init") {
            @Override
            protected void doBefore(Request request, Response response) {
                reset(request);
            }

            protected Object doHandle(Request request, Response response, Neo4jService service) {
                final Map input = requestBodyToMap(request);
                final String id = param(input, "id", null);
                final Map<String, Object> result;
                if (id != null) {
                    result = consoleService.init(service, id);
                } else {
                    result = consoleService.init(service, input);
                }
                return new Gson().toJson(result);
            }

        });
        get(new Route("/console/visualization") {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                String query = request.queryParams("query");
                return new Gson().toJson(service.cypherQueryViz(query));
            }
        });
        get(new Route("/console/to_geoff") {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                return service.exportToGeoff();
            }
        });
        get(new Route("/console/to_yuml") {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                String query = param( request,"query", "");
                String[] props = param( request,"props", "name").split(",");
                final String type = param(request, "type", "jpg");
                final String scale = param(request, "type", "100");
                SubGraph graph;
                if (query.trim().isEmpty() || !service.isCypherQuery(query) || service.isMutatingQuery(query)) {
                    graph = SubGraph.from(service.getGraphDatabase());
                } else {
                    final CypherQueryExecutor.CypherResult result = service.cypherQuery(query);
                    graph = SubGraph.from(result);
                }
                final String yuml = new YumlExport().toYuml(graph, props);
                return String.format("http://yuml.me/diagram/scruffy;dir:LR;scale:%s;/class/%s.%s",scale,yuml,type);
            }
        });
        get( new Route( "/console/to_cypher" )
        {
            protected Object doHandle( Request request, Response response, Neo4jService service )
            {
                return service.exportToCypher();
            }
        } );
        get( new Route( "/console/url" )
        {
            protected Object doHandle( Request request, Response response, Neo4jService service ) throws IOException
            {
                final String uri = baseUri( request.raw(), "init=" + URLEncoder.encode( service.exportToGeoff(), "UTF-8" ) + hasRootNodeParam( service ), null);
                return consoleService.shortenUrl( uri );
            }
        } );
        get(new Route("/console/shorten") {
            protected Object doHandle(Request request, Response response, Neo4jService service) throws IOException {
                return consoleService.shortenUrl(request.queryParams("url"));
            }
        });

        delete(new Route("/console") {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                reset(request);
                return "deleted";
            }
        });

        post(new Route("/console/geoff") {
            protected Object doHandle(Request request, Response response, Neo4jService service) throws SyntaxError, SubgraphError {
                String geoff = request.body();
                if (geoff!=null && !geoff.isEmpty()) {
                    LOG.warn( "geoff: "+geoff );
                }
                Map res = service.mergeGeoff( geoff );
                return new Gson().toJson(res);
            }
        });
    }

    private Map requestBodyToMap(Request request) {
        Map result = new Gson().fromJson(request.body(), Map.class);
        return result!=null ? result : map();
    }

    private String hasRootNodeParam(Neo4jService service) {
        return service.hasReferenceNode() ? "" : "&no_root=true";
    }
}
