/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.ckeditor;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.richtext.IHtmlCleanerService;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for CKEditor field plugins, which use CKEditor for editing fields that contain HTML.
 * Configuration properties:
 * <ul>
 *     <li>ckeditor.config.json: String property with a JSON object that specifies the configuration of
 *     the CKEditor instance created for the edited field. Will be ignored when empty or missing.</li>
 *     <li>htmlcleaner.id: String property with the ID of the HTML cleaner service to use. Use an empty string
 *     to disable the HTML cleaner. Default value: "org.hippoecm.frontend.plugins.richtext.IHtmlCleanerService".</li>
 * </ul>
 */
public abstract class AbstractCKEditorPlugin extends RenderPlugin {

    public static final String CONFIG_CKEDITOR_CONFIG_JSON = "ckeditor.config.json";
    public static final String CONFIG_HTML_CLEANER_SERVICE_ID = "htmlcleaner.id";
    public static final String CONFIG_MODEL_COMPARE_TO = "model.compareTo";

    private static final String WICKET_ID_PANEL = "panel";

    private static final Logger log = LoggerFactory.getLogger(AbstractCKEditorPlugin.class);

    public AbstractCKEditorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        IEditor.Mode mode = getMode();
        Panel panel = createPanel(mode);
        add(panel);
    }

    private IEditor.Mode getMode() {
        final String modeName = getPluginConfig().getString("mode", IEditor.Mode.VIEW.name().toLowerCase());
        return IEditor.Mode.fromString(modeName);
    }

    private Panel createPanel(final IEditor.Mode mode) {
        switch (mode) {
            case VIEW:
                return createViewPanel(WICKET_ID_PANEL);
            case EDIT:
                return createEditPanel(WICKET_ID_PANEL);
            case COMPARE:
                return createComparePanel(WICKET_ID_PANEL);
            default:
                throw new IllegalStateException("Unsupported editor mode: " + mode);
        }
    }

    /**
     * Creates the panel to show in 'view' mode. It typically shows a preview of the HTML contained in the model.
     *
     * @param id the Wicket ID of the panel
     * @return the panel to show in 'view' mode.
     */
    protected abstract Panel createViewPanel(final String id);

    private Panel createEditPanel(final String id) {
        final String editorConfigJson = readAndValidateEditorConfig();
        return createEditPanel(id, editorConfigJson);
    }

    private String readAndValidateEditorConfig() {
        String jsonOrNull = getPluginConfig().getString(CONFIG_CKEDITOR_CONFIG_JSON);
        try {
            // validate JSON and return the sanitized version. This also strips additional extra JSON literals from the end.
            return JsonUtils.createJSONObject(jsonOrNull).toString();
        } catch (JSONException e) {
            log.warn("Ignoring CKEditor configuration variable '{}' because does not contain valid JSON, but \"{}\"",
                    CONFIG_CKEDITOR_CONFIG_JSON, jsonOrNull);
        }
        return StringUtils.EMPTY;
    }

    /**
     * Create the panel to show in 'edit' mode, which should render a CKEditor instance to edit the model.
     * @param id the Wicket ID of the panel
     * @param editorConfigJson the JSON configuration of the CKEditor instance to create.
     * @return the panel that displays a CKEditor instance.
     */
    protected abstract Panel createEditPanel(final String id, final String editorConfigJson);

    private Panel createComparePanel(final String id) {
        final IModel baseModel = getBaseModelOrNull();
        if (baseModel == null) {
            log.warn("Plugin '{}' cannot instantiate compare mode, using regular HTML preview instead.",
                    getPluginConfig().getName());
            return createViewPanel(id);
        }

        final IModel currentModel = getDefaultModel();

        return createComparePanel(id, baseModel, currentModel);
    };

    /**
     * Creates the panel the show in 'compare' mode. It typically displays the differences between the provided
     * base model and current model.
     * @param id the Wicket ID of the panel
     * @param baseModel the model with the old HTML
     * @param currentModel the model with the new HTML
     * @return a panel that compares the current model with the base model.
     */
    protected abstract Panel createComparePanel(final String id, final IModel baseModel, final IModel currentModel);

    private IModel getBaseModelOrNull() {
        final IPluginConfig config = getPluginConfig();
        if (!config.containsKey(CONFIG_MODEL_COMPARE_TO)) {
            log.warn("Plugin {} is missing configuration property '{}'", config.getName(), CONFIG_MODEL_COMPARE_TO);
            return null;
        }

        final String compareToServiceId = config.getString(CONFIG_MODEL_COMPARE_TO);
        IModel model = getModelFromServiceOrNull(compareToServiceId);
        if (model == null) {
            log.warn("Plugin {} cannot get the node model from service '{}'. Check the config property '{}'",
                new Object[]{config.getName(), compareToServiceId, CONFIG_MODEL_COMPARE_TO});
        }
        return model;
    }

    private IModel getModelFromServiceOrNull(final String serviceName) {
        final IModelReference modelRef = getPluginContext().getService(serviceName, IModelReference.class);
        if (modelRef == null) {
            log.warn("The service '{}' is not available", serviceName);
            return null;
        }
        return modelRef.getModel();
    }

    /**
     * @return the model with the HTML markup string that will be edited with CKEditor.
     */
    protected abstract IModel<String> getHtmlModel();

    /**
     * @return the HTML cleaner for the edited model, or null if the HTML should not be cleaned.
     */
    protected IHtmlCleanerService getHtmlCleanerOrNull() {
        final IPluginConfig config = getPluginConfig();
        final String serviceId = config.getString(CONFIG_HTML_CLEANER_SERVICE_ID, IHtmlCleanerService.class.getName());

        if (StringUtils.isBlank(serviceId)) {
            log.info("CKEditor plugin '{}' does not use an HTML cleaner", config.getName());
            return null;
        }

        final IHtmlCleanerService service = getPluginContext().getService(serviceId, IHtmlCleanerService.class);

        if (service != null) {
            log.info("CKEditor plugin '{}' uses HTML cleaner '{}'", config.getName(), serviceId);
        } else {
            log.warn("CKEditor plugin '" + config.getName() + "'"
                    + " cannot load HTML cleaner '" + serviceId + "'"
                    + " as specified in configuration property '" + CONFIG_HTML_CLEANER_SERVICE_ID + "'."
                    + " No HTML cleaner will be used.");
        }

        return service;
    }

}
