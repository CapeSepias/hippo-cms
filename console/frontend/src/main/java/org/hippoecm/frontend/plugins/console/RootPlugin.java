/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.js.GlobalJsResourceBehavior;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.console.behavior.PathHistoryBehavior;
import org.hippoecm.frontend.plugins.yui.layout.PageLayoutBehavior;
import org.hippoecm.frontend.plugins.yui.layout.PageLayoutSettings;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppBehavior;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppSettings;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.util.MappingException;
import org.hippoecm.frontend.util.PluginConfigMapper;
import org.hippoecm.frontend.widgets.Pinger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RootPlugin extends RenderPlugin {

    static final Logger log = LoggerFactory.getLogger(RootPlugin.class);

    private static final long serialVersionUID = 1L;

    private boolean rendered = false;
    private PathHistoryBehavior pathHistoryBehavior;

    public RootPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new GlobalJsResourceBehavior());

        if (config.containsKey("pinger.interval")) {
            add(new Pinger("pinger", config.getAsDuration("pinger.interval")));
        } else {
            add(new Pinger("pinger"));
        }

        if (config.getString(RenderService.MODEL_ID) != null) {
            String modelId = config.getString(RenderService.MODEL_ID);
            ModelReference modelService = new ModelReference(modelId, new JcrNodeModel("/"));
            modelService.init(context);

            pathHistoryBehavior = new PathHistoryBehavior(null, modelService);
            context.registerService(pathHistoryBehavior, IObserver.class.getName());
            add(pathHistoryBehavior);
        }

        PageLayoutSettings plSettings = new PageLayoutSettings();
        try {
            PluginConfigMapper.populate(plSettings, config.getPluginConfig("yui.config"));
        } catch (MappingException e) {
            throw new RuntimeException(e);
        }
        add(new PageLayoutBehavior(plSettings));

        add(new Label("pageTitle", getPageTitle(config)));

        final String faviconPath = config.getString("favicon.path", "console-red.ico");
        add(new ResourceLink("faviconLink", new ResourceReference(RootPlugin.class, faviconPath)));
    }

    @Override
    public void render(PluginRequestTarget target) {
        if (!rendered) {
            WebAppSettings settings = new WebAppSettings();
            getPage().add(new WebAppBehavior(settings));
            rendered = true;
        }
        super.render(target);
    }

    private String getPageTitle(IPluginConfig config) {
        StringBuilder pageTitle = new StringBuilder(config.getString("page.title", "Hippo CMS Console"));
        if(config.getAsBoolean("page.title.showservername", false)) {
            pageTitle.append(config.getString("page.title.separator", "@"));
            pageTitle.append(getWebRequest().getHttpServletRequest().getServerName());
        }
        return pageTitle.toString();
    }

}

