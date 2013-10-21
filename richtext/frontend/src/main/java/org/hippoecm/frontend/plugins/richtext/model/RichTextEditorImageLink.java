/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.model;

import java.util.Map;

import org.apache.wicket.model.IDetachable;

public abstract class RichTextEditorImageLink extends RichTextEditorDocumentLink {
    private static final long serialVersionUID = 1L;

    public static final String URL = "f_url";
    public static final String FACET_SELECT = "f_facetselect";
    public static final String ALT = "f_alt";
    public static final String ALIGN = "f_align";
    public static final String WIDTH = "f_width";
    public static final String HEIGHT = "f_height";
    public static final String TYPE = "f_type";

    public RichTextEditorImageLink(Map<String, String> values, IDetachable targetId) {
        super(values, targetId);
    }

    public void setUrl(String url) {
        put(URL, url);
    }

    public String getUrl() {
        return get(URL);
    }

    public void setFacetSelectPath(String facetSelectPath) {
        put(FACET_SELECT, facetSelectPath);
    }

    public String getFacetSelectPath() {
        return get(FACET_SELECT);
    }

    public void setType(String type){
        put(TYPE, type);
    }

    public String getType(){
        return (String) get(TYPE);
    }

    @Override
    public void setLinkTarget(IDetachable model) {
        super.setLinkTarget(model);
        if(model != null && !model.equals(getInitialModel())) {
            put(WIDTH, "");
            put(HEIGHT, "");
        }
    }
}