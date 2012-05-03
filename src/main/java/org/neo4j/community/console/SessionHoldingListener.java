package org.neo4j.community.console;

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
    private static final Collection<WeakReference<HttpSession>> sessions = new HashSet<WeakReference<HttpSession>>();

    @Override
    public void sessionCreated(HttpSessionEvent httpSessionEvent) {
        final HttpSession session = httpSessionEvent.getSession();
        synchronized (sessions) {
            sessions.add(new WeakReference<HttpSession>(session));
        }
        System.err.println("Session created: " + session + " sessions " + sessions.size());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
        final HttpSession destroyedSession = httpSessionEvent.getSession();
        synchronized (sessions) {
            for (Iterator<WeakReference<HttpSession>> it = sessions.iterator(); it.hasNext(); ) {
                WeakReference<HttpSession> reference = it.next();
                final HttpSession session = reference.get();
                if (session == null || session.equals(destroyedSession)) {
                    SessionService.cleanSession(session, false);
                    it.remove();
                }
            }
        }
        System.err.println("Session destroyed: " + destroyedSession + " sessions " + sessions.size());
    }

    static void cleanSessions() {
        WeakReference[] clone;
        synchronized (sessions) {
            clone = sessions.toArray(new WeakReference[sessions.size()]);
        }
        System.err.println("Cleaning sessions " + sessions.size());
        for (WeakReference reference : clone) {
            final HttpSession session = (HttpSession) reference.get();
            if (session != null) {
                System.err.println("Cleaning session: " + session);
                SessionService.cleanSession(session, true);
            }
        }
        System.gc();
        System.err.println("Cleaned sessions " + sessions.size());
    }
}
