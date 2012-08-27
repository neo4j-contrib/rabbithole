package org.neo4j.community.console;

import spark.servlet.SparkFilter;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * @author mh
 * @since 05.06.12
 */
public class ConsoleFilter extends SparkFilter {
    private final static ThreadLocal<ServletContext> ctx=new ThreadLocal<ServletContext>();
    public static final String DATABASE_ATTRIBUTE = "database";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ctx.set(filterConfig.getServletContext());
        super.init(filterConfig);
        ctx.remove();
    }

    public static DatabaseInfo getDatabase() {
        return getDatabase(ctx.get());
    }

    public static DatabaseInfo getDatabase(final ServletContext context) {
        if (context == null || context.getAttribute(DATABASE_ATTRIBUTE) == null) {
            return null;
        }
        return (DatabaseInfo) context.getAttribute(DATABASE_ATTRIBUTE);
    }
}
