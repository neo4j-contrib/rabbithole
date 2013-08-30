package org.neo4j.community.console;

import org.neo4j.kernel.lifecycle.LifecycleException;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author mh
 * @since 09.04.12
 */
class SessionService {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(SessionService.class);

    private static final long TIMEOUT = TimeUnit.MINUTES.toMillis(20);
    private static final long SCHEDULE = TimeUnit.MINUTES.toMillis(1);

    public static final String SESSION_HEADER = "X-Session";

    private static DatabaseInfo databaseInfo;

    private static Map<String, Long> lastUsage = new HashMap<>();
    private static Map<String, Neo4jService> sessions=new HashMap<>();
    private static Timer timer = new Timer(true);
    static {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long unusedTime = System.currentTimeMillis()-TIMEOUT;
                Iterator<Map.Entry<String, Long>> it = lastUsage.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Long> entry = it.next();
                    if (entry.getValue() < unusedTime) {
                        cleanSession(entry.getKey());
                        it.remove();
                    }
                }
            }
        },SCHEDULE,SCHEDULE);
    }
    public static void setDatabaseInfo(DatabaseInfo databaseInfo) {
        SessionService.databaseInfo = databaseInfo;
    }

    public static void reset(final HttpServletRequest httpRequest) {
        String sessionId = getSessionId(httpRequest);
        if (sessionId==null) return;
        Neo4jService service = sessions.get(sessionId);
        if (service == null) return;
        try {
            service.stop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sessions.remove(sessionId);
            lastUsage.remove(sessionId);
        }
    }

    public static void cleanSession(String sessionId) {
        Neo4jService service = sessions.get(sessionId);
        if (service == null) return;
        try {
            service.stop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sessions.remove(sessionId);
        }
    }

    public static Neo4jService getService(final HttpServletRequest request,boolean create) {
        String sessionId = getSessionId(request);
        try {
            Neo4jService service = sessions.get(sessionId);
            lastUsage.put(sessionId,System.currentTimeMillis());
            if (service != null) {
                LOG.warn("Session found for "+sessionId+" request "+request.getRequestURI());
                return service;
            }
            if (!create) throw new IllegalStateException("No Service for session "+sessionId+" available");
//            LOG.info("Session created for "+sessionId+" request "+request.getRequestURI());

            service = databaseInfo.shouldCreateNew() ? new Neo4jService() : new Neo4jService(databaseInfo.getDatabase());
            if (databaseInfo.shouldImport()) {
                service.initializeFrom(SubGraph.from(databaseInfo.getDatabase()));
            }
            sessions.put(sessionId, service);
            return service;
        } catch (IllegalStateException ise) {
            throw ise;
        } catch (LifecycleException | OutOfMemoryError e) {
            cleanSessions();
            throw e;
        } catch (Throwable t) {
            reset(request);
            throw new RuntimeException(t);
        }
    }

    public static void cleanSessions() {
        synchronized(sessions) {
            Iterator<Map.Entry<String,Neo4jService>> it = sessions.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Neo4jService> entry = it.next();
                Neo4jService service = entry.getValue();
                if (service !=null) {
                    try {
                        service.stop();
                    } catch (Exception e) {
                        // ignore
                    }
                }
                it.remove();
            }
        }
        lastUsage.clear();
    }

    private static String getSessionId(HttpServletRequest request) {
        return request.getHeader(SESSION_HEADER);
    }

    public static DatabaseInfo getDatabaseInfo() {
        return databaseInfo;
    }
}
