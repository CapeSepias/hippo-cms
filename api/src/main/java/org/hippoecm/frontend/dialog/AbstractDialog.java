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
package org.hippoecm.frontend.dialog;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.AjaxIndicatorAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessagesModel;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.DefaultMarkupCacheKeyProvider;
import org.apache.wicket.markup.DefaultMarkupResourceStreamProvider;
import org.apache.wicket.markup.IMarkupCacheKeyProvider;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.MarkupException;
import org.apache.wicket.markup.MarkupNotFoundException;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IFormSubmittingComponent;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.widgets.AjaxUpdatingWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

/**
 * Utility class for implementing the {@link IDialogService.Dialog} interface.
 * Provides OK and Cancel buttons by default, that can be manipulated.
 */
public abstract class AbstractDialog<T> extends Form<T> implements IDialogService.Dialog, IAjaxIndicatorAware {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(AbstractDialog.class);

    protected final static IValueMap SMALL = new ValueMap("width=380,height=250").makeImmutable();
    protected final static IValueMap MEDIUM = new ValueMap("width=475,height=375").makeImmutable();
    protected final static IValueMap LARGE = new ValueMap("width=855,height=450").makeImmutable();

    static private IMarkupCacheKeyProvider cacheKeyProvider = new DefaultMarkupCacheKeyProvider();
    static private IMarkupResourceStreamProvider streamProvider = new DefaultMarkupResourceStreamProvider();

    protected static class PersistentFeedbackMessagesModel extends FeedbackMessagesModel {
        private static final long serialVersionUID = 1L;
        private List<FeedbackMessage> messages;

        private PersistentFeedbackMessagesModel(Component component) {
            super(component);
        }

        protected void reset() {
            messages = null;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected List processMessages(final List messages) {
            if (this.messages == null) {
                this.messages = messages;
            }
            return this.messages;
        }

    }

    @SuppressWarnings("unchecked")
    private class Container extends Panel implements IMarkupCacheKeyProvider, IMarkupResourceStreamProvider {
        private static final long serialVersionUID = 1L;

        public Container(String id) {
            super(id);
        }

        public String getCacheKey(MarkupContainer container, Class containerClass) {
            return cacheKeyProvider.getCacheKey(AbstractDialog.this, AbstractDialog.this.getClass());
        }

        // implement IMarkupResourceStreamProvider.
        public IResourceStream getMarkupResourceStream(MarkupContainer container, Class containerClass) {
            return streamProvider.getMarkupResourceStream(AbstractDialog.this, AbstractDialog.this.getClass());
        }

        // used for markup inheritance (<wicket:extend />)
        @Override
        public MarkupStream getAssociatedMarkupStream(final boolean throwException) {
            try {
                return getApplication().getMarkupSettings().getMarkupCache().getMarkupStream(AbstractDialog.this,
                        false, throwException);
            } catch (MarkupException ex) {
                // re-throw it. The exception contains already all the information
                // required.
                throw ex;
            } catch (WicketRuntimeException ex) {
                // throw exception since there is no associated markup
                throw new MarkupNotFoundException(
                        exceptionMessage("Markup of type '"
                                + getMarkupType()
                                + "' for component '"
                                + AbstractDialog.this.getClass().getName()
                                + "' not found."
                                + " Enable debug messages for org.apache.wicket.util.resource to get a list of all filenames tried"),
                        ex);
            }
        }
    }

    protected class ExceptionFeedbackPanel extends FeedbackPanel {
        private static final long serialVersionUID = 1L;

        protected ExceptionFeedbackPanel(String id) {
            super(id);
            setOutputMarkupId(true);
        }

        protected class ExceptionLabel extends Panel {
            private static final long serialVersionUID = 1L;

            protected ExceptionLabel(String id, IModel<String> model, final Exception ex, boolean escape) {
                super(id);
                setOutputMarkupId(true);

                Link<String> link = new Link<String>("message") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick() {
                        RequestCycle.get().setRequestTarget(new ErrorDownloadRequestTarget(ex));
                    }
                };
                Label label = new Label("label", model);
                label.setEscapeModelStrings(escape);
                link.add(label);
                add(link);
            }
        }

        @Override
        protected Component newMessageDisplayComponent(String id, FeedbackMessage message) {
            Serializable serializable = message.getMessage();
            if (serializable instanceof Exception) {
                Exception ex = (Exception) serializable;
                Map<String, String> details = new HashMap<String, String>();
                details.put("type", ex.getClass().getName());
                details.put("message", ex.getMessage());
                StackTraceElement[] elements = ex.getStackTrace();
                if (elements.length > 0) {
                    StackTraceElement top = elements[0];
                    details.put("clazz", top.getClassName());
                }
                ExceptionLabel label = new ExceptionLabel(id, new StringResourceModel(
                        "exception,type=${type},message=${message}"
                                + (details.containsKey("clazz") ? ",class=${clazz}" : ""), AbstractDialog.this,
                        new Model<Serializable>((Serializable) details), ex.getLocalizedMessage()), ex,
                        ExceptionFeedbackPanel.this.getEscapeModelStrings());
                return label;
            } else {
                Label label = new Label(id);
                label.setDefaultModel(new Model<String>(serializable == null ? "" : serializable.toString()));
                label.setEscapeModelStrings(ExceptionFeedbackPanel.this.getEscapeModelStrings());
                return label;
            }
        }

