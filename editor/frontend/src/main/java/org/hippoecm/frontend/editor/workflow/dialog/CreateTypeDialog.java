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
package org.hippoecm.frontend.editor.workflow.dialog;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.extensions.wizard.Wizard;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.editor.layout.ILayoutProvider;
import org.hippoecm.frontend.editor.workflow.action.Action;

public abstract class CreateTypeDialog extends Wizard implements IDialogService.Dialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private Action action;
    private FeedbackPanel feedback;
    private IDialogService dialogService;

    public CreateTypeDialog(Action action, ILayoutProvider layouts) {
        super("content", false);

        this.action = action;
        final IFeedbackMessageFilter[] filters = new IFeedbackMessageFilter[2];
        filters[0] = new ContainerFeedbackMessageFilter(action);
        filters[1] = new ContainerFeedbackMessageFilter(this);
        this.feedback = new FeedbackPanel(FEEDBACK_ID, new IFeedbackMessageFilter() {
            private static final long serialVersionUID = 1L;

            public boolean accept(FeedbackMessage message) {
                for (IFeedbackMessageFilter filter : filters) {
                    if (filter.accept(message)) {
                        return true;
                    }
                }
                return false;
            }
        });

        setOutputMarkupId(true);
    }

    @Override
    public void renderHead(final HtmlHeaderContainer container) {
        super.renderHead(container);
        container.getHeaderResponse().renderOnDomReadyJavascript("if (Wicket != null && Wicket.Window != null) { Wicket.Window.unloadConfirmation = false; };");
    }

    @Override
    protected Component newButtonBar(String id) {
        MarkupContainer container = (MarkupContainer) super.newButtonBar(id);
        container.visitChildren(Button.class, new IVisitor() {

            public Object component(Component component) {
                component.add(newOnClickBehavior(component));

                return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
            }

        });

        return container;
    }

    protected IBehavior newOnClickBehavior(final Component component) {
        return new AjaxEventBehavior("onclick") {
            private static final long serialVersionUID = 1L;

            @Override
            protected CharSequence getEventHandler() {
                return new AppendingStringBuffer(super.getEventHandler()).append("; return false;");
            }

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                ((Button) component).onSubmit();
                target.addComponent(CreateTypeDialog.this);
            }

        };
    }

    @Override
    protected FeedbackPanel newFeedbackPanel(String id) {
        return feedback;
    }

    @Override
    public void onCancel() {
        dialogService.close();
    }

    @Override
    public void onFinish() {
        action.execute();

        List<FeedbackMessage> messages = (List) feedback.getFeedbackMessagesModel().getObject();
        for (FeedbackMessage message : messages) {
            if (message.getLevel() == FeedbackMessage.ERROR) {
                return;
            }
        }
        dialogService.close();
    }

    public IValueMap getProperties() {
        return new ValueMap("width=500,height=325");
    }

    public Component getComponent() {
        return this;
    }

    public void onClose() {
    }

    public void render(PluginRequestTarget target) {
    }

    public void setDialogService(IDialogService service) {
        dialogService = service;
    }

}
