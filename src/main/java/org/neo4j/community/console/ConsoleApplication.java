package org.neo4j.community.console;

import com.google.gson.Gson;
import org.neo4j.geoff.except.SubgraphError;
import org.neo4j.geoff.except.SyntaxError;
import spark.Request;
import spark.Response;
import spark.servlet.SparkApplication;

import java.util.Map;

import static spark.Spark.*;

public class ConsoleApplication implements SparkApplication {

    @Override
    public void init() {
        post(new Route("/console/cypher") {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                final String query = request.body();
                return service.cypherQuery(query);
            }
        });
        get(new Route("/console/cypher") {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                String query = request.queryParams("query");
                return new Gson().toJson(service.cypherQueryResults(query));
            }

        });
        get(new Route("/console/visualization") {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                String query = request.queryParams("query");
                return new Gson().toJson(service.cypherQueryViz(query));
            }
        });
        get(new Route("/console/share") {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                return service.toGeoff();
            }
        });

        delete(new Route("/console") {
            protected Object doHandle(Request request, Response response, Neo4jService service) {
                request.raw().getSession().invalidate();
                return "deleted";
            }
        });

        post(new Route("/console/geoff") {
            protected Object doHandle(Request request, Response response, Neo4jService service) throws SyntaxError, SubgraphError {
                Map res = service.mergeGeoff(request.body());
                return new Gson().toJson(res);
            }
        });
    }
}