        @Override
        protected FeedbackMessagesModel newFeedbackMessagesModel() {
            return AbstractDialog.this.getFeedbackMessagesModel();
        }
    }

    class ButtonWrapper implements IClusterable {
        private static final long serialVersionUID = 1L;

        private Button button;

        private boolean ajax;
        private IModel<String> label;
        private boolean visible;
        private boolean enabled;

        public ButtonWrapper(Button button) {
            this.button = button;
            visible = button.isVisible();
            enabled = button.isEnabled();
            label = button.getModel();

            if (button instanceof AjaxButton) {
                ajax = true;
            }
        }

        public ButtonWrapper(IModel<String> label) {
            this(label, true);
        }

        public ButtonWrapper(IModel<String> label, boolean ajax) {
            this.ajax = ajax;
            this.label = label;
            this.visible = true;
            this.enabled = true;
        }

        private Button createButton() {
            if (ajax) {
                AjaxButton button = new AjaxButton(getButtonId()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        if (!closing) {
                            ButtonWrapper.this.onSubmit();
                        }
                    }

                    @Override
                    public boolean isVisible() {
                        return visible;
                    }

                    @Override
                    public boolean isEnabled() {
                        return enabled;
                    }
                };
                button.setModel(label);
                return button;
            } else {
                Button button = new Button(getButtonId()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit() {
                        if (!closing) {
                            ButtonWrapper.this.onSubmit();
                        }
                    }

                    @Override
                    public boolean isVisible() {
                        return visible;
                    }

                    @Override
                    public boolean isEnabled() {
                        return enabled;
                    }
                };
                button.setModel(label);
                return button;
            }
        }

        public Button getButton() {
            if (button == null) {
                button = decorate(createButton());
            }
            return button;
        }

        protected Button decorate(Button button) {
            button.setEnabled(enabled);
            button.setVisible(visible);
            if (getKeyType() != null) {
                button.add(new InputBehavior(new KeyType[] { getKeyType() }, EventType.click));
            }
            return button;
        }

        public void setEnabled(boolean isset) {
            enabled = isset;
            if(button != null) {
                button.setEnabled(isset);
                if (ajax) {
                    AjaxRequestTarget target = AjaxRequestTarget.get();
                    if (target != null) {
                        target.addComponent(button);
                    }
                }
            }
        }

        public void setVisible(boolean isset) {
            visible = isset;
            if (button != null) {
                button.setVisible(isset);
                if(ajax) {
                    AjaxRequestTarget target = AjaxRequestTarget.get();
                    if (target != null) {
                        target.addComponent(button);
                    }
                }
            }
        }

        public void setAjax(boolean c) {
            ajax = c;
        }

        public void setLabel(IModel<String> label) {
            this.label = label;
            if (button != null) {
                button.setModel(label);
            }
            //TODO: test if this works or if it needs to add itself to the render target
        }

        protected void onSubmit() {
        }

        public boolean hasChanges() {
            if (!ajax) {
                return false;
            }

            if (button == null) {
                return true;
            }

            if (visible != button.isVisible()) {
                return true;
            }

            if (enabled != button.isEnabled()) {
                return true;
            }
            return false;
        }
        
