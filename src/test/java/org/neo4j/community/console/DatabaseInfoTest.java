package org.neo4j.community.console;

import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author mh
 * @since 05.06.12
 */
public class DatabaseInfoTest {

    private final GraphDatabaseService database = mock(GraphDatabaseService.class);

    @Test
    public void testExpose() throws Exception {
        final DatabaseInfo info = DatabaseInfo.expose(database);
        assertThat(info.getDatabase(),is(database));
        assertThat(info.shouldCreateNew(),is(false));
        assertThat(info.shouldImport(),is(false));
        assertThat(info.isSandbox(),is(false));
    }

    @Test
    public void testSandbox() throws Exception {
        final DatabaseInfo info = DatabaseInfo.sandbox(database);
        assertThat(info.getDatabase(),is(database));
        assertThat(info.shouldCreateNew(),is(true));
        assertThat(info.shouldImport(),is(true));
        assertThat(info.isSandbox(),is(true));

    }
    @Test
    public void testSandboxNullDatabase() throws Exception {
        final DatabaseInfo info = DatabaseInfo.sandbox(null);
        assertThat(info.getDatabase(),is((GraphDatabaseService)null));
        assertThat(info.shouldCreateNew(),is(true));
        assertThat(info.shouldImport(),is(false));
        assertThat(info.isSandbox(),is(true));

    }

    @Test
    public void testExposeNullDatabase() throws Exception {
        final DatabaseInfo info = DatabaseInfo.expose(null);
        assertThat(info.getDatabase(),is((GraphDatabaseService)null));
        assertThat(info.shouldCreateNew(),is(true));
        assertThat(info.shouldImport(),is(false));
        assertThat(info.isSandbox(),is(false));

    }
}
