/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions;

import java.io.Serializable;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standardworkflow.InitializationPayload;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.DocumentWorkflowAction;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowTransition;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPreviewWorkflowPlugin extends AbstractDocumentWorkflowPlugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(AbstractPreviewWorkflowPlugin.class);

    private final StdWorkflow infoAction;
    private final StdWorkflow infoEditAction;
    private final StdWorkflow editAction;
    private final Map<String, Serializable> info;

    protected AbstractPreviewWorkflowPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        final TypeTranslator translator = new TypeTranslator(new JcrNodeTypeModel(HippoStdNodeType.NT_PUBLISHABLESUMMARY));
        info = getHints();

        infoAction = new StdWorkflow("info", "info") {

            @Override
            public String getSubMenu() {
                return "info";
            }

            @Override
            protected IModel getTitle() {
                return translator.getValueName(HippoStdNodeType.HIPPOSTD_STATESUMMARY,
                        new PropertyModel<>(AbstractPreviewWorkflowPlugin.this, "stateSummary"));
            }

            @Override
            protected void invoke() {
                // The infoEdit workflow only shows feedback based on the hints.
                // It does not show any dialog.
            }

            @Override
            public boolean isVisible() {
                // Show the workflow status of the document, except when it is live (in that case,
                // no user action is required anymore).
                return !"live".equals(getStateSummary());
            }
        };
        add(infoAction);
        infoEditAction = getInfoEditAction();
        add(infoEditAction);

        editAction = new StdWorkflow("edit", new StringResourceModel("edit-label", this, null), getModel()) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.PENCIL_SQUARE);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                final WorkflowTransition obtainTransition = new WorkflowTransition.Builder()
                        .action(DocumentWorkflowAction.OBTAIN_EDITABLE_INSTANCE)
                        .initializationPayload(InitializationPayload.get())
                        .build();
                Document docRef = (Document) wf.transition(obtainTransition);
                Session session = UserSession.get().getJcrSession();
                session.refresh(true);
                Node docNode = session.getNodeByIdentifier(docRef.getIdentity());
                IEditorManager editorMgr = getPluginContext().getService(
                        getPluginConfig().getString(IEditorManager.EDITOR_ID), IEditorManager.class);
                if (editorMgr != null) {
                    JcrNodeModel docModel = new JcrNodeModel(docNode);
                    IEditor editor = editorMgr.getEditor(docModel);
                    if (editor == null) {
                        editorMgr.openEditor(docModel);
                    }
                } else {
                    log.warn("No editor found to edit {}", docNode.getPath());
                }
                return null;
            }
        };
        add(editAction);

        hideInvalidActions();
    }



    @SuppressWarnings("unused")  // used by a PropertyModel
    public String getStateSummary() {
        try {
            WorkflowDescriptorModel wdm = getModel();
            Node handleNode = wdm.getNode();
            for (Node child : new NodeIterable(handleNode.getNodes())) {
                if (child.hasProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY)) {
                    return child.getProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY).getString();
                }
            }
        } catch (RepositoryException ex) {
            log.warn("Unable to ascertain state summary", ex);
        }
        return StringUtils.EMPTY;
    }

    private void hideInvalidActions() {
        hideIfNotAllowed(info, "obtainEditableInstance", editAction);
        hideOrDisable(info, "status", infoAction);
        infoEditAction.setVisible(getInfoEditActionVisibility());
    }

    protected final String getHint(final String key){
        final Serializable serializable = info.get(key);
        if (serializable instanceof String) {
            return (String) serializable;
        }
        return StringUtils.EMPTY;
    }


    protected abstract boolean getInfoEditActionVisibility();

    protected abstract StdWorkflow getInfoEditAction();

}