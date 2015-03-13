/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.upload.jquery;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.upload.FileUploadException;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.upload.FileUploadViolationException;
import org.hippoecm.frontend.plugins.yui.upload.validation.DefaultUploadValidationService;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

/**
 * The multi-files upload dialog using jQuery File Upload plugin
 */
public abstract class JQueryFileUploadDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JQueryFileUploadDialog.class);

    public static final String FILEUPLOAD_WIDGET_ID = "uploadPanel";
    public static final String UPLOADING_SCRIPT = "jqueryFileUploadImpl.uploadFiles()";
    private final IPluginContext pluginContext;
    private final IPluginConfig pluginConfig;

    private FileUploadWidget fileUploadWidget;

    private final FileUploadValidationService validator;
    private final Button uploadButton;

    protected JQueryFileUploadDialog(final IPluginContext pluginContext, final IPluginConfig pluginConfig){
        setOutputMarkupId(true);
        setMultiPart(true);

        setOkVisible(false);
        setOkEnabled(false);

        uploadButton = new AjaxButton(DialogConstants.BUTTON, new StringResourceModel("button-upload-label", this, null)){
            @Override
            protected String getOnClickScript(){
                return UPLOADING_SCRIPT;
            }
        };
        uploadButton.add(new InputBehavior(new KeyType[]{KeyType.Enter}, EventType.click));
        uploadButton.setOutputMarkupId(true);
        this.addButton(uploadButton);

        this.pluginContext = pluginContext;
        this.pluginConfig = pluginConfig;
        this.validator = getValidator();

        createComponents();
    }

     private void createComponents() {
        fileUploadWidget = new FileUploadWidget(FILEUPLOAD_WIDGET_ID, pluginConfig, validator){

            @Override
            protected void onFileUpload(final FileUpload fileUpload) throws FileUploadViolationException {
                JQueryFileUploadDialog.this.handleFileUpload(fileUpload);
            }

            @Override
            protected void onFinished() {
                JQueryFileUploadDialog.this.onFinished();
            }
        };
        add(fileUploadWidget);
    }

    /**
     * Invoke file upload event and translate error messages.
     *
     * @param file
     * @throws FileUploadViolationException
     */
    private void handleFileUpload(final FileUpload file) throws FileUploadViolationException {
        try {
            onFileUpload(file);
        } catch (FileUploadException e) {
            List<String> errors = new ArrayList<>();
            Throwable t = e;
            while(t != null) {
                final String translatedMessage = (String) getExceptionTranslation(t, file.getClientFileName()).getObject();
                if (translatedMessage != null && !errors.contains(translatedMessage)) {
                    errors.add(translatedMessage);
                }
                t = t.getCause();
            }
            if (log.isDebugEnabled()) {
                log.debug("FileUploadException caught: {}", StringUtils.join(errors.toArray(), ";"), e);
            } else {
                log.info("FileUploadException caught: ", e);
            }
            throw new FileUploadViolationException(errors);
        }
    }

    /**
     * Called when uploading files has done.
     */
    protected void onFinished() {
        log.debug("Finished uploading");
    }

    protected FileUploadValidationService getValidator() {
        String serviceId = pluginConfig.getString(FileUploadValidationService.VALIDATE_ID, "service.gallery.image.validation");
        FileUploadValidationService validator = pluginContext.getService(serviceId, FileUploadValidationService.class);

        if (validator == null) {
            validator = new DefaultUploadValidationService();
        }
        return validator;
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.MEDIUM;
    }

    protected abstract void onFileUpload(FileUpload file) throws FileUploadException;
}
