/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class PreviewWorkflowPlugin extends AbstractPreviewWorkflowPlugin {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings({"unused", "FieldCanBeLocal"}) // used by a PropertyModel
    private String inUseBy;

    public PreviewWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
        inUseBy = getHint("inUseBy");
    }

    boolean getInfoEditActionVisibility() {
        return StringUtils.isNotEmpty(inUseBy);
    }

    StdWorkflow getInfoEditAction() {
        return new StdWorkflow("infoEdit", "infoEdit") {

            @Override
            public String getSubMenu() {
                return "info";
            }

            @Override
            protected IModel getTitle() {
                return new StringResourceModel("in-use-by", this, null,
                        new PropertyModel(PreviewWorkflowPlugin.this, "inUseBy"));
            }

            @Override
            protected void invoke() {
                // The infoEdit workflow only shows feedback based on the hints.
                // It does not show any dialog.
            }
        };
    }
}
