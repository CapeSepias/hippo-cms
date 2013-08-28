/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.ckeditor;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptUrlReferenceHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.ckeditor.CKEditorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders an instance of CKEditor to edit the HTML in the given model.
 * Additional behavior can be added via the {@link #addBehavior(CKEditorPanelBehavior)} method.
 */
public class CKEditorPanel extends Panel {

    private static final String WICKET_ID_EDITOR = "editor";
    private static final ResourceReference CKEDITOR_PANEL_CSS = new PackageResourceReference(CKEditorPanel.class, "CKEditorPanel.css");
    private static final ResourceReference CKEDITOR_PANEL_JS = new PackageResourceReference(CKEditorPanel.class, "CKEditorPanel.js");
    private static final int LOGGED_EDITOR_CONFIG_INDENT_SPACES = 2;

    private static final Logger log = LoggerFactory.getLogger(CKEditorPanel.class);

    private final String editorConfigJson;
    private final String editorId;
    private final List<CKEditorPanelBehavior> behaviors;

    public CKEditorPanel(final String id,
                  final String editorConfigJson,
                  final IModel<String> editModel) {
        super(id);

        this.editorConfigJson = editorConfigJson;

        final TextArea<String> textArea = new TextArea<String>(WICKET_ID_EDITOR, editModel);
        textArea.setOutputMarkupId(true);
        add(textArea);

        editorId = textArea.getMarkupId();

        behaviors = new LinkedList<CKEditorPanelBehavior>();
    }

    /**
     * @return the ID of the editor instance.
     */
    public String getEditorId() {
        return editorId;
    }

    /**
     * Adds custom server-side behavior to this panel.
     * @param behavior the behavior to add.
     */
    public void addBehavior(CKEditorPanelBehavior behavior) {
        behaviors.add(behavior);

        for (AbstractAjaxBehavior ajaxBehavior : behavior.getAjaxBehaviors()) {
            add(ajaxBehavior);
        }
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(CKEDITOR_PANEL_CSS));
        response.render(JavaScriptUrlReferenceHeaderItem.forReference(CKEditorConstants.CKEDITOR_JS));
        response.render(JavaScriptUrlReferenceHeaderItem.forReference(CKEDITOR_PANEL_JS));

        JSONObject editorConfig = getConfigurationForEditor();
        renderContentsCss(response, editorConfig);
        response.render(OnDomReadyHeaderItem.forScript(getJavaScriptForEditor(editorConfig)));
    }

    private JSONObject getConfigurationForEditor() {
        try {
            JSONObject editorConfig = JsonUtils.createJSONObject(editorConfigJson);

            // configure behaviors
            for (CKEditorPanelBehavior behavior : behaviors) {
                behavior.addCKEditorConfiguration(editorConfig);
            }

            // always use the language of the current CMS locale
            final Locale locale = getLocale();
            editorConfig.put(CKEditorConstants.CONFIG_LANGUAGE, locale.getLanguage());

            // load the localized hippo styles if no other styles are specified
            JsonUtils.putIfAbsent(editorConfig, CKEditorConstants.CONFIG_STYLES_SET, HippoStyles.getConfigStyleSet(locale));

            // use a div-based editor instead of an iframe-based one to decrease loading time for many editor instances
            JsonUtils.appendToCommaSeparatedString(editorConfig, CKEditorConstants.CONFIG_EXTRA_PLUGINS, CKEditorConstants.PLUGIN_DIVAREA);

            // disable custom config loading if not configured
            JsonUtils.putIfAbsent(editorConfig, CKEditorConstants.CONFIG_CUSTOM_CONFIG, StringUtils.EMPTY);

            if (log.isInfoEnabled()) {
                log.info("CKEditor configuration:\n" + editorConfig.toString(LOGGED_EDITOR_CONFIG_INDENT_SPACES));
            }

            return editorConfig;
        } catch (JSONException e) {
            throw new IllegalStateException("Error creating CKEditor configuration.", e);
        }
    }

    private void renderContentsCss(IHeaderResponse response, JSONObject editorConfig) {
        String contentsCss = "";
        try {
            contentsCss = editorConfig.getString(CKEditorConstants.CONFIG_CONTENTS_CSS);
            response.render(CssHeaderItem.forUrl(contentsCss));
        } catch (JSONException e) {
            log.warn("Cannot render contents CSS '" + contentsCss + "', the default CKEditor CSS will be used instead", e);
        }
    }

    private String getJavaScriptForEditor(JSONObject editorConfig) {
        return "Hippo.createCKEditor('" + editorId + "', " + editorConfig.toString() + ");";
    }

    @Override
    protected void onDetach() {
        for (CKEditorPanelBehavior behavior : behaviors) {
            behavior.detach();
        }
        super.onDetach();
    }

}
