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

    protected Neo4jService service(Request request) {
        HttpSession session = request.raw().getSession(true);
        Neo4jService service = (Neo4jService) session.getAttribute("service");
        if (service != null) return service;

        service = new Neo4jService();
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

    protected abstract Object doHandle(Request request, Response response, Neo4jService service) throws Exception;

    protected String param(Request request, String param, String defaultValue) {
        String geoff = request.queryParams(param);
        if (geoff==null || geoff.isEmpty()) {
            geoff= defaultValue;
        }
        return geoff;
    }

    protected long trace(String msg, long time) {
        long now=System.currentTimeMillis();
        System.err.println("## "+msg+" took: "+(now-time)+" ms.");
        return now;
    }
}
