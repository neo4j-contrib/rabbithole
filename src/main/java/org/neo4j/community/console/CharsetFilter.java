package org.neo4j.community.console;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

/**
 * @author mh
 * @since 25.11.12
 */
public class CharsetFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
            final String contentType = servletResponse.getContentType();
        if (contentType!=null && contentType.contains("text/html")) {
            servletResponse.setContentType("text/html;charset=UTF-8");
            if (servletResponse.getCharacterEncoding().equalsIgnoreCase("ISO-8859-1"))
                servletResponse.setCharacterEncoding("UTF-8");
        }
        chain.doFilter(servletRequest,servletResponse);
    }

    @Override
    public void destroy() { }
}
