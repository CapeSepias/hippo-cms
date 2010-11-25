/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.editor.workflow.action;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.editor.NamespaceValidator;
import org.hippoecm.editor.repository.NamespaceWorkflow;
import org.hippoecm.editor.template.JcrTemplateStore;
import org.hippoecm.editor.type.JcrDraftLocator;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.editor.layout.ILayoutProvider;
import org.hippoecm.frontend.editor.workflow.RemodelWorkflowPlugin;
import org.hippoecm.frontend.editor.workflow.TemplateFactory;
import org.hippoecm.frontend.editor.workflow.dialog.CreateCompoundTypeDialog;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.repository.api.Workflow;

public class NewCompoundTypeAction extends Action {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private ILayoutProvider layoutProvider;

    public String name;
    public String layout;

    public NewCompoundTypeAction(RemodelWorkflowPlugin plugin, ILayoutProvider layouts) {
        super(plugin, "new-compound-type", new StringResourceModel("new-compound-type", plugin, null));
        this.layoutProvider = layouts;
    }

    @Override
    protected Dialog createRequestDialog() {
        return new CreateCompoundTypeDialog(this, layoutProvider);
    }

    @Override
    protected String execute(Workflow wf) throws Exception {
        NamespaceValidator.checkName(name);

        if (layout == null) {
            throw new Exception("No layout specified");
        }

        // create type
        NamespaceWorkflow workflow = (NamespaceWorkflow) wf;
        workflow.addType("compound", name);

        String prefix = (String) workflow.hints().get("prefix");

        // create layout
        // FIXME: should be managed by template engine
        JcrTemplateStore templateStore = new JcrTemplateStore(new JcrDraftLocator(prefix));
        IClusterConfig template = new TemplateFactory().createTemplate(layoutProvider.getDescriptor(layout));
        template.put("type", prefix + ":" + name);
        templateStore.save(template);

        openEditor(prefix + ":" + name);

        return null;
    }
    
    @Override
    protected ResourceReference getIcon() {
        return new ResourceReference(StdWorkflow.class, "compound-new-16.png");
    }
    
}
