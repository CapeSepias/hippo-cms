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
package org.hippoecm.frontend.plugins.standards.tabs;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.CancelEventIfNoAjaxDecorator;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.markup.html.list.Loop.LoopItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.behaviors.IContextMenuManager;
import org.hippoecm.frontend.plugins.yui.rightclick.RightClickBehavior;
import org.hippoecm.frontend.service.IconSize;

public class TabbedPanel extends WebMarkupContainer {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public static final String TAB_PANEL_ID = "panel";

    private final TabsPlugin plugin;

    private int maxTabLength = 12;
    private final List<TabsPlugin.Tab> tabs;
    private MarkupContainer panelContainer;
    private MarkupContainer tabsContainer;
    private IconSize iconType = IconSize.TINY;
    private transient boolean redraw = false;

    public TabbedPanel(String id, TabsPlugin plugin, List<TabsPlugin.Tab> tabs, MarkupContainer tabsContainer) {
        super(id, new Model<Integer>(-1));

        if (tabs == null) {
            throw new IllegalArgumentException("argument [tabs] cannot be null");
        }

        this.plugin = plugin;
        this.tabs = tabs;
        this.tabsContainer = tabsContainer;

        setOutputMarkupId(true);

        final IModel<Integer> tabCount = new AbstractReadOnlyModel<Integer>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Integer getObject() {
                return TabbedPanel.this.tabs.size();
            }
        };

