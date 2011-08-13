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

package org.hippoecm.frontend.plugins.yui.header.templates;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;

public class FinalTextTemplate implements IHeaderContributor {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private TextTemplateHeaderContributor headerContributor;

    public FinalTextTemplate(PackagedTextTemplate template, Map<String, Object> parameters) {
        headerContributor = TextTemplateHeaderContributor.forJavaScript(template, new ReadOnlyModel(parameters));
    }

    public FinalTextTemplate(Class<?> clazz, String filename, Map<String, Object> parameters) {
        this(new PackagedTextTemplate(clazz, filename), parameters);
    }

    public void renderHead(IHeaderResponse response) {
        headerContributor.renderHead(response);
    }

    private static class ReadOnlyModel extends AbstractReadOnlyModel {
        private static final long serialVersionUID = 1L;
        private Map<String, Object> values  = new HashMap<String, Object>();

        public ReadOnlyModel(Map<String, Object> values) {
            this.values.putAll(values);
        }

        @Override
        public Object getObject() {
            return values;
        }
    }
}
