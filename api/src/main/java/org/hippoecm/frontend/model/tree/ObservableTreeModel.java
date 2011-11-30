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
package org.hippoecm.frontend.model.tree;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.event.JcrEvent;
import org.hippoecm.frontend.model.event.JcrEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JCR tree model implementation that can be shared by multiple tree instances.
 * It is observable and can therefore not be used to register listeners.  Use the {@link JcrTreeModel}
 * instead to register {@link TreeModelListener}s.
 */
public class ObservableTreeModel extends DefaultTreeModel implements IJcrTreeModel, IObservable, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public class ObservableTreeModelEvent implements IEvent<ObservableTreeModel> {

        private JcrEvent jcrEvent;

        public ObservableTreeModelEvent(JcrEvent event) {
            this.jcrEvent = event;
        }

        public JcrEvent getJcrEvent() {
            return jcrEvent;
        }

        public ObservableTreeModel getSource() {
            return ObservableTreeModel.this;
        }

    }

    final static Logger log = LoggerFactory.getLogger(ObservableTreeModel.class);

    private IObservationContext<ObservableTreeModel> observationContext;
    private JcrEventListener listener;
    protected final IJcrTreeNode root;

    public ObservableTreeModel(IJcrTreeNode rootModel) {
        super(rootModel);

        root = rootModel;
    }

    public TreePath lookup(JcrNodeModel nodeModel) {
        IJcrTreeNode node = root;
        if (nodeModel != null) {
            String basePath = ((JcrNodeModel) root.getNodeModel()).getItemModel().getPath();
            String path = nodeModel.getItemModel().getPath();
            if (path != null && path.startsWith(basePath)) {
                String[] elements = StringUtils.split(path.substring(basePath.length()), '/');
                List<Object> nodes = new LinkedList<Object>();
                nodes.add(node);
                try {
                    for (String element : elements) {
                        IJcrTreeNode child = node.getChild(element);
                        if (child != null) {
                            nodes.add(child);
                            node = child;
                        } else {
                            break;
                        }
                    }
                } catch (RepositoryException ex) {
                    log.error("Unable to find node in tree", ex.getMessage());
                }
                return new TreePath(nodes.toArray(new Object[nodes.size()]));
            }
        }
        return null;
    }

    public void detach() {
        root.detach();
    }

    @SuppressWarnings("unchecked")
    public void setObservationContext(IObservationContext<? extends IObservable> context) {
        this.observationContext = (IObservationContext<ObservableTreeModel>) context;
    }

    public void startObservation() {
        listener = new JcrEventListener(
                new IObservationContext<JcrNodeModel>() {
                    private static final long serialVersionUID = 1L;

                    public void notifyObservers(EventCollection<IEvent<JcrNodeModel>> events) {
                        EventCollection<IEvent<ObservableTreeModel>> treeModelEvents = new EventCollection<IEvent<ObservableTreeModel>>();
                        for (IEvent<JcrNodeModel> event : events) {
                            IEvent<ObservableTreeModel> treeModelEvent = new ObservableTreeModelEvent((JcrEvent) event);
                            treeModelEvents.add(treeModelEvent);
                        }
                        observationContext.notifyObservers(treeModelEvents);
                    }

                    public void registerObserver(IObserver<?> observer) {
                        observationContext.registerObserver(observer);
                    }

                    public void unregisterObserver(IObserver<?> observer) {
                        observationContext.registerObserver(observer);

                    }
                }, Event.NODE_REMOVED | Event.NODE_ADDED | Event.NODE_MOVED,
                ((JcrNodeModel) root.getNodeModel()).getItemModel().getPath(), true,
                null, null);
        listener.start();
    }

    public void stopObservation() {
        if (listener != null) {
            listener.stop();
            listener = null;
        }
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ObservableTreeModel) {
            return root.equals(((ObservableTreeModel) obj).root);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 67).append(root).toHashCode();
    }

}