        // add the loop used to generate tab names
        tabsContainer.add(new Loop("tabs", tabCount) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(LoopItem item) {
                final int index = item.getIteration();

                final WebMarkupContainer titleMarkupContainer = getTitleMarkupContainer(index);
                item.add(titleMarkupContainer);
                item.add(newBehavior(index));

                final WebMarkupContainer menu = createContextMenu("contextMenu", index);

                item.add(menu);
                item.add(new RightClickBehavior(menu, item) {

                    @Override
                    protected void respond(AjaxRequestTarget target) {
                        getContextmenu().setVisible(true);
                        target.addComponent(getComponentToUpdate());
                        IContextMenuManager menuManager = (IContextMenuManager) findParent(IContextMenuManager.class);
                        if (menuManager != null) {
                            menuManager.showContextMenu(this);
                            String x = RequestCycle.get().getRequest().getParameter(MOUSE_X_PARAM);
                            String y = RequestCycle.get().getRequest().getParameter(MOUSE_Y_PARAM);
                            target.appendJavascript("Hippo.ContextMenu.renderAtPosition('"
                                    + menu.getMarkupId() + "', " + x + ", " + y + ");");
                        }
                    }
                });
                item.setOutputMarkupId(true);
            }

            @Override
            protected LoopItem newItem(int iteration) {
                return newTabContainer(iteration);
            }

        });

        panelContainer = new WebMarkupContainer("panel-container");
        panelContainer.setOutputMarkupId(true);
        panelContainer.add(plugin.getEmptyPanel());
        add(panelContainer);
    }

    private WebMarkupContainer createContextMenu(String contextMenu, final int index) {
        final TabsPlugin.Tab tab = getTabs().get(index);
        WebMarkupContainer menuContainer = new WebMarkupContainer(contextMenu);
        menuContainer.setOutputMarkupId(true);
        menuContainer.setVisible(false);
        AjaxLink closeLink = new AjaxLink("editor-close") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                plugin.onClose(tab, target);
            }
        };


        menuContainer.add(closeLink);

        AjaxLink closeOthersLink = new AjaxLink("editor-close-others") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //Create a copy so we won't run into ConcurrentModificationException
                List<TabsPlugin.Tab> tabsCopy = new ArrayList<TabsPlugin.Tab>(tabs);
                for (TabsPlugin.Tab currentTab : tabsCopy) {
                    if (!currentTab.equals(tab)) {
                        plugin.onClose(currentTab, target);
                    }
                }
            }
        };

        menuContainer.add(closeOthersLink);

        AjaxLink closeAllLink = new AjaxLink("editor-close-all") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                 plugin.closeAll(target);
            }
        };


        menuContainer.add(closeAllLink);

        AjaxLink closeUnmodifiedLink = new AjaxLink("editor-close-unmodified") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //Create a copy so we won't run into ConcurrentModificationException
                List<TabsPlugin.Tab> tabsCopy = new ArrayList<TabsPlugin.Tab>(tabs);
                for (TabsPlugin.Tab currentTab : tabsCopy) {
                    plugin.onClose(currentTab, target, true);
                }
            }
        };

        menuContainer.add(closeUnmodifiedLink);

        return menuContainer;
    }


    protected LoopItem newTabContainer(final int tabIndex) {
        return new LoopItem(tabIndex) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                String cssClass = (String) tag.getString("class");
                if (cssClass == null) {
                    cssClass = " ";
                }
                cssClass += " tab" + getIteration();

                if (getIteration() == getSelectedTab()) {
                    cssClass += " selected";
                }
                if (getIteration() == getTabs().size() - 1) {
                    cssClass += " last";
                }
                tag.put("class", cssClass.trim());
            }
        };
    }

    // used by superclass to add title to the container
    protected WebMarkupContainer getTitleMarkupContainer(final int index) {
        WebMarkupContainer container = new WebMarkupContainer("container", new Model<Integer>(index));
        TabsPlugin.Tab tab = getTabs().get(index);
        final IModel<TabsPlugin.Tab> tabModel = new Model<TabsPlugin.Tab>(tab);
        if (tab.canClose()) {
            container.add(new AjaxFallbackLink<TabsPlugin.Tab>("close", tabModel) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    plugin.onClose(getModelObject(), target);
                }
            });
        } else {
            container.add(new Label("close").setVisible(false));
        }
        WebMarkupContainer link = new AjaxFallbackLink<TabsPlugin.Tab>("link", tabModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                plugin.onSelect(getModelObject(), target);
            }
        };

        ResourceReference iconResource = tab.getIcon(iconType);
        Image image;
        if (iconResource == null) {
            image = new Image("icon");
            image.setVisible(false);
        } else {
            image = new Image("icon", iconResource);
        }
        IModel<String> sizeModel = new Model<String>(Integer.valueOf(iconType.getSize()).toString());
        image.add(new AttributeModifier("width", true, sizeModel));
        image.add(new AttributeModifier("height", true, sizeModel));
        link.add(image);

        link.add(new Label("title", new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                IModel<String> titleModel = tabModel.getObject().getTitle();
                if (titleModel != null) {
                    String title = titleModel.getObject();
                    if (title.length() > maxTabLength) {
                        // leave space for two .. then add them
                        title = title.substring(0, maxTabLength - 2) + "..";
                    }
                    return title;
                }
                return "title";
            }
        }));
        link.add(new AttributeAppender("title", new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                IModel<String> titleModel = tabModel.getObject().getTitle();
                if (titleModel != null) {
                    return titleModel.getObject();
                }
                return "";
            }

        }, ""));
        container.add(link);

        return container;
    }

    protected IBehavior newBehavior(final int tabIndex) {
        return new AjaxEventBehavior("onclick") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                plugin.onSelect(tabs.get(tabIndex), target);
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new CancelEventIfNoAjaxDecorator(null);
            }
        };
    }

    public void setMaxTitleLength(int maxTitleLength) {
        this.maxTabLength = maxTitleLength;
    }

    @Override
    public boolean isTransparentResolver() {
        return true;
    }

    public void redraw() {
        redraw = true;
    }

    public void render(PluginRequestTarget target) {
        if (redraw) {
            if (target != null) {
                target.addComponent(tabsContainer);
                target.addComponent(get("panel-container"));
            }
            redraw = false;
        }
    }

    // @see org.apache.wicket.Component#onAttach()
    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        if (!hasBeenRendered() && getSelectedTab() == -1) {
            // select the first tab by default
            setSelectedTab(0);
        }
    }

    public final List<TabsPlugin.Tab> getTabs() {
        return tabs;
    }

    public void setSelectedTab(int index) {
        if (index < 0 || index >= tabs.size()) {
            panelContainer.replace(plugin.getEmptyPanel());
            return;
        }

        setDefaultModelObject(index);

        ITab tab = tabs.get(index);

        Panel panel = tab.getPanel(TAB_PANEL_ID);

        if (panel == null) {
            throw new WicketRuntimeException("ITab.getPanel() returned null. TabbedPanel [" + getPath()
                    + "] ITab index [" + index + "]");

        }

        if (!panel.getId().equals(TAB_PANEL_ID)) {
            throw new WicketRuntimeException(
                    "ITab.getPanel() returned a panel with invalid id ["
                            + panel.getId()
                            + "]. You must always return a panel with id equal to the provided panelId parameter. TabbedPanel ["
                            + getPath() + "] ITab index [" + index + "]");
        }

        panelContainer.replace(panel);
    }

    public final int getSelectedTab() {
        return (Integer) getDefaultModelObject();
    }

    public void setIconType(IconSize iconType) {
        this.iconType = iconType;
    }

    public IconSize getIconType() {
        return iconType;
    }

}
