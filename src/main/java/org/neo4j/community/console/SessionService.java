package org.neo4j.community.console;

import org.neo4j.kernel.lifecycle.LifecycleException;
import spark.HaltException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author mh
 * @since 09.04.12
 */
class SessionService {
    public static final String SERVICE = "service";

    private static DatabaseInfo databaseInfo;

    public static void setDatabaseInfo(DatabaseInfo databaseInfo) {
        SessionService.databaseInfo = databaseInfo;
    }

    public static void reset(final HttpServletRequest httpRequest) {
        final HttpSession session = httpRequest.getSession(false);
        cleanSession(session, true);
    }

    public static void cleanSession(HttpSession session, final boolean invalidate) {
        if (session == null) return;
        Neo4jService service = (Neo4jService) session.getAttribute(SERVICE);
        if (service != null) {
            try {
                service.stop();
                session.removeAttribute(SERVICE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (invalidate) {
            session.invalidate();
        }
    }

    public static Neo4jService getService(final HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(true);
            Neo4jService service = (Neo4jService) session.getAttribute(SERVICE);
            if (service != null) return service;

            service = databaseInfo.shouldCreateNew() ? new Neo4jService() : new Neo4jService(databaseInfo.getDatabase());
            if (databaseInfo.shouldImport()) {
                service.initializeFrom(SubGraph.from(databaseInfo.getDatabase()));
            }
            session.setAttribute(SERVICE, service);
            return service;
        } catch (LifecycleException lce) {
            reset(request);
            SessionHoldingListener.cleanSessions();
            throw new RuntimeException(lce);
        } catch (OutOfMemoryError oom) {
            reset(request);
            SessionHoldingListener.cleanSessions();
            throw new RuntimeException(oom);
        }
    }

    public static DatabaseInfo getDatabaseInfo() {
        return databaseInfo;
    }
}