        protected KeyType getKeyType() {
            return null;
        }

    }

    private final static ResourceReference AJAX_LOADER_GIF = new ResourceReference(AbstractDialog.class,
            "ajax-loader.gif");

    protected PersistentFeedbackMessagesModel fmm;
    protected FeedbackPanel feedback;
    private Component focusComponent;

    private LinkedList<ButtonWrapper> buttons;
    private final ButtonWrapper ok;
    private final ButtonWrapper cancel;

    private IDialogService dialogService;
    private Panel container;
    private AjaxIndicatorAppender indicator;

    private transient boolean closing = false;
    protected boolean cancelled = false;

    public AbstractDialog() {
        this(null);
    }

    public AbstractDialog(IModel<T> model) {
        super("form", model);

        setOutputMarkupId(true);

        container = new Container(IDialogService.DIALOG_WICKET_ID);
        container.add(this);
        
        feedback = new ExceptionFeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        buttons = new LinkedList<ButtonWrapper>();
        ListView<ButtonWrapper> buttonsView = new ListView<ButtonWrapper>("buttons", buttons) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ButtonWrapper> item) {
                item.add(item.getModelObject().getButton());
            }
        };
        buttonsView.setReuseItems(true);
        buttonsView.setOutputMarkupId(true);
        add(buttonsView);

        ok = new ButtonWrapper(new StringResourceModel("ok", AbstractDialog.this, null)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit() {
                handleSubmit();
            }
            
            @Override
            protected KeyType getKeyType() {
                return KeyType.Enter;
            }

        };
        buttons.add(ok);

        cancel = new ButtonWrapper(new ResourceModel("cancel")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit() {
                cancelled = true;
                onCancel();
                closeDialog();
            }

            @Override
            protected Button decorate(Button b) {
                b.setDefaultFormProcessing(false);
                return super.decorate(b);
            }
            
            @Override
            protected KeyType getKeyType() {
                return KeyType.Escape;
            }

        };
        buttons.add(cancel);

        add(indicator = new AjaxIndicatorAppender() {
            private static final long serialVersionUID = 1L;

            @Override
            protected CharSequence getIndicatorUrl() {
                return RequestCycle.get().urlFor(AJAX_LOADER_GIF);
            }
        });
        
    }
    
    @Override
    protected void onDetach() {
        if (fmm != null) {
            fmm.detach();
        }
        super.onDetach();
    }

    @Deprecated
    protected void onDefaultSubmit() {
        handleSubmit();
    }
    
    protected FeedbackPanel newFeedbackPanel(String id) {
        return new ExceptionFeedbackPanel(id);
    }

    protected final FeedbackMessagesModel getFeedbackMessagesModel() {
        if (fmm == null) {
            fmm = new PersistentFeedbackMessagesModel(this);
        }
        return fmm;
    }

    public String getAjaxIndicatorMarkupId() {
        return indicator.getMarkupId();
    }

    protected final void closeDialog() {
        if (!closing) {
            closing = true;
            dialogService.close();
        }
    }

    // button manipulation routines

    public void setNonAjaxSubmit() {
        ok.setAjax(false);
    }

    protected void setOkEnabled(boolean isset) {
        ok.setEnabled(isset);
    }

    protected void setOkVisible(boolean isset) {
        ok.setVisible(isset);
    }

    protected void setOkLabel(String label) {
        setOkLabel(new Model<String>(label));
    }

    protected void setOkLabel(IModel<String> label) {
        ok.setLabel(label);
    }

    protected void setFocusOnOk() {
        setFocus(ok.getButton());
    }

    protected void setCancelEnabled(boolean isset) {
        cancel.setEnabled(isset);
    }

    protected void setCancelVisible(boolean isset) {
        cancel.setVisible(isset);
    }

    protected void setCancelLabel(String label) {
        setCancelLabel(new Model<String>(label));
    }

    protected void setCancelLabel(IModel<String> label) {
        cancel.setLabel(label);
    }

    protected void setFocusOnCancel() {
        setFocus(cancel.getButton());
    }

    /**
     * {@inheritDoc}
     */
    public void setDialogService(IDialogService dialogService) {
        this.dialogService = dialogService;
    }

    protected String getButtonId() {
        return "button";
    }

    /**
     * Add a {@link Button} to the button bar.  The id of the button must equal "button".
     */
    protected void addButton(Button button) {
        if (getButtonId().equals(button.getId())) {
            buttons.addFirst(new ButtonWrapper(button));
        } else {
            log.error("Failed to add button: component id is not '{}'", getButtonId());
        }
    }

    /**
     * Remove a button from the button bar.
     */
    protected void removeButton(Button button) {
        for (ButtonWrapper bw : buttons) {
            if (bw.getButton().equals(button)) {
                buttons.remove(bw);
                break;
            }
        }
    }

    protected void handleSubmit() {
        onOk();
        if (!hasError()) {
            closeDialog();
        }
    }

    @Override
    protected final void onSubmit() {
        Page page = findParent(Page.class);
        if (page != null) {
            if (fmm != null) {
                fmm.reset();
            }
            IFormSubmittingComponent submitButton = findSubmittingButton();
            if (submitButton == null) {
                handleSubmit();
            }
        }
    }

    @Override
    protected final void onError() {
        Page page = findParent(Page.class);
        if (page != null) {
            if (fmm != null) {
                fmm.reset();
            }
        }
        super.onError();
    }
    
    protected void onOk() {
    }

    protected void onCancel() {
    }

    /**
     * {@inheritDoc}
     */
    public Component getComponent() {
        return container;
    }

    /**
     * {@inheritDoc}
     */
    public void render(PluginRequestTarget target) {
        if (target != null) {
            target.addComponent(feedback);
            for (ButtonWrapper bw : buttons) {
                if (bw.hasChanges()) {
                    target.addComponent(bw.getButton());
                }
            }

            if (focusComponent != null) {
                target.focusComponent(focusComponent);
                focusComponent = null;
            }
        }
    }
    
    public void onClose() {
        AjaxRequestTarget target = AjaxRequestTarget.get();
        if (target != null) {
            for (ButtonWrapper bw : buttons) {
                if (bw.getKeyType() != null) {
                    target.appendJavascript("shortcut.remove('" + bw.getKeyType() + "');\n");
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public IValueMap getProperties() {
        return LARGE;
    }

    public Component setFocus(Component c) {
        if (focusComponent != null) {
            return c;
        }

        if (!c.getOutputMarkupId()) {
            c.setOutputMarkupId(true);
        }
        return focusComponent = c;
    }

    public AjaxUpdatingWidget<?> setFocus(AjaxUpdatingWidget<?> widget) {
        setFocus(widget.getFocusComponent());
        return widget;
    }

}
