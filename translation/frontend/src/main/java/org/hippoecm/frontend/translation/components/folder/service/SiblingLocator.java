/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.translation.components.folder.service;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.translation.components.folder.model.T9Node;
import org.hippoecm.frontend.translation.components.folder.model.T9Tree;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wicketstuff.js.ext.ExtObservable;
import org.wicketstuff.js.ext.data.AbstractExtBehavior;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtJsonRequestTarget;
import org.wicketstuff.js.ext.util.ExtPropertyConverter;

@ExtClass("Hippo.Translation.SiblingLocator")
public final class SiblingLocator extends ExtObservable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public static final String T9ID_ID = "t9id";

    private final AbstractAjaxBehavior behavior;
    private final IModel<T9Tree> data;

    public SiblingLocator(IModel<T9Tree> data) {
        this.data = data;
        this.behavior = new AbstractExtBehavior() {
            private static final long serialVersionUID = 1L;
            
            @Override
            public void onRequest() {
                final RequestCycle requestCycle = RequestCycle.get();
                String t9Id = requestCycle.getRequest().getParameter(T9ID_ID);
                if (t9Id != null) {
                    try {
                        JSONObject siblingsAsJson = getSiblingsAsJSON(t9Id);
                        requestCycle.setRequestTarget(new ExtJsonRequestTarget(siblingsAsJson));
                    } catch (JSONException e) {
                        throw new WicketRuntimeException("Could not build map of siblings");
                    }
                } else {
                    throw new WicketRuntimeException("No node id provided");
                }
            }
        };
    }

    @Override
    public void bind(Component component) {
        super.bind(component);
        component.add(behavior);
    }

    public JSONObject getSiblingsAsJSON(String t9id) throws JSONException {
        Map<String, List<T9Node>> siblings = getSiblings(t9id);
        JSONObject asJson = new JSONObject();
        for (Map.Entry<String, List<T9Node>> entry : siblings.entrySet()) {
            JSONArray path = new JSONArray();
            for (T9Node node : entry.getValue()) {
                JSONObject properties = new JSONObject();
                ExtPropertyConverter.addProperties(node, node.getClass(), properties);
                path.put(properties);
            }
            asJson.put(entry.getKey(), path);
        }
        return asJson;
    }

    public Map<String, List<T9Node>> getSiblings(String t9id) {
        Map<String, List<T9Node>> result = new TreeMap<String, List<T9Node>>();
        T9Tree tree = data.getObject();
        List<T9Node> children = tree.getSiblings(t9id);
        if (children != null) {
            for (T9Node child : children) {
                String language = child.getLang();
                result.put(language, tree.getPath(child.getId()));
            }
        }
        return result;
    }

    @Override
    protected JSONObject getProperties() throws JSONException {
        JSONObject properties = super.getProperties();
        properties.put("dataUrl", behavior.getCallbackUrl());
        return properties;
    }

}
