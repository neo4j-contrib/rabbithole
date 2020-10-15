package org.neo4j.community.console;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import spark.Request;
import spark.Response;
import spark.servlet.SparkApplication;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class ConsoleApplication implements SparkApplication {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger("spark.Console");

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

        post("console/cypher", new Route() {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                Map<String, Object> result;
                try {
                    final String body = request.body();
                    if (body != null && !body.isEmpty()) {
                        LOG.warn("cypher: " + body);
                    }
                    Map<String, Object> requestParams = queryParamsMap(request);
                    Map data = body.startsWith("{") ? fromJson(body) : Collections.singletonMap("query", body);
                    String query = (String) data.get("query");
                    Map<String, Object> queryParams = (Map) data.get("queryParams");
                    result = consoleService.execute(service, null, query, null, requestParams, queryParams);
                } catch (Exception e) {
                    result = MapUtil.map("error", e.toString());
                }
                return toJson(result);
            }
        });
        post("console/version", new Route() {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                final String version = request.body();
                service.setVersion(version);
                return toJson(MapUtil.map("version", service.getVersion()));
            }
        });
        get("console/cypher", new Route() {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                String query = param(request, "query", "");
                return service.cypherQueryResults(query).toString();
            }
        });
        post("console/init", new Route() {
            @Override
            protected void doBefore(Request request, Response response) {
                Neo4jService service = SessionService.getService(request.raw(), true);
                if (service.isInitialized()) {
                    reset(request);
                }
            }

            protected Object doHandle(Request request, Response response, Neo4jService service) {
                @SuppressWarnings("unchecked") final Map<String, Object> input = requestBodyToMap(request);
                final String id = param(input, "id", null);
                final Map<String, Object> result;
                if (id != null) {
                    result = consoleService.init(service, id, input);
                } else {
                    result = consoleService.init(service, input);
                }
                return toJson(result);
            }

        });
        get("console/visualization",new Route() {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                String query = request.queryParams("query");
                return toJson(service.cypherQueryViz(query));
            }
        });
        get("console/to_yuml", new Route() {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                String query = param(request, "query", "");
                String[] props = param(request, "props", "name").split(",");
                final String type = param(request, "type", "jpg");
                final String scale = param(request, "type", "100");
                SubGraph graph;
                if (query.trim().isEmpty() || !service.isCypherQuery(query) || service.isMutatingQuery(query)) {
                    graph = SubGraph.from(consoleService.getDriver(), service.getDb());
                } else {
                    final CypherQueryExecutor.CypherResult result = service.cypherQuery(query, null);
                    graph = SubGraph.from(result);
                }
                final String yuml = new YumlExport().toYuml(graph, props);
                return String.format("http://yuml.me/diagram/scruffy;dir:LR;scale:%s;/class/%s.%s", scale, yuml, type);
            }
        });
        get("console/to_cypher", new Route() {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                return service.exportToCypher();
            }
        });
        get("console/shorten", new Route() {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                return consoleService.shortenUrl(request.queryParams("url"));
            }
        });

        delete("console", new Route() {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                reset(request);
                return "deleted";
            }
        });
    }

    private Map<String, Object> queryParamsMap(Request request) {
        Map<String, Object> result = new HashMap<>();
        for (String param : request.queryParams()) {
            result.put(param, request.queryParams(param));
        }
        return result;
    }

    private String toJson(Object result) {
        return new GsonBuilder().serializeNulls().create().toJson(result);
    }

    private Map fromJson(String input) {
        return new GsonBuilder().serializeNulls().create().fromJson(input, Map.class);
    }

    private Map requestBodyToMap(Request request) {
        Map result = new Gson().fromJson(request.body(), Map.class);
        return result != null ? result : Collections.emptyMap();
    }
}
