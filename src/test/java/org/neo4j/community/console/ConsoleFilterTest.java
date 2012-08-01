package org.neo4j.community.console;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;

import spark.servlet.SparkApplication;

/**
 * @author mh
 * @since 05.06.12
 */
public class ConsoleFilterTest {

    static DatabaseInfo databaseInfo;

    @Test
    public void testGetDatabaseFactory() throws Exception {
        final FilterConfig config = mock(FilterConfig.class);
        final ServletContext context = mock(ServletContext.class);
        final GraphDatabaseService database = mock(GraphDatabaseService.class);
        when(context.getAttribute(argThat(is(ConsoleFilter.DATABASE_ATTRIBUTE)))).thenReturn(DatabaseInfo.sandbox(database));
        when(config.getServletContext()).thenReturn(context);
        when(config.getInitParameter(argThat(is("applicationClass")))).thenReturn(TestApplication.class.getName());
        new ConsoleFilter().init(config);
        assertThat(databaseInfo.getDatabase(),is(database));
        assertThat(databaseInfo.isSandbox(),is(true));

    }

    public static class TestApplication implements SparkApplication {
        @Override
        public void init() {
            databaseInfo = ConsoleFilter.getDatabase();
        }
    }
}
