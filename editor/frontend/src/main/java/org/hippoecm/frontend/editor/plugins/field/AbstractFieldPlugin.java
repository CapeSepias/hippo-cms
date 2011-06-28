/*
 *  Copyright 2008-2011 Hippo.
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
package org.hippoecm.frontend.editor.plugins.field;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.Item;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.compare.IComparer;
import org.hippoecm.frontend.editor.compare.NodeComparer;
import org.hippoecm.frontend.editor.compare.ObjectComparer;
import org.hippoecm.frontend.model.AbstractProvider;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.event.Observer;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.ListViewPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.ModelPath;
import org.hippoecm.frontend.validation.ModelPathElement;
import org.hippoecm.frontend.validation.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFieldPlugin<P extends Item, C extends IModel> extends ListViewPlugin<P> implements
        ITemplateFactory<C> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final String CLUSTER_OPTIONS = "cluster.options";

    private static final String MAX_ITEMS = "maxitems";
    private static final int DEFAULT_MAX_ITEMS = 0;
    private final int maxItems;
    private IPluginConfig parameters;

    static abstract class ValidationFilter extends Model<String> {
        private static final long serialVersionUID = 1L;

        private boolean valid = true;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public abstract void onValidation(IValidationResult result);

        @Override
        public String getObject() {
            if (valid) {
                return "";
            } else {
                return "invalid";
            }
        }
    }

    static final Logger log = LoggerFactory.getLogger(AbstractFieldPlugin.class);

    public static final String FIELD = "field";
    public static final String TYPE = "type";

    protected IEditor.Mode mode;
    private boolean restartTemplates = true;

    // view and edit modes
    protected AbstractProvider<C> provider;
    private FieldPluginHelper helper;
    private TemplateController<C> templateController;
    private boolean managedValidation = false;
    private Map<Object, ValidationFilter> listeners = new HashMap<Object, ValidationFilter>();

    // compare mode
    private IModel<P> compareTo;
    protected AbstractProvider<C> oldProvider;
    protected AbstractProvider<C> newProvider;
    private ComparingController<C> comparingController;

    protected AbstractFieldPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        this.parameters = new JavaPluginConfig(config.getPluginConfig(CLUSTER_OPTIONS));
        this.maxItems =  config.getInt(MAX_ITEMS, DEFAULT_MAX_ITEMS);

        helper = new FieldPluginHelper(context, config);
        if (helper.getValidationModel() != null && helper.getValidationModel() instanceof IObservable) {
            context.registerService(new Observer((IObservable) helper.getValidationModel()) {
                private static final long serialVersionUID = 1L;

                public void onEvent(Iterator events) {
                    for (ValidationFilter listener : new ArrayList<ValidationFilter>(listeners.values())) {
                        IValidationResult validationResult = helper.getValidationModel().getObject();
                        if (validationResult != null) {
                            listener.onValidation(validationResult);
                        }
                    }
                }

            }, IObserver.class.getName());

        }
        mode = IEditor.Mode.fromString(config.getString(ITemplateEngine.MODE, "view"));
        if (IEditor.Mode.COMPARE == mode) {
            if (config.containsKey("model.compareTo")) {
                IModelReference<P> compareToModelRef = context.getService(config.getString("model.compareTo"),
                        IModelReference.class);
                if (compareToModelRef != null) {
                    // TODO: add observer
                    compareTo = compareToModelRef.getModel();
                    if (compareTo == null) {
                        log.warn("compareTo model is null, falling back to view mode");
                    }
                } else {
                    log.warn("No compareTo.model configured, falling back to view mode");
                }
            } else {
                log.warn("No compareTo model reference for field plugin in editor cluster that implements compare mode, field " + config.getString("field"));
            }
            if (compareTo == null) {
                mode = IEditor.Mode.VIEW;
            }
        }

        if (IEditor.Mode.COMPARE == mode) {
            comparingController = new ComparingController<C>(context, config, this, getComparer(), getItemId());

            if (helper.getField().isMultiple()) {
                // always use managed compare for multi-valued properties
                comparingController.setUseCompareWhenPossible(false);
            }

        } else {
            IModel<IValidationResult> validationModel = null;
            if (IEditor.Mode.EDIT == mode) {
                validationModel = helper.getValidationModel();
            }
            templateController = new TemplateController<C>(context, config, validationModel, this,
                    getItemId());

            provider = getProvider(getModel());

            IFieldDescriptor field = helper.getField();
            if (field != null && !doesTemplateSupportValidation()) {
                final ValidationFilter holder = new ValidationFilter() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onValidation(IValidationResult validation) {
                        boolean valid = true;
                        if (!validation.isValid()) {
                            IFieldDescriptor field = getFieldHelper().getField();
                            for (Violation violation : validation.getViolations()) {
                                Set<ModelPath> paths = violation.getDependentPaths();
                                for (ModelPath path : paths) {
                                    if (path.getElements().length > 0) {
                                        ModelPathElement first = path.getElements()[0];
                                        if (first.getField().equals(field)) {
                                            valid = false;
                                        }
                                        break;
                                    }
                                }
                                if (!valid) {
                                    break;
                                }
                            }
                        }
                        if (valid != isValid()) {
                            redraw();
                            setValid(valid);
                        }
                    }

                };
                if (validationModel != null && validationModel.getObject() != null) {
                    holder.setValid(validationModel.getObject().isValid());
                }
                addValidationFilter(this, holder);

                managedValidation = true;
                if (!field.isMultiple()) {
                    add(new CssClassAppender(holder));
                }
            }
        }
    }

    protected IComparer<?> getComparer() {
        IComparer comparer;
        IFieldDescriptor field = helper.getField();
        if (field != null) {
            ITypeDescriptor type = helper.getField().getTypeDescriptor();
            if (type.isNode()) {
                comparer = new NodeComparer(type);
            } else {
                comparer = new ObjectComparer();
            }
        } else {
            comparer = new ObjectComparer();
        }
        return comparer;
    }

    @Override
    protected String getItemId() {
        String serviceId = getPluginContext().getReference(this).getServiceId();
        return serviceId + ".item";
    }

    @Override
    protected void onDetach() {
        if (provider != null) {
            provider.detach();
        }
        helper.detach();
        if (templateController != null) {
            templateController.detach();
        }
        if (comparingController != null) {
            comparingController.detach();
        }
        super.onDetach();
    }

    protected void resetValidation() {
        for (ValidationFilter listener : listeners.values()) {
            listener.setValid(true);
        }
    }

    @Override
    protected void redraw() {
        super.redraw();
        if (!restartTemplates) {
            restartTemplates = true;
            if (templateController != null) {
                templateController.stop();
            } else {
                comparingController.stop();
            }
        }
    }

    @Override
    protected void onBeforeRender() {
        if (restartTemplates) {
            if (templateController != null) {
                provider = getProvider(getModel());
                if (provider != null) {
                    setVisible(true);
                    templateController.start(provider);
                } else {
                    setVisible(false);
                }
            } else if (comparingController != null) {
                oldProvider = getProvider(compareTo);
                newProvider = getProvider(getModel());
                comparingController.start(oldProvider, newProvider, helper.getField().getTypeDescriptor());
            }
            restartTemplates = false;
        }
        super.onBeforeRender();
    }

    private AbstractProvider<C> getProvider(IModel<P> model) {
        IFieldDescriptor field = helper.getField();
        if (field != null) {
            ITemplateEngine engine = getTemplateEngine();
            if (engine != null) {
                ITypeDescriptor subType = field.getTypeDescriptor();
                AbstractProvider<C> provider = newProvider(field, subType, model);
                if (IEditor.Mode.EDIT == mode && (provider.size() == 0)
                        && (!field.isMultiple() || field.getValidators().contains("required"))) {
                    provider.addNew();
                }
                return provider;
            } else {
                log.warn("No engine found to display new model");
            }
        }
        return null;
    }

    /**
     * Factory method for provider of models that will be used to instantiate templates.
     * This method may be called from the base class constructor.
     *
     * @param descriptor
     * @param type
     * @param parentModel
     * @return
     */
    protected abstract AbstractProvider<C> newProvider(IFieldDescriptor descriptor, ITypeDescriptor type,
            IModel<P> parentModel);

    protected boolean canAddItem() {
        IFieldDescriptor field = getFieldHelper().getField();
        if (IEditor.Mode.EDIT == mode && (field != null)) {
            if (field.isMultiple()) {
                if (getMaxItems() > 0) {
                    return getNumberOfItems() < getMaxItems();
                }
                return true;
            }

            return getNumberOfItems() == 0;
        }

        return false;
    }

    protected boolean canRemoveItem() {
        IFieldDescriptor field = helper.getField();
        if (IEditor.Mode.EDIT != mode || (field == null)) {
            return false;
        }
        if (!field.isMultiple()) {
            return false;
        }
        if (field.getValidators().contains("required") && provider.size() == 1) {
            return false;
        }
        return true;
    }

    protected boolean canReorderItems() {
        IFieldDescriptor field = helper.getField();
        if (IEditor.Mode.EDIT != mode || field == null || !field.isMultiple() || !field.isOrdered()) {
            return false;
        }
        return true;
    }

    public void onAddItem(AjaxRequestTarget target) {
        provider.addNew();
    }

    public void onRemoveItem(C childModel, AjaxRequestTarget target) {
        provider.remove(childModel);
    }

    public void onMoveItemUp(C model, AjaxRequestTarget target) {
        provider.moveUp(model);
        redraw();
    }

    private void addValidationFilter(Object key, ValidationFilter listener) {
        listeners.put(key, listener);
    }

    private void removeValidationFilter(Object key) {
        listeners.remove(key);
    }

    @Override
    protected final void onAddRenderService(final org.apache.wicket.markup.repeater.Item<IRenderService> item,
            IRenderService renderer) {
        super.onAddRenderService(item, renderer);

        switch (mode) {
        case EDIT:
            final FieldItem itemRenderer = templateController.getFieldItem(renderer);
            if (managedValidation && getFieldHelper().getField().isMultiple()) {
                item.setOutputMarkupId(true);
                ValidationFilter listener = new ValidationFilter() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onValidation(IValidationResult result) {
                        boolean valid = itemRenderer.isValid();
                        if (valid != this.isValid()) {
                            AjaxRequestTarget target = AjaxRequestTarget.get();
                            if (target != null) {
                                target.addComponent(item);
                            }
                            setValid(valid);
                        }
                    }
                };
                listener.setValid(itemRenderer.isValid());
                addValidationFilter(item, listener);
                item.add(new CssClassAppender(listener));
            }
            C model = (C) itemRenderer.getModel();
            populateEditItem(item, model);
            break;
        case COMPARE:
            populateCompareItem(item);
            break;
        case VIEW:
            populateViewItem(item);
            break;
        }
    }

    @Override
    protected final void onRemoveRenderService(org.apache.wicket.markup.repeater.Item<IRenderService> item,
            IRenderService renderer) {
        removeValidationFilter(item);
        super.onRemoveRenderService(item, renderer);
    }

    protected void populateEditItem(org.apache.wicket.markup.repeater.Item<IRenderService> item, C model) {
    }

    protected void populateViewItem(org.apache.wicket.markup.repeater.Item<IRenderService> item) {
    }

    protected void populateCompareItem(org.apache.wicket.markup.repeater.Item<IRenderService> item) {
    }

    protected FieldPluginHelper getFieldHelper() {
        return helper;
    }

    protected IModel<String> getCaptionModel() {
        IFieldDescriptor field = getFieldHelper().getField();
        String caption = getPluginConfig().getString("caption");
        String captionKey = field != null ? field.getName() : caption;
        if (captionKey == null) {
            return new Model("undefined");
        }
        if (caption == null && field != null && field.getName().length() >= 1) {
            caption = field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
        }
        return new StringResourceModel(captionKey, this, null, caption);
    }

    protected ITemplateEngine getTemplateEngine() {
        return getPluginContext()
                .getService(getPluginConfig().getString(ITemplateEngine.ENGINE), ITemplateEngine.class);
    }

    protected boolean doesTemplateSupportValidation() {
        ITemplateEngine engine = getTemplateEngine();
        IFieldDescriptor field = helper.getField();
        try {
            IClusterConfig template = engine.getTemplate(field.getTypeDescriptor(), mode);
            return (template.getReferences().contains(IValidationService.VALIDATE_ID));
        } catch (TemplateEngineException e) {
            return false;
        }
    }

    public IClusterControl newTemplate(String id, IEditor.Mode mode, IModel<?> model) throws TemplateEngineException {
        if (mode == null) {
            mode = this.mode;
        }
        ITemplateEngine engine = getTemplateEngine();
        IFieldDescriptor field = helper.getField();
        IClusterConfig template;
        try {
            template = engine.getTemplate(field.getTypeDescriptor(), mode);
        } catch (TemplateEngineException ex) {
            if (IEditor.Mode.COMPARE == mode) {
                template = engine.getTemplate(field.getTypeDescriptor(), IEditor.Mode.VIEW);
            } else {
                throw ex;
            }
        }

        this.parameters.put(ITemplateEngine.ENGINE, getPluginConfig().getString(ITemplateEngine.ENGINE));
        this.parameters.put(RenderService.WICKET_ID, id);
        this.parameters.put(ITemplateEngine.MODE, mode.toString());

        return getPluginContext().newCluster(template, parameters);
    }

    protected Component createNrItemsLabel() {
        if ((IEditor.Mode.EDIT == mode) && (getMaxItems() > 0)) {
            final IModel propertyModel = new StringResourceModel("nrItemsLabel", this, new Model<AbstractFieldPlugin>(this));
            return new Label("nrItemsLabel", propertyModel).setOutputMarkupId(true);
        }
        return new Label("nrItemsLabel").setVisible(false);
    }

    public int getMaxItems() {
        return maxItems;
    }

    public int getNumberOfItems() {
        return provider.size();
    }
}
