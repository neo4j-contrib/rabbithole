package org.neo4j.community.console;

import org.slf4j.Logger;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * @author mh
 * @since 09.04.12
 */
public class SessionHoldingListener implements HttpSessionListener {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(SessionHoldingListener.class);

    private long counter=0;

    @Override
    public void sessionCreated(HttpSessionEvent httpSessionEvent) {
        final HttpSession session = httpSessionEvent.getSession();
        counter++;
        LOG.warn("Session created: " + session + " sessions " + counter);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
        final HttpSession session = httpSessionEvent.getSession();
        counter--;
        LOG.warn("Session destroyed: " + session + " sessions " + counter);
    }
}

