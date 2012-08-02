/*
 *  Copyright 2009-2011 Hippo.
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
package org.hippoecm.frontend.observation;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
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

import org.apache.jackrabbit.JcrConstants;
import org.hippoecm.frontend.observation.IFacetRootsObserver;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetRootsObserver implements IFacetRootsObserver {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";

    static final Logger log = LoggerFactory.getLogger(FacetRootsObserver.class);

    private WeakReference<Session> sessionRef;
    private Map<String, List<FacetRootListener>> listeners;
    private Set<UpstreamEntry> upstream;
    private boolean broadcast = false;
    private boolean disabled = Boolean.getBoolean("facetrootsobserver.disabled");

    public FacetRootsObserver(Session session) {
        this.sessionRef = new WeakReference<Session>(session);
        this.upstream = new HashSet<UpstreamEntry>();
        this.listeners = new HashMap<String, List<FacetRootListener>>();
        log.info("Facet roots observation is {}", disabled ? "disabled" : "enabled");
    }

    /**
     * Broadcast facet roots update events to listeners that observe ancestors
     * or descendants of a facet root node.  To be invoked when the session has
     * been refreshed.
     */
    public void broadcastEvents() {
        broadcast = true;
    }

    void start() {
        Session session = sessionRef.get();
        try {
            QueryManager queryMgr = session.getWorkspace().getQueryManager();
            Query query = queryMgr.createQuery("select * from " + JcrConstants.NT_BASE + " where "
                    + JcrConstants.JCR_PRIMARYTYPE + "='" + HippoNodeType.NT_FACETSEARCH + "' or "
                    + JcrConstants.JCR_PRIMARYTYPE + "='hippofacnav:facetnavigation'", Query.SQL);
            QueryResult result = query.execute();
            NodeIterator nodes = result.getNodes();

            ObservationManager obMgr = session.getWorkspace().getObservationManager();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                if (node != null) {
                    String uuid = node.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                    String[] uuids = {uuid};
                    if ("hippofacnav:facetnavigation".equals(node.getPrimaryNodeType().getName())) {
                        uuids = uuid.split(",\\s*");
                    }
                    for (String id : uuids) {
                        try {
                            String docbase = session.getNodeByIdentifier(id).getPath();
                            List<FacetRootListener> rootListeners = listeners.get(node.getPath());
                            if (rootListeners == null) {
                                rootListeners = new LinkedList<FacetRootListener>();
                                listeners.put(node.getPath(),rootListeners);
                            }
                            // CMS7-5568: facet navigation can have multiple docbases.
                            rootListeners.add(addFacetSearchListener(obMgr, docbase, node));
                        } catch (ItemNotFoundException e) {
                            log.warn("The {} property of facet node {} refers to a non-existing UUID '{}'",
                                    new Object[]{HippoNodeType.HIPPO_DOCBASE, node.getPath(), uuid});
                        }
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error("Failure to subscribe to facet root nodes", ex);
        }
    }

    void stop() {
        Session session = sessionRef.get();
        if (session != null && session.isLive()) {
            try {
                ObservationManager obMgr = session.getWorkspace().getObservationManager();
                Iterator<Map.Entry<String, List<FacetRootListener>>> iter = listeners.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, List<FacetRootListener>> entry = iter.next();
                    for (FacetRootListener listener : entry.getValue()) {
                        obMgr.removeEventListener(listener);
                    }
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
            Iterator<Map.Entry<String, List<FacetRootListener>>> iter = listeners.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, List<FacetRootListener>> entry = iter.next();
                for (FacetRootListener listener : entry.getValue()) {
                    listener.broadcast();
                }
            }
        }
    }

    void subscribe(EventListener listener, String basePath) {
        if (disabled) {
            return;
        }
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

    private FacetRootListener addFacetSearchListener(ObservationManager mgr, String docbase, Node node)
            throws RepositoryException {
        FacetRootListener listener = new FacetRootListener(node.getPath());
        mgr.addEventListener(listener, Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_CHANGED, docbase, true,
                null, null, false);
        return listener;
    }

    private static class UpstreamEntry {
        String basePath;
        EventListener listener;
    }

    private class FacetRootListener implements EventListener {

        // path to the facet root node
        private String nodePath;
        private boolean refresh = false;

        FacetRootListener(String path) {
            this.nodePath = path;
        }

        void broadcast() {
            if (refresh) {
                refresh = false;
                List<Event> base = new ArrayList<Event>(1);
                base.add(new Event() {

                    public String getPath() throws RepositoryException {
                        return nodePath;
                    }

                    public int getType() {
                        return 0;
                    }

                    public String getUserID() {
                        return "FacetRootsObserver";
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
                    if (nodePath.startsWith(listener.basePath) || listener.basePath.startsWith(nodePath)) {
                        final Iterator<Event> baseIter = base.iterator();
                        if (log.isDebugEnabled()) {
                            log.error("Notifying listener at " + listener.basePath + " of change at " + nodePath);
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
                if (nodePath.startsWith(listener.basePath) || listener.basePath.startsWith(nodePath)) {
                    refresh = true;
                    break;
                }
            }
        }

    }

}
