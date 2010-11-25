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

package org.hippoecm.frontend.plugins.xinha.dialog.links;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.xinha.dialog.AbstractBrowserDialog;
import org.hippoecm.frontend.plugins.xinha.model.DocumentLink;
import org.hippoecm.frontend.widgets.BooleanFieldWidget;
import org.hippoecm.frontend.widgets.ThrottledTextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentBrowserDialog<T extends DocumentLink> extends AbstractBrowserDialog<T> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(DocumentBrowserDialog.class);

    public DocumentBrowserDialog(IPluginContext context, IPluginConfig config, IModel<T> model) {
        super(context, config, model);

        add(new ThrottledTextFieldWidget("title", new StringPropertyModel(model, DocumentLink.TITLE)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                checkState();
            }
        });

        add(new BooleanFieldWidget("popup", new PropertyModel<Boolean>(model, "target")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                checkState();
            }
        });

        checkState();
    }

    @Override
    protected void onOk() {
        if (getModelObject().isValid()) {
            getModelObject().save();
        } else {
            error("Please select a document");
        }
    }

}
