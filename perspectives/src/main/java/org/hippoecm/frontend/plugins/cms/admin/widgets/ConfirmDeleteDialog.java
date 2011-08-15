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
package org.hippoecm.frontend.plugins.cms.admin.widgets;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;

/**
 * Dialog for easy creating confirmation dialogs;
 */
public class ConfirmDeleteDialog extends AbstractDialog {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    protected IModel model;
    protected Component component;

    public ConfirmDeleteDialog() {
        add(new Label("label", new ResourceModel(getTextKey())));
        setFocusOnCancel();
    }

    public ConfirmDeleteDialog(final IModel model, final Component component) {
        super();
        this.model = model;
        this.component = component;
        add(new Label("label", new StringResourceModel(getTextKey(), component, model)));
        setFocusOnCancel();
    }

    public IModel getTitle() {
        if (component == null) {
            return new ResourceModel(getTitleKey());
        } else {
            return new StringResourceModel(getTitleKey(), component, model);
        }
    }

    protected String getTitleKey() {
        return "confirm-delete-title";
    }

    protected String getTextKey() {
        return "confirm-delete-text";
    }

    @Override
    public IValueMap getProperties() {
        return SMALL;
    }

}
