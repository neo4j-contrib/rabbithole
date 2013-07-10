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
public class AllowAjaxFilter implements Filter {
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN      = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_METHODS     = "Access-Control-Allow-Methods";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS     = "Access-Control-Allow-Headers";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String ACCESS_CONTROL_REQUEST_METHOD = "access-control-request-method";
    private static final String ACCESS_CONTROL_REQUEST_HEADERS = "access-control-request-headers";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException { }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request= (HttpServletRequest) servletRequest;
        HttpServletResponse response= (HttpServletResponse) servletResponse;

        String origin=request.getHeader("origin");
        if (origin!=null) response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        else response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.addHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

        // Allow all forms of requests
        final Enumeration aclMethods = request.getHeaders(ACCESS_CONTROL_REQUEST_METHOD);
        while (aclMethods.hasMoreElements()) {
            String value = (String) aclMethods.nextElement();
            response.addHeader( ACCESS_CONTROL_ALLOW_METHODS, value );
        }

        // Allow all types of headers
        final Enumeration aclHeaders = request.getHeaders(ACCESS_CONTROL_REQUEST_HEADERS);
        while (aclHeaders.hasMoreElements()) {
            String value = (String) aclHeaders.nextElement();
            response.addHeader( ACCESS_CONTROL_ALLOW_HEADERS, value );
        }

        chain.doFilter(request,response);
    }



    @Override
    public void destroy() { }
}
