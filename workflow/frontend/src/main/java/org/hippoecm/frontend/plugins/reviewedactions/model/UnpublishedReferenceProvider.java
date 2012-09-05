/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugins.reviewedactions.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortState;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnpublishedReferenceProvider implements ISortableDataProvider<String> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(UnpublishedReferenceProvider.class);

    private ReferenceProvider wrapped;
    private ISortState state = new SortState();
    private transient List<String> entries;

    public UnpublishedReferenceProvider(ReferenceProvider provider) {
        this.wrapped = provider;
    }

    public String getDocumentPath() {
        return wrapped.getNodeModel().getItemModel().getPath();
    }
    
    public Iterator<String> iterator(int first, int count) {
        load();
        if (first < entries.size()) {
            if ((first + count) <= entries.size()) {
                return entries.subList(first, first + count).iterator();
            } else {
                return entries.subList(first, entries.size()).iterator();
            }
        }
        return Collections.EMPTY_LIST.iterator();
    }

    public IModel<String> model(String object) {
        return new Model<String>(object);
    }

    public int size() {
        load();
        return entries.size();
    }

    public void detach() {
        wrapped.detach();
    }

    public ISortState getSortState() {
        return state;
    }

    public void setSortState(ISortState state) {
        this.state = state;
    }

    protected void load() {
        if (entries == null) {
            entries = new ArrayList<String>();
            Session session = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
            Iterator<String> upstream = wrapped.iterator(0, wrapped.size());
            try {
                while (upstream.hasNext()) {
                    String uuid = upstream.next();
                    try {
                        Node node = session.getNodeByUUID(uuid);
                        boolean valid = true;
                        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                            valid = false;

                            NodeIterator docs = node.getNodes(node.getName());
                            while (docs.hasNext()) {
                                Node document = docs.nextNode();
                                if (document.isNodeType(HippoStdNodeType.NT_PUBLISHABLE)) {
                                    String state = document.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString();
                                    if ("published".equals(state)) {
                                        valid = true;
                                        break;
                                    }
                                } else {
                                    valid = true;
                                    break;
                                }
                            }
                        }
                        if (!valid) {
                            entries.add(uuid);
                        }
                    } catch (ItemNotFoundException ex) {
                        log.debug("Reference to UUID " + uuid + " could not be dereferenced.");
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage(), ex);
            }
        }
    }

}
