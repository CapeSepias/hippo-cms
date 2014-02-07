/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.workflow.dialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.editor.workflow.model.DocumentMetadataEntry;
import org.hippoecm.frontend.service.IEditorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog that shows document metadata.
 */
public class DocumentMetadataDialog extends AbstractDialog {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(DocumentMetadataDialog.class);

    public DocumentMetadataDialog(WorkflowDescriptorModel model, IEditorManager editorMgr) {
        super(model);

        setOkVisible(false);
        setCancelLabel(new StringResourceModel("close", this, null));

        List<DocumentMetadataEntry> metaDataList = new ArrayList<>();
        try {
            final Node node = model.getNode();

            final Map<String, String> names = getNames(node);
            String namesLabel;
            if(names.size() > 1) {
                namesLabel = getString("document-names");
            } else {
                namesLabel = getString("document-name");
            }
            for(Map.Entry<String, String> entry : names.entrySet()) {
                StringBuilder name = new StringBuilder(entry.getValue());
                if(StringUtils.isNotBlank(entry.getKey())) {
                    name.append(" (");
                    name.append(entry.getKey());
                    name.append(")");
                }
                metaDataList.add(new DocumentMetadataEntry(namesLabel, name.toString()));
                namesLabel = StringUtils.EMPTY;
            }

            metaDataList.add(new DocumentMetadataEntry(getString("url-name"), node.getName()));

        } catch (RepositoryException e) {
            throw new WicketRuntimeException("No document node present", e);
        }


        add(new ListView("metadatalist", metaDataList) {
            protected void populateItem(ListItem item) {
                final DocumentMetadataEntry entry = (DocumentMetadataEntry) item.getModelObject();
                item.add(new Label("key", entry.getKey()));
                item.add(new Label("value", entry.getValue()));
            }
        });

    }

    private Map<String, String> getNames(final Node node) throws RepositoryException {
        Map<String, String> names = new HashMap<>();
        final NodeIterator nodeIterator = node.getParent().getNodes("hippo:translation");
        while(nodeIterator.hasNext()) {
            final Node translationNode = nodeIterator.nextNode();
            names.put(translationNode.getProperty("hippo:language").getString(),
                    translationNode.getProperty("hippo:message").getString());
        }
        return names;
    }

    public IModel getTitle() {
        return new StringResourceModel("document-info", this, null);
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.MEDIUM;
    }

}
