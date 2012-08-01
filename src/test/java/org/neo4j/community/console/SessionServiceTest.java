package org.neo4j.community.console;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * @author mh
 * @since 05.06.12
 */
public class SessionServiceTest {
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
}
