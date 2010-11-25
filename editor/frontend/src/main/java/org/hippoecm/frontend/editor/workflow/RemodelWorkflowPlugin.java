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
package org.hippoecm.frontend.editor.workflow;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.editor.cnd.CndSerializer;
import org.hippoecm.editor.repository.NamespaceWorkflow;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.editor.layout.ILayoutProvider;
import org.hippoecm.frontend.editor.workflow.action.NewCompoundTypeAction;
import org.hippoecm.frontend.editor.workflow.action.NewDocumentTypeAction;
import org.hippoecm.frontend.editor.workflow.dialog.RemodelDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.standardworkflow.Change;
import org.hippoecm.repository.standardworkflow.ChangeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemodelWorkflowPlugin extends CompatibilityWorkflowPlugin<NamespaceWorkflow> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(RemodelWorkflowPlugin.class);

    private static final long serialVersionUID = 1L;

    public RemodelWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final ILayoutProvider layouts = context.getService(ILayoutProvider.class.getName(), ILayoutProvider.class);
        add(new NewDocumentTypeAction(this, "new-document-type", new StringResourceModel("new-document-type", this, null), layouts));
        add(new NewCompoundTypeAction(this, layouts));

        add(new WorkflowAction("remodel", new StringResourceModel("update-model", this, null)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Dialog createRequestDialog() {
                return new RemodelDialog(this, (WorkflowDescriptorModel) RemodelWorkflowPlugin.this.getDefaultModel());
            }

            @Override
            protected String execute(NamespaceWorkflow workflow) throws Exception {
                try {
                    javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
                    Map<String, Serializable> hints = workflow.hints();
                    String prefix = (String) hints.get("prefix");
                    log.info("remodelling namespace " + prefix);
                    String cnd = new CndSerializer(session, prefix).getOutput();
                    log.debug("new cnd:\n" + cnd);
                    Map<String,List<Change>> cargo = makeCargo(session, prefix);
                    session.save();
                    session.refresh(false);


                    // log out; the session model will log in again after the updateModel workflow call
                    // Sessions cache path resolver information, which is incorrect after remapping the prefix.
                    ((UserSession) Session.get()).releaseJcrSession();
                    workflow.updateModel(cnd, cargo);

                    return null;
                } catch (Exception ex) {
                    log.error("Failed updateModel workflow", ex);
                    return ex.getClass().getName() + ": " + ex.getMessage();
                }
            }

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(StdWorkflow.class, "update-all-16.png");
            }
        });
    }

    public IEditorManager getEditorManager() {
        return getPluginContext().getService(getPluginConfig().getString("editor.id", IEditorManager.class.getName()), IEditorManager.class);
    }
    
    public static Map<String, List<Change>> makeCargo(javax.jcr.Session session, String prefix) throws RepositoryException {
        Map<String, List<Change>> changes = new TreeMap<String, List<Change>>();
        String uri = null;
        try {
            uri = session.getNamespaceURI(prefix);
        } catch (NamespaceException ex) {
            // namespace is new, ignore
        }
        Node templateNamespace = session.getRootNode().getNode(HippoNodeType.NAMESPACES_PATH).getNode(prefix);
        for (NodeIterator nodeTypeIter = templateNamespace.getNodes(); nodeTypeIter.hasNext();) {
            Node nodeTypeNode = nodeTypeIter.nextNode();
            List<Change> changeList = new LinkedList<Change>();
            log.info("remodelling rules nodetype " + prefix + ":" + nodeTypeNode.getName());
            changes.put(prefix + ":" + nodeTypeNode.getName(), changeList);

            Node draftNodeType = null;
            Node currentNodeType = null;
            String prototype = null;
            if (nodeTypeNode.hasNode(HippoNodeType.HIPPO_PROTOTYPES)) {
                for(NodeIterator iter = nodeTypeNode.getNode(HippoNodeType.HIPPO_PROTOTYPES).getNodes(HippoNodeType.HIPPO_PROTOTYPE); iter.hasNext(); ) {
                    Node candidate = iter.nextNode();
                    prototype = candidate.getPath();
                    if(candidate.isNodeType("nt:unstructured")) {
                        break;
                    }
                }
            }
            for (NodeIterator variantIter = nodeTypeNode.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE).getNodes(HippoNodeType.HIPPOSYSEDIT_NODETYPE); variantIter.hasNext();) {
                Node nodeTypeVariantNode = variantIter.nextNode();
                if (nodeTypeVariantNode.isNodeType(HippoNodeType.NT_REMODEL)) {
                    if (nodeTypeVariantNode.getProperty(HippoNodeType.HIPPO_URI).getString().equals(uri)) {
                        currentNodeType = nodeTypeVariantNode;
                    }
                } else {
                    draftNodeType = nodeTypeVariantNode;
                }
            }
            Map<String, Node> draftDefinition = new TreeMap<String, Node>();
            if (draftNodeType != null) {
                for (NodeIterator iter = draftNodeType.getNodes(); iter.hasNext();) {
                    Node field = iter.nextNode();
                    if (!field.isNodeType(HippoNodeType.NT_FIELD)) {
                        continue;
                    }
                    String name;
                    if (field.hasProperty(HippoNodeType.HIPPO_NAME)) {
                        name = field.getProperty(HippoNodeType.HIPPO_NAME).getString();
                    } else {
                        name = field.getName();
                    }
                    draftDefinition.put(name, field);
                }
            }
            Map<String, Node> currentDefinition = new TreeMap<String, Node>();
            if (currentNodeType != null) {
                for (NodeIterator iter = currentNodeType.getNodes(); iter.hasNext();) {
                    Node field = iter.nextNode();
                    if (!field.isNodeType(HippoNodeType.NT_FIELD)) {
                        continue;
                    }
                    String name;
                    if (field.hasProperty(HippoNodeType.HIPPO_NAME)) {
                        name = field.getProperty(HippoNodeType.HIPPO_NAME).getString();
                    } else {
                        name = field.getName();
                    }
                    currentDefinition.put(name, field);
                }
            }
            if (draftNodeType != null) {
                for (Map.Entry<String, Node> def : draftDefinition.entrySet()) {
                    Node draftItem = def.getValue();
                    Node currentItem = currentDefinition.get(def.getKey());
                    if (currentItem == null) {
                        // field is new in draft defintion, but only add it if mandatory
                        if (draftItem.hasProperty(HippoNodeType.HIPPO_MANDATORY) && draftItem.getProperty(HippoNodeType.HIPPO_MANDATORY).getBoolean()) {
                            String newPath = draftItem.getProperty(HippoNodeType.HIPPO_PATH).getString();
                            changeList.add(new Change(ChangeType.ADDITION, draftItem.getProperty(HippoNodeType.HIPPO_PATH).getString(), prototype+"/"+newPath));
                            log.info("remodelling rule add " + draftItem.getProperty(HippoNodeType.HIPPO_PATH).getString());
                        }
                    } else {
                        String newPath = draftItem.getProperty(HippoNodeType.HIPPO_PATH).getString();
                        String oldPath = currentItem.getProperty(HippoNodeType.HIPPO_PATH).getString();
                        if (!newPath.equals(oldPath)) {
                            changeList.add(new Change(ChangeType.RENAMED, oldPath, newPath));
                            log.info("remodelling rule renamed " + oldPath +" to " + newPath);
                        }
                        if (draftItem.hasProperty(HippoNodeType.HIPPO_MANDATORY) && draftItem.getProperty(HippoNodeType.HIPPO_MANDATORY).getBoolean() &&
                            !(currentItem.hasProperty(HippoNodeType.HIPPO_MANDATORY) && currentItem.getProperty(HippoNodeType.HIPPO_MANDATORY).getBoolean())) {
                            // field existed, but was not mandatory in current definition
                            log.info("remodelling rule add " + newPath);
                            changeList.add(new Change(ChangeType.ADDITION, newPath, prototype+"/"+newPath));
                        }
                    }
                }
                for (Map.Entry<String, Node> def : currentDefinition.entrySet()) {
                    Node draftItem = draftDefinition.get(def.getKey());
                    if (draftItem == null) {
                        // field has been removed in draft definition
                        log.info("remodelling rule drop " + def.getValue().getProperty(HippoNodeType.HIPPO_PATH).getString());
                        changeList.add(new Change(ChangeType.DROPPED, def.getValue().getProperty(HippoNodeType.HIPPO_PATH).getString(), null));
                    }
                }
            }
        }

        return changes;
    }
}
