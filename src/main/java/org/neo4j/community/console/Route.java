package org.neo4j.community.console;

import org.neo4j.kernel.lifecycle.LifecycleException;
import spark.Request;
import spark.Response;

import java.util.Map;

/**
 * @author mh
 * @since 08.04.12
 */
abstract class Route extends spark.Route {

    Route(String path) {
        super(path);
    }

    @Override
    public Object handle(Request request, Response response) {
        try {
            doBefore(request,response);
            return doHandle(request, response, service(request));
        } catch (LifecycleException lce) {
            reset(request);
            SessionHoldingListener.cleanSessions();
            return handleException(lce);
        } catch (OutOfMemoryError oom) {
            reset(request);
            SessionHoldingListener.cleanSessions();
            return handleException(oom);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private Object handleException(Throwable e) {
        e.printStackTrace();
        halt(500, e.getMessage());
        return e.getMessage();
    }

    protected abstract Object doHandle(Request request, Response response, Neo4jService service) throws Exception;
    protected void doBefore(Request request, Response response) {
    }

    protected String param(Request request, String param, String defaultValue) {
        String data = request.queryParams(param);
        if (data == null || data.isEmpty()) {
            data = defaultValue;
        } else {
            System.err.println(param+": "+data);
        }
        return data;
    }
    protected String param(Map input, String param, String defaultValue) {
        if (input==null) return defaultValue;
        String data = (String) input.get(param);
        if (data == null || data.isEmpty()) {
            data = defaultValue;
        } else {
            System.err.println(param+": "+data);
        }
        return data;
    }

    protected long trace(String msg, long time) {
        long now = System.currentTimeMillis();
        System.err.println("## " + msg + " took: " + (now - time) + " ms.");
        return now;
    }

    protected void reset(Request request) {
        SessionService.reset(request.raw());
    }

    protected Neo4jService service(Request request) {
        return SessionService.getService(request.raw());
    }
}
