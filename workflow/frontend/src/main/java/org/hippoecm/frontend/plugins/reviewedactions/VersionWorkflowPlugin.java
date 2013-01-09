/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.version.Version;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.HistoryDialog;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;
import org.hippoecm.repository.standardworkflow.VersionWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionWorkflowPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(VersionWorkflowPlugin.class);

    public VersionWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow("info", "info") {
            @Override
            protected IModel getTitle() {
                return new StringResourceModel("created", this, null, new Object[] { new IModel() {

                    public Object getObject() {
                        try {
                            Node frozenNode = ((WorkflowDescriptorModel) VersionWorkflowPlugin.this.getDefaultModel()).getNode();
                            Node versionNode = frozenNode.getParent();
                            Calendar calendar = versionNode.getProperty("jcr:created").getDate();
                            return calendar.getTime();
                        } catch (ValueFormatException e) {
                            log.error("Value is not a date", e);
                        } catch (PathNotFoundException e) {
                            log.error("Could not find node", e);
                        } catch (RepositoryException e) {
                            log.error("Repository error", e);
                        }
                        return null;
                    }

                    public void setObject(Object object) {

                    }

                    public void detach() {
                    }

                } }, "unknown");
            }

            @Override
            protected void invoke() {
            }
        });

        add(new StdWorkflow("restore", new StringResourceModel("restore", this, null).getString(), null, context, this) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "restore-16.png");
            }

            @Override
            public boolean isVisible() {
                Node frozenNode;
                try {
                    frozenNode = ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                    String primaryType = frozenNode.getProperty("jcr:frozenPrimaryType").getString();
                    String prefix = primaryType.substring(0, primaryType.indexOf(':'));
                    if (prefix.contains("_")) {
                        return false;
                    }
                } catch (RepositoryException e) {
                    log.warn("Could not determine whether to enable restore button", e);
                }
                return true;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                Node frozenNode = ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                Session session = frozenNode.getSession();
                Version versionNode = (Version) frozenNode.getParent();
                Version handleVersion = JcrHelper.getVersionParent(versionNode);
                Node handle = session.getNodeByIdentifier(
                        handleVersion.getContainingHistory().getVersionableIdentifier());

                WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace())
                        .getWorkflowManager();

                Node unpublished = null;
                Node document = null;
                NodeIterator docs = handle.getNodes(handle.getName());
                while (docs.hasNext()) {
                    document = docs.nextNode();
                    if (document.hasProperty("hippostd:state")
                            && "unpublished".equals(document.getProperty("hippostd:state").getString())) {
                        unpublished = document;
                    }
                }

                if (document == null) {
                    return "document has been deleted";
                }

                if (unpublished != null) {
                    // create a revision to prevent loss of content from unpublished.
                    VersionWorkflow versionWorkflow = (VersionWorkflow) workflowManager.getWorkflow("versioning",
                            unpublished);
                    versionWorkflow.version();
                }

                BasicReviewedActionsWorkflow braw = (BasicReviewedActionsWorkflow) workflowManager.getWorkflow(
                        "default", document);
                Document doc = braw.obtainEditableInstance();

                try {
                    VersionWorkflow versionWorkflow = (VersionWorkflow) workflowManager.getWorkflow("versioning",
                            frozenNode);
                    versionWorkflow.restoreTo(doc);
                } finally {
                    doc = braw.commitEditableInstance();
                }

                JcrNodeModel unpubModel = new JcrNodeModel(session.getNodeByIdentifier(doc.getIdentity()));
                IEditorManager editorMgr = getEditorManager();
                IEditor editor = editorMgr.getEditor(unpubModel);
                if (editor == null) {
                    editor = editorMgr.openPreview(unpubModel);
                }
                IRenderService renderer = getEditorRenderer(editor);
                if (renderer != null) {
                    renderer.focus(null);
                }

                editor = getEditor();
                editor.close();
                return null;
            }
        });

        add(new StdWorkflow("select", new StringResourceModel("select", this, null).getString(), null, context, this) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "select-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
                return new HistoryDialog(wdm, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                // TODO
                return null;
            }
        });
    }

    private IEditor getEditor() {
        return getPluginContext().getService(getPluginConfig().getString("editor.id"), IEditor.class);
    }

    private IEditorManager getEditorManager() {
        return getPluginContext().getService(getPluginConfig().getString("editor.id"), IEditorManager.class);
    }

    private IRenderService getEditorRenderer(IEditor editor) {
        IPluginContext context = getPluginContext();
        return getPluginContext().getService(context.getReference(editor).getServiceId(), IRenderService.class);
    }
    
}
