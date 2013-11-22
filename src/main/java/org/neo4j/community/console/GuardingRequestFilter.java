package org.neo4j.community.console;

import org.slf4j.Logger;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.guard.Guard;
import org.neo4j.kernel.guard.GuardException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_REQUEST_TIMEOUT;

public class GuardingRequestFilter implements Filter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(GuardingRequestFilter.class);

    private int timeout;
    private int maxOps;

    private Guard getGuard(ServletRequest request) {
        Neo4jService service = SessionService.getService((HttpServletRequest) request,true);
        return ((GraphDatabaseAPI)service.getGraphDatabase()).getDependencyResolver().resolveDependency(Guard.class);
    }

    private void rollback(ServletRequest request) {
        Neo4jService service = SessionService.getService((HttpServletRequest) request,false);
        if (service==null) return;
        GraphDatabaseAPI graphDatabase = (GraphDatabaseAPI) service.getGraphDatabase();
        try {
            Transaction tx = graphDatabase.getDependencyResolver().resolveDependency(TransactionManager.class).getTransaction();
            if (tx!=null) tx.rollback();
        } catch(Exception e) {
            LOG.error("Error rolling back transaction ",e);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        maxOps = Integer.parseInt(filterConfig.getInitParameter("maxOps"));
        timeout = Integer.parseInt(filterConfig.getInitParameter("timeout"));
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws ServletException, IOException {
        if (req instanceof HttpServletRequest && res instanceof HttpServletResponse) {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;

            int timeLimit = getTimeLimit(request);
            if (timeLimit > 0) {
                Guard guard = getGuard(req);
                if (guard != null) {
                    //guard.startTimeout(timeLimit);
                    guard.startOperationsCount(maxOps);
                    try {
                        chain.doFilter(req, res);
                        return;
                    } catch (GuardException e) {
                        LOG.warn("Aborting Request "+dump(request));
                        response.setStatus(SC_REQUEST_TIMEOUT);
                        rollback(req);
                    } finally {
                        guard.stop();
                    }
                }
            }
        }
        chain.doFilter(req, res);
    }

    private String dump(HttpServletRequest req) {
        return req.getRemoteHost()+" "+req.getRequestURI()+" "+req.getQueryString()+" "+req.getRemoteUser();
    }

    @Override
    public void destroy() {
    }

    private int getTimeLimit(HttpServletRequest request) {
        int timeLimit = timeout;
        String headerValue = request.getHeader("max-execution-time");
        if (headerValue != null) {
            int maxHeader = Integer.parseInt(headerValue);
            if (timeLimit < 0 || (maxHeader > 0 && maxHeader < timeLimit)) {
                return maxHeader;
            }
        }
        return timeLimit;
    }
}
