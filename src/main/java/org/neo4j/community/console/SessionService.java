package org.neo4j.community.console;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("UTC"));

    private static DatabaseInfo databaseInfo;

    private static Map<String, Long> lastUsage = new HashMap<>();
    private static final Map<String, Neo4jService> sessions = new ConcurrentHashMap<>();
    private static Timer timer = new Timer(true);
    private static Driver driver;

    static {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long unusedTime = System.currentTimeMillis() - TIMEOUT;
                Set<Map.Entry<String, Long>> entries = new HashSet<>(lastUsage.entrySet());
                for (Map.Entry<String, Long> entry : entries) {
                    if (entry.getValue() < unusedTime) {
                        cleanSession(entry.getKey());
                        lastUsage.remove(entry.getKey());
                    }
                }
            }
        }, SCHEDULE, SCHEDULE);
    }

    public static void setDatabaseInfo(DatabaseInfo databaseInfo) {
        SessionService.databaseInfo = databaseInfo;
    }
    public static void setDriver(Driver driver) {
        SessionService.driver = driver;
    }

    public static void reset(final HttpServletRequest httpRequest) {
        String sessionId = getSessionId(httpRequest);
        if (sessionId == null) return;
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

    public static Neo4jService getService(final HttpServletRequest request, boolean create) {
        String sessionId = getSessionId(request);
        try {
            Neo4jService service = sessions.get(sessionId);
            lastUsage.put(sessionId, System.currentTimeMillis());
            if (service != null) return service;
            if (!create) throw new IllegalStateException("No Service for session " + sessionId + " available");
            synchronized (sessions) {
                service = sessions.get(sessionId);
                if (service != null) return service;
                LOG.warn("Session created for " + sessionId + " request " + request.getRequestURI());
                service = createSession(sessionId);
                sessions.put(sessionId, service);
                return service;
            }
        } catch (IllegalStateException ise) {
            throw ise;
        } catch (OutOfMemoryError e) {
            cleanSessions();
            throw e;
        } catch (Throwable t) {
            reset(request);
            throw new RuntimeException(t);
        }
    }

    private static Neo4jService createSession(String sessionId) {
        Neo4jService service;
        if (databaseInfo.shouldCreateNew()) {
            String dbName = "db"+(sessionId + FORMAT.format(Instant.now())).replaceAll("[_.-]+","");
            systemOperation(dbName, CREATE_DATABASE_COMMANDS);
            service = new Neo4jService(driver, dbName, true);
        } else {
            service = new Neo4jService(driver, databaseInfo.getDatabase(), false);
        }
        if (databaseInfo.shouldImport()) {
            service.initializeFrom(SubGraph.from(driver, databaseInfo.getDatabase()));
        }
        return service;
    }

    private final static String CREATE_DATABASE_COMMANDS = "CREATE DATABASE `$db`;" +
            "CREATE USER `$db` SET PASSWORD '$db' SET PASSWORD CHANGE NOT REQUIRED;" +
            "CREATE ROLE `$db`;" +
            "GRANT ROLE `$db` TO `$db`;" +
            "GRANT ALL ON DATABASE `$db` TO `$db`;" +
            "GRANT ACCESS ON DATABASE `$db` TO `$db`;" +
            "GRANT READ {*} ON GRAPH `$db` TO `$db`;" +
            "GRANT TRAVERSE ON GRAPH `$db` TO `$db`;" +
            "GRANT WRITE ON GRAPH `$db` TO `$db`";

    private final static String DROP_DATABASE_COMMANDS =
            "STOP DATABASE `$db`;" +
            "DROP DATABASE `$db`;" +
            "DROP USER `$db`;" +
            "DROP ROLE `$db`";

    private static void systemOperation(String name, String commands) {
        Session session = driver.session(SessionConfig.builder().withDatabase("system").build());
        session.writeTransaction((tx) -> {
            for (String statement : commands.replaceAll("\\$db", name).split(";")) {
                tx.run(statement);
            }
            tx.commit();
            return null;
        });
    }

    public static void cleanSessions() {
        Set<Map.Entry<String, Neo4jService>> entries = new HashSet<>();
        synchronized (sessions) {
            entries.addAll(sessions.entrySet());
        }
        for (Map.Entry<String, Neo4jService> entry : entries) {
            Neo4jService service = entry.getValue();
            if (service != null) {
                try {
                    if (service.doesOwnDatabase()) systemOperation(service.getDb(),DROP_DATABASE_COMMANDS);
                    service.stop();
                } catch (Exception e) {
                    // ignore
                }
            }
            sessions.remove(entry.getKey());
            lastUsage.remove(entry.getKey());
        }
    }

    static String getSessionId(HttpServletRequest request) {
        return request.getHeader(SESSION_HEADER);
    }

    public static DatabaseInfo getDatabaseInfo() {
        return databaseInfo;
    }
}
