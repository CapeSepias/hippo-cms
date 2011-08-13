/*
 *  Copyright 2008 Hippo.
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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;

import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventJournal;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;
import javax.jcr.util.TraversingItemVisitor;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

enum TriState {
    FALSE, TRUE, UNKNOWN
};

public class JcrObservationManager implements ObservationManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(JcrObservationManager.class);

    private static JcrObservationManager INSTANCE = new JcrObservationManager();

    private final static MessageDigest BINARY_DIGEST;
    static {
        try {
            BINARY_DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private WeakHashMap<Session, Map<String, NodeState>> cache = new WeakHashMap<Session, Map<String, NodeState>>();

    public void setUserData(String userData) throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public EventJournal getEventJournal() throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public EventJournal getEventJournal(int eventTypes, String absPath, boolean isDeep, String[] uuid,
            String[] nodeTypeName) throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static class ObservationException extends Exception {
        private static final long serialVersionUID = 1L;

        public ObservationException(String message) {
            super(message);
        }
    }

    /**
     * Immutable class that contains the names of child nodes and the
     * names and values of properties.  Can be used to generate events
     * between two different states of a node.
     */
    private static class NodeState {

        class NodeEvent implements Event {

            int type;
            String name;

            NodeEvent(String name, int type) {
                this.name = name;
                this.type = type;
                if (type != 0 && name == null) {
                    throw new IllegalArgumentException("Name is mandatory when type of event is known");
                }
            }

            public String getPath() throws RepositoryException {
                if (type != 0) {
                    if (path.equals("/")) {
                        return path + name;
                    } else {
                        return path + "/" + name;
                    }
                } else {
                    return path;
                }
            }

            public int getType() {
                return type;
            }

            public String getUserID() {
                return userId;
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
        }

        private String path;
        private String userId;
        private Map<String, Value[]> properties;
        private List<String> nodes;

        NodeState(Node node, boolean skipBinaries) throws RepositoryException {
            this.path = node.getPath();
            this.userId = node.getSession().getUserID();

            properties = new HashMap<String, Value[]>();
            nodes = new LinkedList<String>();

            ValueFactory factory = node.getSession().getValueFactory();

            PropertyIterator propIter = node.getProperties();
            while (propIter.hasNext()) {
                Property property = propIter.nextProperty();
                // skip binaries, to prevent them being pulled from the database
                if (property.getType() != PropertyType.BINARY) {
                    if (property.getDefinition().isMultiple()) {
                        properties.put(property.getName(), property.getValues());
                    } else {
                        properties.put(property.getName(), new Value[] { property.getValue() });
                    }
                } else if (!skipBinaries && BINARY_DIGEST != null) {
                    if (property.getDefinition().isMultiple()) {
                        Value[] values = property.getValues();
                        Value[] hashes = new Value[values.length];
                        for (int i = 0; i < values.length; i++) {
                            byte[] md5 = BINARY_DIGEST.digest(values[i].getString().getBytes());
                            hashes[i] = factory.createValue(new BigInteger(1, md5).toString(16));
                        }
                        properties.put(property.getName(), hashes);
                    } else {
                        byte[] md5 = BINARY_DIGEST.digest(property.getValue().getString().getBytes());
                        properties.put(property.getName(), new Value[] { factory.createValue(new BigInteger(1, md5).toString(16)) });
                    }
                }
            }

            NodeIterator nodeIter = node.getNodes();
            while (nodeIter.hasNext()) {
                Node child = nodeIter.nextNode();
                if (child != null) {
                    try {
                        nodes.add(child.getName() + (child.getIndex() > 1 ? "[" + child.getIndex() + "]" : ""));
                    } catch (RepositoryException e) {
                        log.warn("Unable to add child node to list: " + e.getMessage());
                        log.debug("Error while adding child node to list: ", e);
                    }
                }
            }
        }

        Iterator<Event> getEvents(NodeState newState) throws RepositoryException {
            List<Event> events = new LinkedList<Event>();

            Map<String, Value[]> newProperties = newState.properties;
            List<String> nodes = newState.nodes;

            for (Map.Entry<String, Value[]> entry : this.properties.entrySet()) {
                if (newProperties.containsKey(entry.getKey())) {
                    Value[] oldValues = entry.getValue();
                    Value[] newValues = newProperties.get(entry.getKey());
                    if (newValues.length != oldValues.length) {
                        events.add(new NodeEvent(entry.getKey(), Event.PROPERTY_CHANGED));
                    } else {
                        for (int i = 0; i < newValues.length; i++) {
                            if (!newValues[i].equals(oldValues[i])) {
                                events.add(new NodeEvent(entry.getKey(), Event.PROPERTY_CHANGED));
                                break;
                            }
                        }
                    }
                } else {
                    events.add(new NodeEvent(entry.getKey(), Event.PROPERTY_REMOVED));
                }
            }
            for (Map.Entry<String, Value[]> entry : newProperties.entrySet()) {
                if (!this.properties.containsKey(entry.getKey())) {
                    events.add(new NodeEvent(entry.getKey(), Event.PROPERTY_ADDED));
                }
            }

            for (String child : nodes) {
                if (!this.nodes.contains(child)) {
                    events.add(new NodeEvent(child, Event.NODE_ADDED));
                }
            }
            for (String child : this.nodes) {
                if (!nodes.contains(child)) {
                    events.add(new NodeEvent(child, Event.NODE_REMOVED));
                }
            }
            return events.iterator();
        }

        Event getChangeEvent() {
            return new NodeEvent(null, 0);
        }

    }

    private class JcrListener extends WeakReference<EventListener> implements EventListener, Comparable<JcrListener> {
        String path;
        int eventTypes;
        boolean isDeep;
        Set<String> uuids;
        String[] nodeTypes;
        boolean noLocal;

        boolean isvirtual;
        List<String> fixed;
        List<Event> events;
        Session session;
        FacetRootsObserver fro;
        WeakReference<UserSession> sessionRef;

        JcrListener(UserSession userSession, EventListener upstream) {
            super(upstream, listenerQueue);
            this.events = new LinkedList<Event>();
            sessionRef = new WeakReference<UserSession>(userSession);
        }

        synchronized public void onEvent(EventIterator events) {
            while (events.hasNext()) {
                this.events.add(events.nextEvent());
            }
        }

        @Override
        public int compareTo(final JcrListener other) {
            int result = path.compareTo(other.path);
            if (result == 0) {
                return Integer.valueOf(hashCode()).compareTo(other.hashCode());
            }
            return result;
        }

        void init(int eventTypes, String absPath, boolean isDeep, String[] uuid, String[] nodeTypes, boolean noLocal)
                throws RepositoryException, ObservationException {
            if (sessionRef.get() == null) {
                throw new ObservationException("No session found");
            }
            if (!absPath.startsWith("/")) {
                throw new ObservationException("Invalid path");
            }

            this.path = absPath;
            this.eventTypes = eventTypes;
            this.isDeep = isDeep;
            if (uuid != null) {
                this.uuids = new HashSet<String>();
                for (String id : uuid) {
                    uuids.add(id);
                }
            }
            this.nodeTypes = nodeTypes;
            this.noLocal = noLocal;

            this.isvirtual = isVirtual(getRoot());

            session = getSession().getJcrSession();

            if (session != null) {
                subscribe();
            } else {
                log.error("No jcr session bound to wicket session");
            }

            fixed = new ArrayList<String>();
            if (!isDeep) {
                fixed.add(absPath);
            } else if (uuids != null) {
                for (Node node : getReferencedNodes()) {
                    fixed.add(node.getPath());
                }
            }
        }

        void dispose() {
            if (session != null) {
                if (log.isDebugEnabled()) {
                    log.debug("disposing listener " + this);
                }
                try {
                    unsubscribe();
                } catch (RepositoryException ex) {
                    log.error("Unable to unregister event listener, " + ex.getMessage());
                }
                session = null;
            }
            events.clear();
        }

        void subscribe() throws RepositoryException {
            ObservationManager obMgr = session.getWorkspace().getObservationManager();
            String[] uuid = null;
            if (uuids != null) {
                uuid = uuids.toArray(new String[uuids.size()]);
            }
            obMgr.addEventListener(this, eventTypes, path, isDeep, uuid, nodeTypes, noLocal);

            // subscribe to facet search observer.
            // FIXME due to HREPTWO-2655, will not be able to receive events on newly
            // created facet search nodes.
            fro = (FacetRootsObserver) getSession().getFacetRootsObserver();

            // subscribe when listening to deep tree structures;
            // there will/might be facetsearches in there.
            if (isDeep && uuids == null) {
                if (nodeTypes == null) {
                    fro.subscribe(this, path);
                } else {
                    for (String type : nodeTypes) {
                        if (type.equals(HippoNodeType.NT_DOCUMENT)) {
                            fro.subscribe(this, path);
                        }
                    }
                }
                return;
            }

            // subscribe when target has a facetsearch as an ancestor
            try {
                for (Node node = getRoot(); node.getDepth() > 0;) {
                    if (node.isNodeType(HippoNodeType.NT_FACETSEARCH)) {
                        fro.subscribe(this, node.getPath());
                        break;
                    }
                    node = node.getParent();
                }

                for (Node node : getReferencedNodes()) {
                    if (node.isNodeType(HippoNodeType.NT_FACETSEARCH)) {
                        fro.subscribe(this, node.getPath());
                    }
                }
            } catch (PathNotFoundException pnfe) {
                log.warn("Path no longer exists, stopping observation; " + pnfe.getMessage());
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }

        void unsubscribe() throws RepositoryException {
            fro.unsubscribe(this);
            fro = null;

            if (session.isLive()) {
                ObservationManager obMgr = session.getWorkspace().getObservationManager();
                obMgr.removeEventListener(this);
            }
            session = null;
        }

        UserSession getSession() {
            return sessionRef.get();
        }

        Node getRoot() throws PathNotFoundException, RepositoryException {
            Session session = getSession().getJcrSession();
            if ("/".equals(path)) {
                return session.getRootNode();
            } else {
                return session.getRootNode().getNode(path.substring(1));
            }
        }

        List<Node> getReferencedNodes() {
            if (uuids != null) {
                Session session = getSession().getJcrSession();
                ArrayList<Node> list = new ArrayList<Node>(uuids.size());
                Set<String> validUuids = new HashSet<String>(uuids.size());
                for (String id : uuids) {
                    try {
                        list.add(session.getNodeByUUID(id));
                        validUuids.add(id);
                    } catch (ItemNotFoundException e) {
                        log.info("Could not dereference uuid {} : {}", id, e.getMessage());
                    } catch (RepositoryException e) {
                        log.warn("Could not dereference uuid {} : {}", id, e.getMessage());
                    }
                }
                uuids = validUuids;
                return list;
            } else {
                return Collections.emptyList();
            }
        }

        boolean isVirtual(Node node) throws RepositoryException {
            if (node instanceof HippoNode) {
                try {
                    Node canonical = ((HippoNode) node).getCanonicalNode();
                    if (canonical == null) {
                        return true;
                    }
                    if (!canonical.isSame(node)) {
                        return true;
                    }
                } catch (ItemNotFoundException e) {
                    log.debug("Item not found, assuming node was virtual: " + e.getMessage());
                    return true;
                }
            }
            return false;
        }

        boolean isVisible(Node node) {
            try {
                String path = node.getPath();
                if ((isDeep && path.startsWith(this.path)) || path.equals(this.path)) {
                    if (uuids != null) {
                        if (node.isNodeType("mix:referenceable") && uuids.contains(node.getUUID())) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            } catch (RepositoryException e) {
                log.warn("Unable to determine if node is visible, defaulting to false: " + e.getMessage());
            }
            return false;
        }

        void processPending(NodeIterator iter, Set<Node> nodes) {
            while (iter.hasNext()) {
                Node node = iter.nextNode();
                if (node != null) {
                    if (isVisible(node)) {
                        nodes.add(node);
                    }
                }
            }
        }

        void expandNew(final Set<Node> nodes) {
            for (Node node : new ArrayList<Node>(nodes)) {
                if (node.isNew()) {
                    ItemVisitor visitor = new TraversingItemVisitor() {
                        public void visit(Node node) throws RepositoryException {
                            // do not traverse into virtual paths
                            if(!(node instanceof HippoNode) || node.isSame(((HippoNode)node).getCanonicalNode())) {
                                super.visit(node);
                            }
                        }

                        @Override
                        protected void entering(Property property, int level) throws RepositoryException {
                        }

                        @Override
                        protected void entering(Node node, int level) throws RepositoryException {
                            if (level > 0) {
                                nodes.add(node);
                            }
                        }

                        @Override
                        protected void leaving(Property property, int level) throws RepositoryException {
                        }

                        @Override
                        protected void leaving(Node node, int level) throws RepositoryException {
                        }

                    };
                    try {
                        visitor.visit(node);
                    } catch (RepositoryException e) {
                        log.warn("Error during visiting node", e);
                    }
                }
            }
        }

        boolean blocks(Event event) throws RepositoryException {

            // check event type
            long type = event.getType();
            if (type != 0 && (eventTypes & type) == 0) {
                return true;
            }

            String eventPath = event.getPath();
            if (type != 0) {
                eventPath = eventPath.substring(0, eventPath.lastIndexOf('/'));
                if (eventPath.equals("")) {
                    eventPath = "/";
                }
            }
            if (!session.itemExists(eventPath)) {
                return true;
            }
            Node parent = (Node) session.getItem(eventPath);

            // check UUIDs
            if (uuids != null) {
                if (!parent.isNodeType("mix:referenceable")) {
                    return true;
                }
                String parentId = parent.getUUID();
                boolean match = false;
                for (String uuid : uuids) {
                    if (uuid.equals(parentId)) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    return true;
                }
            }

            // check node types
            if (nodeTypes != null) {
                NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
                Set<NodeType> eventTypes = new HashSet<NodeType>();
                eventTypes.add(parent.getPrimaryNodeType());
                if (parent.hasProperty("jcr:mixinTypes")) {
                    Value[] mixins = parent.getProperty("jcr:mixinTypes").getValues();
                    for (Value mixin : mixins) {
                        eventTypes.add(ntMgr.getNodeType(mixin.getString()));
                    }
                }
                boolean match = false;
                for (int i = 0; i < nodeTypes.length && !match; i++) {
                    for (Iterator<NodeType> iter = eventTypes.iterator(); iter.hasNext();) {
                        NodeType nodeType = iter.next();
                        if (nodeType.isNodeType(nodeTypes[i])) {
                            match = true;
                            break;
                        }
                    }
                    if (match) {
                        break;
                    }
                }
                if (!match) {
                    return true;
                }
            }

            // finally check path
            boolean match = eventPath.equals(path);
            if (!match && isDeep) {
                match = isAncestor(path, eventPath);
            }
            return !match;
        }

        void checkSession() throws ObservationException {
            // listeners can be invoked after they have been removed
            if (session == null) {
                throw new ObservationException("Listener " + this + " is no longer registerd");
            } else if (!session.isLive()) {
                log.info("resubscribing listener " + this);

                // events have references to the session, so they are useless now
                events.clear();
                try {
                    unsubscribe();
                } catch (RepositoryException ex) {
                    log.debug("Failed to unsubscribe");
                }

                // get new session and subscribe event listener
                session = getSession().getJcrSession();
                if (session != null) {
                    try {
                        subscribe();
                    } catch (RepositoryException x) {
                        log.error("Failed to re-subscribe");
                    }
                } else {
                    throw new ObservationException("No session found");
                }
            }
        }

        synchronized void getChanges(Set<String> paths) {
            try {
                checkSession();
            } catch (ObservationException e1) {
                return;
            }

            if (events.size() > 0) {
                for (Event event : events) {
                    String path;
                    try {
                        path = event.getPath();
                        if (event.getType() != 0) {
                            path = path.substring(0, path.lastIndexOf('/'));
                        }
                        paths.add(path);
                    } catch (RepositoryException e) {
                        log.warn("Failed to get path from event", e);
                    }
                }
            }
        }

        void process(Map<String, NodeState> dirty) {
            try {
                checkSession();
            } catch (ObservationException ex) {
                log.debug(ex.getMessage(), ex);
                return;
            }

            List<Event> events = getEvents(dirty);
            final Iterator<Event> upstream = events.iterator();
            final long size = events.size();
            if (size > 0) {
                EventIterator iter = new EventIterator() {

                    public Event nextEvent() {
                        Event event = upstream.next();
                        log.debug("processing " + event.toString() + ", session " + sessionRef.get().getId());
                        return event;
                    }

                    public long getPosition() {
                        throw new UnsupportedOperationException("getPosition() is not implemented yet");
                    }

                    public long getSize() {
                        return size;
                    }

                    public void skip(long skipNum) {
                        throw new UnsupportedOperationException("skip() is not implemented yet");
                    }

                    public boolean hasNext() {
                        return upstream.hasNext();
                    }

                    public Object next() {
                        return nextEvent();
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("remove() is not implemented yet");
                    }

                };
                try {
                    EventListener listener = get();
                    if (listener != null) {
                        listener.onEvent(iter);
                    } else {
                        log.info("Listener disappeared during processing");
                    }
                } catch (RuntimeException ex) {
                    log.error("Error occured when processing event", ex);
                }
            }
        }

        synchronized List<Event> getEvents(Map<String, NodeState> dirty) {
            List<Event> events = new LinkedList<Event>(this.events);
            this.events.clear();

            if (isvirtual) {
                return events;
            }

            // process pending changes
            Node root = null;
            Set<Node> nodes = new TreeSet<Node>(new NodePathComparator());
            try {
                root = getRoot();
                if (nodeTypes == null) {
                    if ((root.isModified() || root.isNew()) && isVisible(root)) {
                        nodes.add(root);
                    }
                    // use pendingChanges to detect changes in sub-trees and properties
                    if (!root.isNew()) {
                        NodeIterator iter = ((HippoSession) root.getSession()).pendingChanges(root, null, false);
                        processPending(iter, nodes);
                    }
                } else {
                    if ((root.isModified() || root.isNew()) && isVisible(root)) {
                        for (String type : nodeTypes) {
                            try {
                                if (root.isNodeType(type)) {
                                    nodes.add(root);
                                    break;
                                }
                            } catch (RepositoryException e) {
                                log.debug("Unable to determine if node is of type " + type, e);
                            }
                        }
                    }
                    // use pendingChanges to detect changes in sub-trees and properties
                    if (!root.isNew()) {
                        for (String type : nodeTypes) {
                            NodeIterator iter = ((HippoSession) root.getSession()).pendingChanges(root, type, false);
                            processPending(iter, nodes);
                        }
                    }
                }
            } catch (PathNotFoundException ex) {
                log.warn("Root node no longer exists: " + ex.getMessage());
                dispose();
                return events;
            } catch (RepositoryException ex) {
                log.error("Failed to parse pending changes", ex);
                dispose();
                return events;
            }

            for (Node node : getReferencedNodes()) {
                if (nodeTypes == null) {
                    if (node.isModified() || node.isNew()) {
                        nodes.add(node);
                    }
                } else {
                    if (node.isModified() || node.isNew()) {
                        for (String type : nodeTypes) {
                            try {
                                if (root.isNodeType(type)) {
                                    nodes.add(root);
                                    break;
                                }
                            } catch (RepositoryException e) {
                                log.debug("Unable to determine if node is of type " + type, e);
                            }
                        }
                    }
                }
            }

            expandNew(nodes);

            List<String> paths = new LinkedList<String>();
            for (Node node : nodes) {
                String path;
                try {
                    path = node.getPath();
                    paths.add(path);

                    NodeState newState;
                    if (dirty.containsKey(path)) {
                        newState = dirty.get(path);
                    } else {
                        newState = new NodeState(node, false);
                        dirty.put(path, newState);
                    }

                    NodeState oldState = getNodeState(session, path);
                    if (oldState != null) {
                        Iterator<Event> iter = oldState.getEvents(newState);
                        while (iter.hasNext()) {
                            Event event = iter.next();
                            if (!blocks(event)) {
                                events.add(event);
                            }
                        }
                    } else {
                        Event changeEvent = newState.getChangeEvent();
                        if (!blocks(changeEvent)) {
                            events.add(changeEvent);
                        }
                    }
                } catch (RepositoryException e) {
                    log.warn("Failed to process node", e);
                }
            }
            return events;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("path", path)
                    .append("isDeep", isDeep).append("UUIDs", uuids).toString();
        }
    }

    public static JcrObservationManager getInstance() {
        return INSTANCE;
    }

    static boolean isAncestor(String candidate, String path) {
        String[] cParts = candidate.split("/");
        String[] pParts = path.split("/");

        // if candidate has more elements than path, then it cannot be an ancestor
        if (cParts.length > pParts.length) {
            return false;
        }

        for (int i = 0; i < cParts.length; i++) {
            String cEl = cParts[i];
            String pEl = pParts[i];
            if (cEl.endsWith("[1]")) {
                cEl = cEl.substring(0, cEl.length() - 3);
            }
            if (pEl.endsWith("[1]")) {
                pEl = pEl.substring(0, pEl.length() - 3);
            }
            if (!cEl.equals(pEl)) {
                return false;
            }
        }
        return true;
    }

    ReferenceQueue<EventListener> listenerQueue;
    Map<EventListener, JcrListener> listeners;

    private JcrObservationManager() {
        this.listeners = new WeakHashMap<EventListener, JcrListener>();
        this.listenerQueue = new ReferenceQueue<EventListener>();
    }

    public void addEventListener(EventListener listener, int eventTypes, String absPath, boolean isDeep, String[] uuid,
            String[] nodeTypeName, boolean noLocal) throws RepositoryException {
        cleanup();

        UserSession session = (UserSession) org.apache.wicket.Session.get();
        if (session != null) {
            JcrListener realListener = new JcrListener(session, listener);
            try {
                realListener.init(eventTypes, absPath, isDeep, uuid, nodeTypeName, noLocal);
                synchronized (listeners) {
                    listeners.put(listener, realListener);
                }

                // prefetch fixed nodes into cache
                if (realListener.fixed.size() > 0) {
                    Session jcrSession = session.getJcrSession();
                    Map<String, NodeState> states;
                    synchronized (cache) {
                        states = cache.get(jcrSession);
                        if (states == null) {
                            states = new HashMap<String, NodeState>();
                            cache.put(jcrSession, states);
                        }
                    }
                    synchronized (states) {
                        for (String path : realListener.fixed) {
                            if (!states.containsKey(path)) {
                                try {
                                    if (jcrSession.itemExists(path)) {
                                        NodeState state = new NodeState((Node) jcrSession.getItem(path), true);
                                        states.put(path, state);
                                    }
                                } catch (RepositoryException ex) {
                                    log.warn("Failed to initialize node state", ex);
                                }
                            }
                        }
                    }
                }
            } catch (ObservationException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.error("No session found");
        }
    }

    public EventListenerIterator getRegisteredEventListeners() throws RepositoryException {
        // create a local copy of the event listeners
        final Set<EventListener> currentListeners = new HashSet<EventListener>();
        synchronized (listeners) {
            for (EventListener el : listeners.keySet()) {
                currentListeners.add(el);
            }
        }

        return new SimpleEventListenerIterator(currentListeners);
    }

    public void removeEventListener(EventListener listener) throws RepositoryException {
        cleanup();

        JcrListener realListener = null;
        synchronized (listeners) {
            if (listeners.containsKey(listener)) {
                realListener = listeners.remove(listener);
            }
        }
        if (realListener != null) {
            realListener.dispose();
        } else {
            log.info("Listener was not registered");
        }
    }

    private NodeState getNodeState(Session session, String path) {
        synchronized (cache) {
            Map<String, NodeState> states = cache.get(session);
            if (states == null) {
                states = new HashMap<String, NodeState>();
                cache.put(session, states);
            }
            return states.get(path);
        }
    }

    private void prune(Set<String> paths) {
        // filter out descendants
        Iterator<String> pathIter = paths.iterator();
        while (pathIter.hasNext()) {
            String[] ancestors = pathIter.next().split("/");
            StringBuilder compound = new StringBuilder("/");
            for (int i = 1; i < ancestors.length - 1; i++) {
                compound.append(ancestors[i]);
                if (paths.contains(compound.toString())) {
                    pathIter.remove();
                    break;
                }
                compound.append('/');
            }
        }
    }

    public void refreshSession() {
        cleanup();

        UserSession session = (UserSession) org.apache.wicket.Session.get();
        if (session != null) {
            // copy set of listeners; don't synchronize on map while notifying observers
            // as it may need to be modified as a result of the event.
            SortedSet<JcrListener> set = new TreeSet<JcrListener>(new Comparator<JcrListener>() {

                public int compare(JcrListener o1, JcrListener o2) {
                    int result = o1.path.compareTo(o2.path);
                    if (result == 0) {
                        return Integer.valueOf(o1.hashCode()).compareTo(o2.hashCode());
                    }
                    return result;
                }

            });
            synchronized (listeners) {
                for (JcrListener listener : listeners.values()) {
                    if (listener.getSession() == session) {
                        set.add(listener);
                    }
                }
            }

            // notify facet search listeners.
            // FIXME due to HREPTWO-2655, will not be able to receive events on newly
            // created facet search nodes.
            FacetRootsObserver fso = (FacetRootsObserver) session.getFacetRootsObserver();
            fso.refresh();

            // create set of paths that need to be refreshed
            Set<String> paths = new TreeSet<String>();
            for (JcrListener listener : set) {
                listener.getChanges(paths);
            }

            Node root = session.getRootNode();
            if (root != null) {
                try {
                    if (paths.contains("")) {
                        root.refresh(true);
                    } else {
                        prune(paths);

                        // do the refresh
                        for (String path : paths) {
                            log.info("Refreshing {}, keeping changes", path);
                            try {
                                root.getNode(path.substring(1)).refresh(true);
                            } catch (PathNotFoundException ex) {
                                log.info("Could not find path " + path + " for event, discarding event and continue: "
                                        + ex.getMessage());
                            }
                        }
                    }
                } catch (RepositoryException ex) {
                    log.error("Failed to refresh session", ex);
                }
            } else {
                log.info("No root not found; cleaning up listeners");
                synchronized (listeners) {
                    for (Iterator<JcrListener> iter = listeners.values().iterator(); iter.hasNext();) {
                        JcrListener listener = iter.next();
                        if (listener.getSession() == session) {
                            iter.remove();
                        }
                    }
                }
            }
        } else {
            log.error("No session found");
        }
    }

    public void cleanupListeners(UserSession session) {
        cleanup();

        synchronized (listeners) {
            for (Iterator<JcrListener> iter = listeners.values().iterator(); iter.hasNext();) {
                JcrListener listener = iter.next();
                if (listener.getSession() == session) {
                    iter.remove();
                    listener.dispose();
                }
            }
        }
    }

    void processEvents() {
        cleanup();

        UserSession session = (UserSession) org.apache.wicket.Session.get();
        if (session != null) {
            // copy set of listeners; don't synchronize on map while notifying observers
            // as it may need to be modified as a result of the event.
            SortedSet<JcrListener> set = new TreeSet<JcrListener>();

            synchronized (listeners) {
                for (JcrListener listener : listeners.values()) {
                    if (listener.getSession() == session) {
                        set.add(listener);
                    }
                }
            }

            Map<String, NodeState> dirty = new HashMap<String, NodeState>();
            for (JcrListener listener : set) {
                listener.process(dirty);
            }

            Session jcrSession = session.getJcrSession();
            Map<String, NodeState> states;
            synchronized (cache) {
                states = cache.get(jcrSession);
                if (states == null) {
                    states = new HashMap<String, NodeState>();
                    cache.put(jcrSession, states);
                }
            }
            synchronized (states) {
                // update cache              *u
                for (Map.Entry<String, NodeState> nodes : dirty.entrySet()) {
                    states.put(nodes.getKey(), nodes.getValue());
                }

                // remove stale entries
                Iterator<Map.Entry<String, NodeState>> cacheIter = states.entrySet().iterator();
                while (cacheIter.hasNext()) {
                    Map.Entry<String, NodeState> entry = cacheIter.next();
                    try {
                        if (!jcrSession.itemExists(entry.getKey())) {
                            cacheIter.remove();
                        }
                    } catch (RepositoryException ex) {
                        log.warn("Could not determine whether " + entry.getKey() + " exists", ex);
                    }
                }
            }
        } else {
            log.error("No session found");
        }
    }

    private void cleanup() {
        Reference<? extends EventListener> ref;
        synchronized (listeners) {
            // cleanup weak-ref-table
            listeners.size();

            // cleanup gc'ed listeners
            while ((ref = listenerQueue.poll()) != null) {
                if (ref.get() != null) {
                    ((JcrListener) ref.get()).dispose();
                }
            }
        }
    }

    private static class SimpleEventListenerIterator implements EventListenerIterator {
        private final Iterator<EventListener> listenerIterator;
        private final long size;
        private long pos;

        public SimpleEventListenerIterator(final Set<EventListener> listeners) {
            listenerIterator = listeners.iterator();
            size = listeners.size();
            pos = 0;
        }

        public EventListener nextEventListener() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            pos++;
            return listenerIterator.next();
        }

        public void skip(long skipNum) {
            while (skipNum-- > 0) {
                next();
            }
        }

        public long getSize() {
            return size;
        }

        public long getPosition() {
            return pos;
        }

        public void remove() {
            throw new UnsupportedOperationException("EventListenerIterator.remove()");
        }

        public boolean hasNext() {
            return listenerIterator.hasNext();
        }

        public Object next() {
            return nextEventListener();
        }
    }

    private static class NodePathComparator implements Comparator<Node> {

        public int compare(Node o1, Node o2) {
            try {
                return o1.getPath().compareTo(o2.getPath());
            } catch (RepositoryException ex) {
                return 0;
            }
        }

    }

}
