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
package org.hippoecm.frontend.plugins.standards.perspective;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResource;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.render.RenderPlugin;

public abstract class Perspective extends RenderPlugin<Void> implements ITitleDecorator {

    private static final long serialVersionUID = 1L;

    public static final String TITLE = "perspective.title";

    public static final String CLUSTER_NAME = "cluster.name";
    public static final String CLUSTER_PARAMETERS = "cluster.config";

    public static final String ACTIVE_SUFFIX = "-active";
    public static final String IMAGE_EXTENSION = "svg";
    public static final String FALLBACK_IMAGE_EXTENSION = "png";

    private IModel<String> title = new Model<String>("title");

    private boolean rendered;

    private String activeSuffix;
    private String imageExtension;
    private String fallbackImageExtension;

    public Perspective(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config.getString(TITLE) != null) {
            title = new StringResourceModel(config.getString(TITLE), this, null);
        }

        activeSuffix = config.getString("active.suffix", ACTIVE_SUFFIX);
        imageExtension = config.getString("image.extension", IMAGE_EXTENSION);
        fallbackImageExtension = config.getString("fallback.image.extension", FALLBACK_IMAGE_EXTENSION);
    }

    public String getTitleCssClass() {
        // return a stable CSS class name to be able to identify a perspective's link in automated tests
        return "hippo-perspective-" + getClass().getSimpleName().toLowerCase();
    }

    // ITitleDecorator

    @Override
    public IModel<String> getTitle() {
        return title;
    }

    @Override
    public void onComponentTag(final ComponentTag tag) {
        super.onComponentTag(tag);
        tag.append("class", "perspective", " ");
    }

    @Override
    public ResourceReference getIcon(IconSize size) {
        // try (name)-(size).svg
        String image = toImageName(getClass().getSimpleName(), size, imageExtension);
        if (PackageResource.exists(getClass(), image, null, null, null)) {
            return new PackageResourceReference(getClass(), image);
        }

        // try (name).svg
        image = toImageName(getClass().getSimpleName(), null, imageExtension);
        if (PackageResource.exists(getClass(), image, null, null, null)) {
            return new PackageResourceReference(getClass(), image);
        }

        // try (name)-(size).png
        image = toImageName(getClass().getSimpleName(), size, fallbackImageExtension);
        if (PackageResource.exists(getClass(), image, null, null, null)) {
            return new PackageResourceReference(getClass(), image);
        }

        // use built-in picture
        image = toImageName(Perspective.class.getSimpleName(), size, fallbackImageExtension);
        if (PackageResource.exists(Perspective.class, image, null, null, null)) {
            return new PackageResourceReference(Perspective.class, image);
        }

        return null;
    }

    @Override
    public ResourceReference getActiveIcon(IconSize size) {
        // try (name)-active-(size).svg
        String image = toImageName(getClass().getSimpleName() + activeSuffix, size, imageExtension);
        if (PackageResource.exists(getClass(), image, null, null, null)) {
            return new PackageResourceReference(getClass(), image);
        }

        // try (name)-active.svg
        image = toImageName(getClass().getSimpleName() + activeSuffix, null, imageExtension);
        if (PackageResource.exists(getClass(), image, null, null, null)) {
            return new PackageResourceReference(getClass(), image);
        }

        // try (name)-active-(size).png
        image = toImageName(getClass().getSimpleName() + activeSuffix, size, fallbackImageExtension);
        if (PackageResource.exists(getClass(), image, null, null, null)) {
            return new PackageResourceReference(getClass(), image);
        }

        // use built-in active picture
        image = toImageName(Perspective.class.getSimpleName() + activeSuffix, size, fallbackImageExtension);
        if (PackageResource.exists(Perspective.class, image, null, null, null)) {
            return new PackageResourceReference(Perspective.class, image);
        }

        return null;
    }

    protected String toImageName(final String camelCaseString, final IconSize size, final String extension) {
        StringBuilder name = new StringBuilder(camelCaseString.length());
        name.append(Character.toLowerCase(camelCaseString.charAt(0)));
        for (int i = 1; i < camelCaseString.length(); i++) {
            char c = camelCaseString.charAt(i);
            if (Character.isUpperCase(c)) {
                name.append('-').append(Character.toLowerCase(c));
            } else {
                name.append(c);
            }
        }
        if (size != null) {
            name.append('-').append(size.getSize());    
        }
        name.append('.').append(extension);

        return name.toString();
    }

    protected void setTitle(String title) {
        this.title.setObject(title);
    }

    @Override
    public void render(PluginRequestTarget target) {
        if (!rendered && isActive()) {
            rendered = true;

            IPluginConfig config = getPluginConfig();
            String clusterName = config.getString(CLUSTER_NAME);
            if (clusterName != null) {
                IPluginContext context = getPluginContext();
                IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                        IPluginConfigService.class);

                IClusterConfig cluster = pluginConfigService.getCluster(clusterName);
                if (cluster == null) {
                    log.warn("Unable to find cluster '" + clusterName + "'. Does it exist in repository?");
                } else {
                    IPluginConfig parameters = config.getPluginConfig(CLUSTER_PARAMETERS);
                    IClusterControl control = context.newCluster(cluster, parameters);
                    control.start();
                }
            }
        }
        super.render(target);
    }
}
