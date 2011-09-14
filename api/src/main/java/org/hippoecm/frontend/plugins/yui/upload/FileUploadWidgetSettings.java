/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugins.yui.upload;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

/**
 * Settings for file uploads. Currently allowed configurable settings are:
 * <ul>
 *     <li>fileupload.flashEnabled = <code>true</code> for flash or <code>false</code> for javascript upload</li>
 *     <li>fileupload.maxItems = maximum allowed file uploads at the same time</li>
 *     <li>file.extensions = allowed upload file extensions (backwards compatibility)</li>
 *     <li>fileupload.allowedExtensions = allowed upload file extensions</li>
 *     <li>fileupload.autoUpload = if <code>true</code> the plugin will automatically upload the files</li>
 * </ul>
 */
public class FileUploadWidgetSettings implements IClusterable{
    @SuppressWarnings("unused")
    final static String SVN_ID = "$Id$";

    public static final String FILEUPLOAD_FLASH_ENABLED_SETTING = "fileupload.flashEnabled";
    public static final String FILEUPLOAD_MAX_ITEMS_SETTING = "fileupload.maxItems";
    public static final String FILEUPLOAD_AUTOUPLOAD_SETTING = "fileupload.autoUpload";
    public static final String FILEUPLOAD_ALLOWED_EXTENSIONS_SETTING = "fileupload.allowedExtensions";

    //backwards compatibility
    public static final String FILE_EXTENSIONS_SETTING = "file.extensions";

    private String[] fileExtensions = new String[0];
    private int maxNumberOfFiles = 1;
    private boolean autoUpload;
    private boolean clearAfterUpload;
    private int clearTimeout = 1000;
    private boolean hideBrowseDuringUpload;
    private String buttonWidth;
    private String buttonHeight;
    private boolean flashUploadEnabled = true;

    public FileUploadWidgetSettings() {
    } 

    public FileUploadWidgetSettings(IPluginConfig pluginConfig) {
        parsePluginConfig(pluginConfig);
    }

    public void setFileExtensions(String[] fileExtensions) {
        this.fileExtensions = fileExtensions;
    }

    public String[] getFileExtensions() {
        return fileExtensions;
    }

    public void setMaxNumberOfFiles(int nr) {
        maxNumberOfFiles = nr;
    }

    public int getMaxNumberOfFiles() {
        return maxNumberOfFiles;
    }

    public void setAutoUpload(boolean set) {
        autoUpload = set;
    }

    public boolean isAutoUpload() {
        return autoUpload;
    }

    public void setClearAfterUpload(boolean clear) {
        this.clearAfterUpload = clear;
    }

    public boolean isClearAfterUpload() {
        return clearAfterUpload;
    }

    public int getClearTimeout() {
        return clearTimeout;
    }

    public void setClearTimeout(int clearTimeout) {
        this.clearTimeout = clearTimeout;
    }

    public void setHideBrowseDuringUpload(boolean hideBrowseDuringUpload) {
        this.hideBrowseDuringUpload = hideBrowseDuringUpload;
    }

    public boolean isHideBrowseDuringUpload() {
        return hideBrowseDuringUpload;
    }

    public String getButtonWidth() {
        return buttonWidth;
    }

    public void setButtonWidth(String buttonWidth) {
        this.buttonWidth = buttonWidth;
    }

    public String getButtonHeight() {
        return buttonHeight;
    }

    public void setButtonHeight(String buttonHeight) {
        this.buttonHeight = buttonHeight;
    }

    /**
     * Indicates if the upload widget should use Flash.
     * @return <code>true</code> if flash should be used, <code>false</code> otherwise
     */
    public boolean isFlashUploadEnabled() {
        return flashUploadEnabled;
    }

    /**
     * If set to <code>true</code> (default) the upload plugin will use flash for file uploads, otherwise it will use a plain
     * Javascript upload.
     * @param flashUploadEnabled boolean indicating if flash should be used for file uploads.
     */
    public void setFlashUploadEnabled(boolean flashUploadEnabled) {
        this.flashUploadEnabled = flashUploadEnabled;
    }

    private void parsePluginConfig(final IPluginConfig pluginConfig) {
        if(pluginConfig.containsKey(FILEUPLOAD_FLASH_ENABLED_SETTING)) {
            this.flashUploadEnabled = pluginConfig.getAsBoolean(FILEUPLOAD_FLASH_ENABLED_SETTING);
        }
        if(pluginConfig.containsKey(FILEUPLOAD_MAX_ITEMS_SETTING)) {
            this.maxNumberOfFiles = pluginConfig.getAsInteger(FILEUPLOAD_MAX_ITEMS_SETTING);
        }
        // for backwards compatibility
        if (pluginConfig.containsKey(FILE_EXTENSIONS_SETTING)) {
            this.fileExtensions = pluginConfig.getStringArray(FILE_EXTENSIONS_SETTING);
        }
        if (pluginConfig.containsKey(FILEUPLOAD_ALLOWED_EXTENSIONS_SETTING)) {
            this.fileExtensions = pluginConfig.getStringArray(FILEUPLOAD_ALLOWED_EXTENSIONS_SETTING);
        }
        if (pluginConfig.containsKey(FILEUPLOAD_AUTOUPLOAD_SETTING)) {
            this.autoUpload = pluginConfig.getAsBoolean(FILEUPLOAD_AUTOUPLOAD_SETTING);
        }
    }

}
