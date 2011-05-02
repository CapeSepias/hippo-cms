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
package org.hippoecm.addon.workflow;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.ExceptionDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StdWorkflow<T extends Workflow> extends ActionDescription {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(CompatibilityWorkflowPlugin.class);

    private String name;
    private ResourceReference iconModel;
    private IPluginContext pluginContext;
    private RenderPlugin<? extends WorkflowDescriptor> enclosingPlugin;

    public StdWorkflow(String id, String name) {
        super(id);
        this.name = name;
        this.iconModel = null;
        add(new ActionDisplay("text") {
            @Override
            protected void initialize() {
                IModel<String> title = getTitle();
                Label titleLabel = new Label("text", title);
                titleLabel.add(new AttributeModifier("title", true, title));
                add(titleLabel);
            }
        });

        add(new ActionDisplay("icon") {
            @Override
            protected void initialize() {
                ResourceReference model = getIcon();
                add(new Image("icon", model));
            }
        });

        add(new ActionDisplay("panel") {
            @Override
            protected void initialize() {
            }
        });
    }

    public StdWorkflow(String id, String name, IPluginContext pluginContext, RenderPlugin<? extends WorkflowDescriptor> enclosingPlugin) {
        this(id, name);
        this.pluginContext = pluginContext;
        this.enclosingPlugin = enclosingPlugin;
    }

    public StdWorkflow(String id, StringResourceModel name, IPluginContext pluginContext, RenderPlugin<? extends WorkflowDescriptor> enclosingPlugin) {
        this(id, name.getObject());
        this.pluginContext = pluginContext;
        this.enclosingPlugin = enclosingPlugin;
    }

    public StdWorkflow(String id, StringResourceModel name, ResourceReference iconModel, IPluginContext pluginContext, RenderPlugin<? extends WorkflowDescriptor> enclosingPlugin) {
        this(id, name.getObject());
        this.iconModel = iconModel;
        this.pluginContext = pluginContext;
        this.enclosingPlugin = enclosingPlugin;
    }

    public StdWorkflow(String id, String name, ResourceReference iconModel, IPluginContext pluginContext, RenderPlugin<? extends WorkflowDescriptor> enclosingPlugin) {
        this(id, name);
        this.iconModel = iconModel;
        this.pluginContext = pluginContext;
        this.enclosingPlugin = enclosingPlugin;
    }

    protected final String getName() {
        return name;
    }

    protected IModel getTitle() {
        return new StringResourceModel(getName(), this, null, getName());
    }

    protected ResourceReference getIcon() {
        if (iconModel != null) {
            return iconModel;
        } else {
            return new ResourceReference(StdWorkflow.class, "workflow-16.png");
        }
    }

    @Override
    protected IModel initModel() {
        if (enclosingPlugin != null) {
            return enclosingPlugin.getDefaultModel();
        } else {
            return super.initModel();
        }
    }

    protected Dialog createRequestDialog() {
        return null;
    }

    protected Dialog createResponseDialog(String message) {
        return new ExceptionDialog(message);
    }

    protected Dialog createResponseDialog(Exception ex) {
        return new ExceptionDialog(ex);
    }

    @Override
    protected void invoke() {
        Dialog dialog = createRequestDialog();
        if (dialog != null) {
            pluginContext.getService(IDialogService.class.getName(), IDialogService.class).show(dialog);
        } else {
            try {
                execute();
            } catch (WorkflowException ex) {
                log.info("Workflow call failed", ex);
                pluginContext.getService(IDialogService.class.getName(), IDialogService.class).show(createResponseDialog(ex));
            } catch (Exception ex) {
                log.info("Workflow call failed", ex);
                pluginContext.getService(IDialogService.class.getName(), IDialogService.class).show(createResponseDialog(ex));
            }
        }
    }

    protected void execute() throws Exception {
        execute((WorkflowDescriptorModel<T>)enclosingPlugin.getDefaultModel());
    }

    protected void execute(WorkflowDescriptorModel<T> model) throws Exception {
        WorkflowDescriptor descriptor = (WorkflowDescriptor)model.getObject();
        if (descriptor == null) {
            throw new MappingException("action no longer valid");
        }
        WorkflowManager manager = ((UserSession)org.apache.wicket.Session.get()).getWorkflowManager();
        javax.jcr.Session session = ((UserSession)org.apache.wicket.Session.get()).getJcrSession();
        session.refresh(true);
        session.save();
        session.refresh(true);
        Workflow workflow = manager.getWorkflow(descriptor);
        String message = execute((T)workflow);
        if (message != null) {
            throw new WorkflowException(message);
        }

        // workflow may have closed existing session
        // FIXME should be removed
        UserSession us = (UserSession)org.apache.wicket.Session.get();
        session = us.getJcrSession();
        session.refresh(false);
        us.getFacetRootsObserver().broadcastEvents();
    }

    protected String execute(T workflow) throws Exception {
        throw new WorkflowException("unsupported operation");
    }
}