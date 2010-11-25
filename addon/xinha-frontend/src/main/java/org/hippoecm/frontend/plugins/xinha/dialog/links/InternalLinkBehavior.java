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
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.xinha.dialog.AbstractXinhaDialog;
import org.hippoecm.frontend.plugins.xinha.dialog.XinhaDialogBehavior;
import org.hippoecm.frontend.plugins.xinha.services.links.InternalXinhaLink;
import org.hippoecm.frontend.plugins.xinha.services.links.XinhaLinkService;

public class InternalLinkBehavior extends XinhaDialogBehavior {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private XinhaLinkService linkService;

    public InternalLinkBehavior(IPluginContext context, IPluginConfig config, XinhaLinkService service) {
        super(context, config);
        this.linkService = service;
    }

    @Override
    protected void respond(AjaxRequestTarget target) {
        IModel<InternalXinhaLink> model = new Model<InternalXinhaLink>(linkService.create(getParameters()));
        AbstractXinhaDialog<InternalXinhaLink> dialog = new DocumentBrowserDialog<InternalXinhaLink>(
                getPluginContext(), getPluginConfig(), model);
        getDialogService().show(dialog);
    }

}
