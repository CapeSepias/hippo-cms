/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.editor;

import java.util.List;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.model.properties.StringConverter;
import org.hippoecm.frontend.widgets.BooleanFieldWidget;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PropertyValueEditor extends DataView {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PropertyValueEditor.class);

    protected static final int TEXT_AREA_MAX_COLUMNS = 100;

    private JcrPropertyModel propertyModel;

    PropertyValueEditor(String id, JcrPropertyModel dataProvider) {
        super(id, dataProvider);
        this.propertyModel = dataProvider;
        setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
    }

    @Override
    protected void populateItem(Item item) {
        try {
            final JcrPropertyValueModel valueModel = (JcrPropertyValueModel) item.getModel();
            Component valueEditor = createValueEditor(valueModel);

            item.add(valueEditor);

            final AjaxLink removeLink = new AjaxLink("remove") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    try {
                        Property prop = propertyModel.getProperty();
                        Value[] values = prop.getValues();
                        values = (Value[]) ArrayUtils.remove(values, valueModel.getIndex());
                        prop.getParent().setProperty(prop.getName(), values, prop.getType());
                    } catch (RepositoryException e) {
                        log.error(e.getMessage());
                    }

                    NodeEditor editor = findParent(NodeEditor.class);

                    if (editor != null) {
                        target.add(editor);
                    }
                }
            };

            removeLink.add(new Image("remove-icon", new PackageResourceReference(PropertiesEditor.class, "edit-delete-16.png")));
            removeLink.add(new AttributeModifier("title", getString("property.value.remove")));

            PropertyDefinition definition = propertyModel.getProperty().getDefinition();
            removeLink.setVisible(definition.isMultiple() && !definition.isProtected());

            item.add(removeLink);
        }
        catch (RepositoryException e) {
            log.error(e.getMessage());
            item.add(new Label("value", e.getClass().getName() + ":" + e.getMessage()));
            item.add(new Label("remove", ""));
        }
    }

    /**
     * Finds {@link EditorPlugin} containing this from ancestor components.
     * @return
     */
    protected EditorPlugin getEditorPlugin() {
        EditorPlugin plugin = findParent(EditorPlugin.class);
        return plugin;
    }

    /**
     * Creates property value editing component.
     * @param valueModel
     * @return
     * @throws RepositoryException
     */
    protected Component createValueEditor(final JcrPropertyValueModel valueModel) throws RepositoryException {
        List<ValueEditorFactory> factoryList = getEditorPlugin().getPluginContext().getServices(ValueEditorFactory.SERVICE_ID, ValueEditorFactory.class);

        for (ValueEditorFactory factory : factoryList) {
            if (factory.canEdit(valueModel)) {
                return factory.createEditor("value", valueModel);
            }
        }

        if (propertyModel.getProperty().getType() == PropertyType.BINARY) {
            return new BinaryEditor("value", propertyModel);
        }
        else if (propertyModel.getProperty().getDefinition().isProtected()) {
            return new Label("value", valueModel);
        }
        else if (propertyModel.getProperty().getType() == PropertyType.BOOLEAN) {
            return new BooleanFieldWidget("value", valueModel);
        }
        else {
            StringConverter stringModel = new StringConverter(valueModel);
            String asString = stringModel.getObject();

            if (asString.contains("\n")) {
                TextAreaWidget editor = new TextAreaWidget("value", stringModel);
                String[] lines = StringUtils.splitByWholeSeparator(asString, "\n");
                int rowCount = lines.length;
                int columnCount = 1;

                for (String line : lines) {
                    int length = line.length();

                    if (length > columnCount) {
                        if (length > TEXT_AREA_MAX_COLUMNS) {
                            columnCount = TEXT_AREA_MAX_COLUMNS;
                            rowCount += (length / TEXT_AREA_MAX_COLUMNS) + 1;
                        } else {
                            columnCount = length;
                        }
                    }
                }

                editor.setCols(String.valueOf(columnCount + 1));
                editor.setRows(String.valueOf(rowCount + 1));
                return editor;
            }
            else if (asString.length() > TEXT_AREA_MAX_COLUMNS) {
                TextAreaWidget editor = new TextAreaWidget("value", stringModel);
                editor.setCols(String.valueOf(TEXT_AREA_MAX_COLUMNS));
                editor.setRows(String.valueOf((asString.length() / 80)));
                return editor;
            }
            else {
                TextAreaWidget editor = new TextAreaWidget("value", stringModel);
                editor.setCols(String.valueOf(TEXT_AREA_MAX_COLUMNS));
                editor.setRows("1");
                return editor;
            }
        }
    }
}
