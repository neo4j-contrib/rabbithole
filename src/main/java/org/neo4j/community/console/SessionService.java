package org.neo4j.community.console;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author mh
 * @since 09.04.12
 */
class SessionService {
    public static void reset(final HttpServletRequest httpRequest) {
        final HttpSession session = httpRequest.getSession(false);
        cleanSession(session, true);
    }

    public static void cleanSession(HttpSession session, final boolean invalidate) {
        if (session == null) return;
        Neo4jService service = (Neo4jService) session.getAttribute("service");
        if (service != null) {
            try {
                service.stop();
                session.removeAttribute("service");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (invalidate) {
            session.invalidate();
        }
    }

    public static Neo4jService getService(final HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        Neo4jService service = (Neo4jService) session.getAttribute("service");
        if (service != null) return service;

        service = new Neo4jService();
        session.setAttribute("service", service);
        return service;
    }
}
