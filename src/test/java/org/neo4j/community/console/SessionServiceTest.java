package org.neo4j.community.console;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;

/**
 * @author mh
 * @since 05.06.12
 */
public class SessionServiceTest {

    public static final String SESSION_ID = "123";
    public static final String X_SESSION = "X-Session";

    @After
    public void tearDown() throws Exception {
        SessionService.cleanSessions();
    }

    @Test
    public void testGetService() throws Exception {
        final GraphDatabaseService database = mock(GraphDatabaseService.class);
        SessionService.setDatabaseInfo(DatabaseInfo.expose(database));
        final DatabaseInfo newDatabase = SessionService.getDatabaseInfo();
        assertThat(newDatabase.getDatabase(),is(database));
        assertThat(newDatabase.isSandbox(),is(false));
        assertThat(newDatabase.shouldImport(),is(false));
        assertThat(newDatabase.shouldCreateNew(),is(false));
    }

    @Test
    public void testGetSessionFromService() throws Exception {
        final GraphDatabaseService database = new TestGraphDatabaseFactory().newImpermanentDatabase();
        SessionService.setDatabaseInfo(DatabaseInfo.expose(database));
        serviceForId(SESSION_ID, true);
        serviceForId(SESSION_ID, false);
        noServiceForId(SESSION_ID+"foo",false);
    }

    @Test
    public void testGetServiceFromRequestIfNotAvailable() throws Exception {
        noServiceForId(SESSION_ID, false);
    }

    private void noServiceForId(String sessionId, boolean create) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Mockito.when(request.getHeader(eq(X_SESSION))).thenReturn(sessionId);
        try {
            SessionService.getService(request, create);
            fail("should not have a service available");
        } catch (RuntimeException e) {
            assertEquals("No Service for session " + sessionId + " available", e.getMessage());
        }
    }
    private Neo4jService serviceForId(String sessionId, boolean create) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Mockito.when(request.getHeader(eq(X_SESSION))).thenReturn(sessionId);
        Neo4jService service = SessionService.getService(request, create);
        assertNotNull(service);
        return service;
    }
}
