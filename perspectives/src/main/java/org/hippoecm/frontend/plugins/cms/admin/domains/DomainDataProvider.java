/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.domains;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;

import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainDataProvider extends SortableDataProvider<Domain, String> {

    private static final Logger log = LoggerFactory.getLogger(DomainDataProvider.class);

    private static final String QUERY_DOMAIN_LIST = "SELECT * FROM hipposys:domain";

    private static final transient List<Domain> domainList = new ArrayList<>();
    private static volatile boolean dirty = true;

    private static String sessionId = "none";

    public DomainDataProvider() {
        setSort("name", SortOrder.ASCENDING);
    }

    @Override
    public Iterator<Domain> iterator(long first, long count) {
        final List<Domain> domains = getDomainList();
        domains.sort((domain1, domain2) -> {
            final int direction = getSort().isAscending() ? 1 : -1;
            return direction * domain1.compareTo(domain2);
        });

        final int endIndex = (int) Math.min(first + count, domains.size());
        return domains.subList((int) first, endIndex).iterator();
    }

    @Override
    public IModel<Domain> model(Domain domain) {
        return new DetachableDomain(domain);
    }

    @Override
    public long size() {
        return getDomainList().size();
    }

    /**
     * Actively invalidate cached list
     */
    public static void setDirty() {
        dirty = true;
    }

    /**
     * Populate list, refresh when a new session id is found or when dirty
     */
    private static void populateDomainList() {
        synchronized (DomainDataProvider.class) {
            if (!dirty && sessionId.equals(Session.get().getId())) {
                return;
            }
            domainList.clear();
            NodeIterator iterator;
            try {
                @SuppressWarnings("deprecation")
                Query listQuery = UserSession.get().getQueryManager().createQuery(QUERY_DOMAIN_LIST, Query.SQL);
                iterator = listQuery.execute().getNodes();
                while (iterator.hasNext()) {
                    Node node = iterator.nextNode();
                    if (node != null) {
                        try {
                            domainList.add(new Domain(node));
                        } catch (RepositoryException e) {
                            log.warn("Unable to instantiate new domain.", e);
                        }
                    }
                }
                Collections.sort(domainList);
                sessionId = Session.get().getId();
                dirty = false;
            } catch (RepositoryException e) {
                log.error("Error while trying to query domain nodes.", e);
            }
        }
    }

    public List<Domain> getDomainList() {
        populateDomainList();
        return domainList;
    }
}
