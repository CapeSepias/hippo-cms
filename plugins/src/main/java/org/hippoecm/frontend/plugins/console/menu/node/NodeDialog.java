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
package org.hippoecm.frontend.plugins.console.menu.node;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.DefaultCssAutocompleteTextField;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeDialog extends AbstractDialog<Node> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(NodeDialog.class);

    private String name;
    private String type;

    private final Map<String, Collection<String>> namesToTypes = new HashMap<String, Collection<String>>();
    private final Map<String, Collection<String>> typesToNames = new HashMap<String, Collection<String>>();
    
    private final IModelReference<Node> modelReference;

    public NodeDialog(IModelReference<Node> modelReference) {
        this.modelReference = modelReference;
        JcrNodeModel nodeModel = (JcrNodeModel) modelReference.getModel();
        setModel(nodeModel);

        // list defined child node names and types for automatic completion
        Node node = nodeModel.getNode();
        try {
            NodeType pnt = node.getPrimaryNodeType();
            for (NodeDefinition nd : pnt.getChildNodeDefinitions()) {
                for (NodeType nt : nd.getRequiredPrimaryTypes()) {
                    if (!nt.isAbstract()) {
                        Collection<String> types = namesToTypes.get(nd.getName());
                        if (types == null) {
                            types = new HashSet<String>(5);
                            namesToTypes.put(nd.getName(), types);
                        }
                        types.add(nt.getName());
                        Collection<String> names = typesToNames.get(nt.getName());
                        if (names == null) {
                            names = new HashSet<String>(5);
                            typesToNames.put(nt.getName(), names);
                        }
                        names.add(nd.getName());
                    }
                }
            }
            for (NodeType nt : node.getMixinNodeTypes()) {
                for (NodeDefinition nd : nt.getChildNodeDefinitions()) {
                    for (NodeType cnt : nd.getRequiredPrimaryTypes()) {
                        if (!cnt.isAbstract()) {
                            Collection<String> types = namesToTypes.get(nd.getName());
                            if (types == null) {
                                types = new HashSet<String>(5);
                                namesToTypes.put(nd.getName(), types);
                            }
                            types.add(cnt.getName());
                            Collection<String> names = typesToNames.get(cnt.getName());
                            if (names == null) {
                                names = new HashSet<String>(5);
                                typesToNames.put(cnt.getName(), names);
                            }
                            names.add(nd.getName());
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            log.warn("Unable to populate autocomplete list for child node names", e);
        }

        AutoCompleteSettings settings = new AutoCompleteSettings();
        settings.setAdjustInputWidth(false);
        settings.setUseSmartPositioning(true);
        settings.setShowCompleteListOnFocusGain(true);

        final Model<String> typeModel = new Model<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                if (name != null && namesToTypes.containsKey(name)) {
                    Collection<String> types = namesToTypes.get(name);
                    if (types.size() == 1) {
                        type = types.iterator().next();
                    }
                }
                else if (namesToTypes.size() == 1) {
                    Collection<String> types = namesToTypes.values().iterator().next();
                    if (types.size() == 1) {
                        type = types.iterator().next();
                    }
                }
                return type;
            }

            @Override
            public void setObject(String s) {
                type = s;
            }
        };
        final AutoCompleteTextField<String> typeField = new AutoCompleteTextField<String>("type", typeModel, settings) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Iterator<String> getChoices(String input) {
                Collection<String> result = new HashSet<String>();
                if (name != null && !name.isEmpty()) {
                    if (namesToTypes.get(name) != null) {
                        result.addAll(namesToTypes.get(name));
                    }
                    if (namesToTypes.get("*") != null) {
                        result.addAll(namesToTypes.get("*"));
                    }
                }
                else {
                    for (Collection<String> types : namesToTypes.values()) {
                        result.addAll(types);
                    }
                }
                Iterator<String> resultIter = result.iterator();
                while (resultIter.hasNext()) {
                    if (!resultIter.next().startsWith(input)) {
                        resultIter.remove();
                    }
                }
                return result.iterator();
            }
        };
        typeField.add(CSSPackageResource.getHeaderContribution(DefaultCssAutocompleteTextField.class,
                "DefaultCssAutocompleteTextField.css"));
        add(typeField);

        final Model<String> nameModel = new Model<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                if (type != null && typesToNames.containsKey(type)) {
                    Collection<String> names = typesToNames.get(type);
                    if (names.size() == 1) {
                        String _name = names.iterator().next();
                        if (name == null || !_name.equals("*")) {
                            name = _name;
                        }
                    }
                }
                else if (typesToNames.size() == 1) {
                    Collection<String> names = typesToNames.values().iterator().next();
                    if (names.size() == 1) {
                        name = names.iterator().next();
                    }
                }
                return name;
            }

            @Override
            public void setObject(String s) {
                name = s;
            }
        };
        final AutoCompleteTextField<String> nameField = new AutoCompleteTextField<String>("name", nameModel, settings) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Iterator<String> getChoices(String input) {
                Collection<String> result = new HashSet<String>();
                for (String nodeName : namesToTypes.keySet()) {
                    if (!nodeName.equals("*") && nodeName.startsWith(input)) {
                        result.add(nodeName);
                    }
                }
                return result.iterator();
            }
        };
        nameField.add(CSSPackageResource.getHeaderContribution(DefaultCssAutocompleteTextField.class,
                "DefaultCssAutocompleteTextField.css"));
        nameField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.addComponent(typeField);
            }
        });
        typeField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.addComponent(nameField);
            }
        });
        add(setFocus(nameField));

    }

    @Override
    public void onOk() {
        try {
            JcrNodeModel nodeModel = (JcrNodeModel) getModel();
            Node node = nodeModel.getNode().addNode(name, type);

            modelReference.setModel(new JcrNodeModel(node));
        } catch (RepositoryException ex) {
            error(ex.toString());
        }
    }

    public IModel<String> getTitle() {
        return new Model<String>("Add a new Node");
    }

    @Override
    public IValueMap getProperties() {
        return SMALL;
    }
}
