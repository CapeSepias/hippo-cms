/*
 *  Copyright 2009 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetSearchObserver {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";

    static final Logger log = LoggerFactory.getLogger(FacetSearchObserver.class);

    private WeakReference<Session> sessionRef;
    private Map<String, FacetSearchListener> listeners;
    private Set<UpstreamEntry> upstream;
    private boolean broadcast = false;

    public FacetSearchObserver(Session session) {
        this.sessionRef = new WeakReference<Session>(session);
        this.upstream = new HashSet<UpstreamEntry>();
        this.listeners = new HashMap<String, FacetSearchListener>();
    }

    /**
     * Broadcast facet search update events to listeners that observe ancestors
     * or descendants of a facetsearch node.  To be invoked when the session has
     * been refreshed.
     */
    public void broadcastEvents() {
        broadcast = true;
    }

    void start() {
        Session session = sessionRef.get();
        try {
            QueryManager queryMgr = session.getWorkspace().getQueryManager();
            Query query = queryMgr.createQuery("select * from " + HippoNodeType.NT_FACETSEARCH, Query.SQL);
            QueryResult result = query.execute();
            NodeIterator nodes = result.getNodes();

            ObservationManager obMgr = session.getWorkspace().getObservationManager();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                if (node != null) {
                    String uuid = node.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                    String docbase = session.getNodeByUUID(uuid).getPath();
                    listeners.put(node.getPath(), addFacetSearchListener(obMgr, docbase, node));
                }
            }
        } catch (RepositoryException ex) {
            log.error("Failure to subscribe to facetsearch nodes", ex);
        }
    }

    void stop() {
        Session session = sessionRef.get();
        if (session != null && session.isLive()) {
            try {
                ObservationManager obMgr = session.getWorkspace().getObservationManager();
                Iterator<Map.Entry<String, FacetSearchListener>> iter = listeners.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, FacetSearchListener> entry = iter.next();
                    obMgr.removeEventListener(entry.getValue());
                    iter.remove();
                }
            } catch (RepositoryException ex) {
                log.error("Failed to unsubscribe", ex);
            }
        }
    }

    void refresh() {
        if (broadcast) {
            broadcast = false;
            for (Map.Entry<String, FacetSearchListener> entry : listeners.entrySet()) {
                FacetSearchListener listener = entry.getValue();
                listener.broadcast();
            }
        }
    }

    void subscribe(EventListener listener, String basePath) {
        synchronized (upstream) {
            if (upstream.size() == 0) {
                start();
            }
            UpstreamEntry entry = new UpstreamEntry();
            entry.basePath = basePath;
            entry.listener = listener;
            upstream.add(entry);
        }
    }

    void unsubscribe(EventListener listener) {
        synchronized (upstream) {
            Iterator<UpstreamEntry> iter = upstream.iterator();
            while (iter.hasNext()) {
                UpstreamEntry entry = iter.next();
                if (entry.listener == listener) {
                    iter.remove();
                }
            }
            if (upstream.size() == 0) {
                stop();
            }
        }
    }

    private FacetSearchListener addFacetSearchListener(ObservationManager mgr, String docbase, Node node)
            throws RepositoryException {
        FacetSearchListener listener = new FacetSearchListener(node.getPath());
        mgr.addEventListener(listener, Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_CHANGED, docbase, true,
                null, null, false);
        return listener;
    }

    private static class UpstreamEntry {
        String basePath;
        EventListener listener;
    }

    private class FacetSearchListener implements EventListener {

        // path to the facetsearch node
        private String fsNodePath;
        private boolean refresh = false;

        FacetSearchListener(String path) {
            this.fsNodePath = path;
        }

        void broadcast() {
            if (refresh) {
                refresh = false;
                List<Event> base = new ArrayList<Event>(1);
                base.add(new Event() {

                    public String getPath() throws RepositoryException {
                        return fsNodePath;
                    }

                    public int getType() {
                        return 0;
                    }

                    public String getUserID() {
                        return "FacetSearchObserver";
                    }

                    public String getIdentifier() throws RepositoryException {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                    public Map getInfo() throws RepositoryException {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                    public String getUserData() throws RepositoryException {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                    public long getDate() throws RepositoryException {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                });
                List<UpstreamEntry> listeners;
                synchronized (upstream) {
                    listeners = new ArrayList<UpstreamEntry>(upstream);
                }
                for (UpstreamEntry listener : listeners) {
                    // notify listener if it registered at an ancestor or child
                    if (fsNodePath.startsWith(listener.basePath) || listener.basePath.startsWith(fsNodePath)) {
                        final Iterator<Event> baseIter = base.iterator();
                        if (log.isDebugEnabled()) {
                            log.error("Notifying listener at " + listener.basePath + " of change at " + fsNodePath);
                        }
                        listener.listener.onEvent(new EventIterator() {

                            public Event nextEvent() {
                                return baseIter.next();
                            }

                            public long getPosition() {
                                return 0;
                            }

                            public long getSize() {
                                return -1;
                            }

                            public void skip(long skipNum) {
                            }

                            public boolean hasNext() {
                                return baseIter.hasNext();
                            }

                            public Object next() {
                                return nextEvent();
                            }

                            public void remove() {
                                throw new UnsupportedOperationException("EventIterator is immutable");
                            }

                        });
                    }
                }
            }
        }

        public void onEvent(EventIterator events) {
            if (refresh) {
                return;
            }
            List<UpstreamEntry> listeners;
            synchronized (upstream) {
                listeners = new ArrayList<UpstreamEntry>(upstream);
            }
            for (UpstreamEntry listener : listeners) {
                // notify listener if it registered at an ancestor or child
                if (fsNodePath.startsWith(listener.basePath) || listener.basePath.startsWith(fsNodePath)) {
                    refresh = true;
                    break;
                }
            }
        }

    }

}
