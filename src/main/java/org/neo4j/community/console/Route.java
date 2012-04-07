package org.neo4j.community.console;

import spark.Request;
import spark.Response;

import javax.servlet.http.HttpSession;

/**
* @author mh
* @since 08.04.12
*/
abstract class Route extends spark.Route {

    Route(String path) {
        super(path);
    }

    private ConsoleApplication.Neo4jService service(Request request) {
        HttpSession session = request.raw().getSession(true);
        ConsoleApplication.Neo4jService service = (ConsoleApplication.Neo4jService) session.getAttribute("service");
        if (service != null) return service;

        service = new ConsoleApplication.Neo4jService();
        session.setAttribute("service", service);
        return service;
    }

    @Override
    public Object handle(Request request, Response response) {
        try {
            return doHandle(request, response, service(request));
        } catch (Exception e) {
            e.printStackTrace();
            halt(500, e.getMessage());
            return e.getMessage();
        }
    }

    protected abstract Object doHandle(Request request, Response response, ConsoleApplication.Neo4jService service) throws Exception;
}
