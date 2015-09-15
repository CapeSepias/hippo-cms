/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.browse.tree;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.tree.ITreeState;
import org.apache.wicket.model.IModel;
import org.hippoecm.addon.workflow.ContextWorkflowPlugin;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.behaviors.IContextMenuManager;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.tree.CmsJcrTree.ITreeNodeTranslator;
import org.hippoecm.frontend.plugins.cms.browse.tree.CmsJcrTree.TreeNodeTranslator;
import org.hippoecm.frontend.plugins.cms.browse.tree.yui.WicketTreeHelperBehavior;
import org.hippoecm.frontend.plugins.cms.browse.tree.yui.WicketTreeHelperSettings;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.hippoecm.frontend.plugins.yui.scrollbehavior.ScrollBehavior;
import org.hippoecm.frontend.plugins.standards.tree.FolderTreeNode;
import org.hippoecm.frontend.plugins.standards.tree.icon.DefaultTreeNodeIconProvider;
import org.hippoecm.frontend.plugins.standards.tree.icon.ITreeNodeIconProvider;
import org.hippoecm.frontend.plugins.yui.rightclick.RightClickBehavior;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.util.MaxLengthNodeNameFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderTreePlugin extends RenderPlugin {
    static final Logger log = LoggerFactory.getLogger(FolderTreePlugin.class);

    protected final CmsJcrTree tree;
    protected JcrTreeModel treeModel;
    protected JcrTreeNode rootNode;
    private JcrNodeModel rootModel;

    private static final String DEFAULT_START_PATH = "/content";

    private WicketTreeHelperBehavior treeHelperBehavior;

    public FolderTreePlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
        
        String startingPath = config.getString("path", DEFAULT_START_PATH);
        boolean canAccessPath = true;
        try {
            final Session session = getSession().getJcrSession();
            if (!session.hasPermission(startingPath, Session.ACTION_READ)) {
                log.warn("User '{} is unauthorized to read at the configured path '{}'", session.getUserID(), startingPath);
                canAccessPath = false;
            } else if (!session.itemExists(startingPath)) {
                log.warn("The configured path '{}' does not exist. Please check the configuration", startingPath);
                canAccessPath =  false;
            }
        } catch (RepositoryException exception) {
            canAccessPath = false;
            log.debug("Path '{}' is invalid", startingPath);
        }

        if (!canAccessPath) {
            tree = null;
            add(new Label("tree", StringUtils.EMPTY));
            return;
        }

        add(tree = initializeTree(context, config, startingPath));
        onModelChanged();
        add(new ScrollBehavior());
    }

    private CmsJcrTree initializeTree(final IPluginContext context, final IPluginConfig config, final String startingPath) {
        rootModel = new JcrNodeModel(startingPath);

        DocumentListFilter folderTreeConfig = new DocumentListFilter(config);
        this.rootNode = new FolderTreeNode(rootModel, folderTreeConfig);
        treeModel = new JcrTreeModel(rootNode);
        context.registerService(treeModel, IObserver.class.getName());
        final CmsJcrTree cmsJcrTree = new CmsJcrTree("tree", treeModel, newTreeNodeTranslator(config), newTreeNodeIconProvider()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected MarkupContainer newContextContent(MarkupContainer parent, String id, final TreeNode node) {
                IPluginConfig workflowConfig = config.getPluginConfig("module.workflow");
                if (workflowConfig != null && (node instanceof IJcrTreeNode)) {
                    ContextWorkflowPlugin content = new ContextWorkflowPlugin(context, workflowConfig);
                    content.bind(FolderTreePlugin.this, id);
                    IModel<Node> nodeModel = ((IJcrTreeNode) node).getNodeModel();
                    content.setModel(nodeModel);
                    return content;
                }
                return new EmptyPanel(id);
            }

            @Override
            protected MarkupContainer newContextLink(final MarkupContainer parent, String id, final TreeNode node,
                                                     final MarkupContainer content) {

                final boolean workflowEnabled = getPluginConfig().getAsBoolean("workflow.enabled", true);
                parent.add(new AbstractBehavior() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void renderHead(IHeaderResponse response) {
                        response.renderOnDomReadyJavascript(treeHelperBehavior.getRenderString());
                        if (workflowEnabled) {
                            response.renderOnDomReadyJavascript(treeHelperBehavior.getUpdateString());
                        }
                    }
                });

                if (getPluginConfig().getBoolean("contextmenu.rightclick.enabled")) {
                    parent.add(new RightClickBehavior(content, parent) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void respond(AjaxRequestTarget target) {
                            updateTree(target);
                            getContextmenu().setVisible(true);
                            target.addComponent(getComponentToUpdate());
                            IContextMenuManager menuManager = findParent(IContextMenuManager.class);
                            if (menuManager != null) {
                                menuManager.showContextMenu(this);
                                String x = RequestCycle.get().getRequest().getParameter(MOUSE_X_PARAM);
                                String y = RequestCycle.get().getRequest().getParameter(MOUSE_Y_PARAM);
                                if (x != null && y != null) {
                                    target.appendJavascript("Hippo.ContextMenu.renderAtPosition('"
                                            + content.getMarkupId() + "', " + x + ", " + y + ");");
                                } else {
                                    target.appendJavascript("Hippo.ContextMenu.renderInTree('" + content.getMarkupId()
                                            + "');");
                                }
                            }
                        }
                    });

                }
                MarkupContainer container = super.newContextLink(parent, id, node, content);
                if (!workflowEnabled) {
                    container.setEnabled(false);
                }
                return container;
            }

            @Override
            protected void onContextLinkClicked(MarkupContainer content, AjaxRequestTarget target) {
                target.appendJavascript("Hippo.ContextMenu.renderInTree('" + content.getMarkupId() + "');");
            }

            @Override
            protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode) {
                if (clickedNode instanceof IJcrTreeNode) {
                    IJcrTreeNode treeNodeModel = (IJcrTreeNode) clickedNode;
                    FolderTreePlugin.this.setDefaultModel(treeNodeModel.getNodeModel());
                    ITreeState state = getTreeState();
                    if (state.isNodeExpanded(clickedNode)) {
                        // super has already switched selection.
                        if (!state.isNodeSelected(clickedNode)) {
                            state.collapseNode(clickedNode);
                        }
                    } else {
                        state.expandNode(clickedNode);
                    }
                }
                updateTree(target);
            }

            @Override
            protected void onJunctionLinkClicked(AjaxRequestTarget target, TreeNode node) {
                updateTree(target);
            }
        };


        cmsJcrTree.add(treeHelperBehavior = new WicketTreeHelperBehavior(new WicketTreeHelperSettings(config)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getWicketId() {
                return tree.getMarkupId();
            }

        });

        cmsJcrTree.setRootLess(config.getBoolean("rootless"));
        return cmsJcrTree;
    }

    protected ITreeNodeTranslator newTreeNodeTranslator(IPluginConfig config) {
        return new TreeNodeTranslator();
    }

    protected ITreeNodeIconProvider newTreeNodeIconProvider() {
        IPluginContext context = getPluginContext();
        IPluginConfig config = getPluginConfig();

        final List<ITreeNodeIconProvider> providers = new LinkedList<ITreeNodeIconProvider>();
        providers.add(new DefaultTreeNodeIconProvider());
        providers.addAll(context.getServices(ITreeNodeIconProvider.class.getName(), ITreeNodeIconProvider.class));
        if (config.containsKey("tree.icon.id")) {
            providers.addAll(context.getServices(config.getString("tree.icon.id"), ITreeNodeIconProvider.class));
        }
        Collections.reverse(providers);

        return new ITreeNodeIconProvider() {
            private static final long serialVersionUID = 1L;

            public ResourceReference getNodeIcon(TreeNode treeNode, ITreeState state) {
                for (ITreeNodeIconProvider provider : providers) {
                    ResourceReference icon = provider.getNodeIcon(treeNode, state);
                    if (icon != null) {
                        return icon;
                    }
                }
                throw new RuntimeException("No icon could be found for tree node");
            }

        };
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        if (tree != null) {
            tree.updateTree();
        }
    }

    @Override
    public void onBeforeRender() {
        if (tree != null) {
            tree.detach();
        }
        super.onBeforeRender();
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();

        if (tree == null) {
            return;
        }
        JcrNodeModel model = (JcrNodeModel) getDefaultModel();
        ITreeState treeState = tree.getTreeState();
        TreePath treePath = treeModel.lookup(model);
        if (treePath != null) {
            for (Object component : treePath.getPath()) {
                TreeNode treeNode = (TreeNode) component;
                if (!treeState.isNodeExpanded(treeNode)) {
                    treeState.expandNode(treeNode);
                }
            }
            treeState.selectNode((TreeNode) treePath.getLastPathComponent(), true);
        }
    }

    public class FormattedTreeNodeTranslator extends MaxLengthNodeNameFormatter implements ITreeNodeTranslator {
        private static final long serialVersionUID = 1L;

        public FormattedTreeNodeTranslator(IPluginConfig config) {
            super(config.getInt("nodename.max.length", -1), config.getString("nodename.splitter", ".."), config.getInt(
                    "nodename.indent.length", 3));
        }

        public String getTitleName(TreeNode treeNode) {
            return getName(((IJcrTreeNode) treeNode).getNodeModel());
        }

        public String getName(TreeNode treeNode, int indent) {
            return parse(getTitleName(treeNode), indent);
        }

        public boolean hasTitle(TreeNode treeNode, int level) {
            return isTooLong(getTitleName(treeNode), level);
        }
    }

}
