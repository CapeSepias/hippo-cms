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
package org.hippoecm.frontend.widgets;

import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextAreaWidget extends AjaxUpdatingWidget<String> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(TextAreaWidget.class);
    
    private static final long serialVersionUID = 1L;
    private String rows;
    @Deprecated
    private String cols;

    private TextArea<String> textArea;

    public TextAreaWidget(String id, IModel<String> model) {
        super(id, model);
        textArea = new TextArea<String>("widget", model) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                String rows = getRows();
                if (rows != null) {
                    try {
                        double rowCount = Double.parseDouble(rows);
                        tag.put("style", "height: " + (rowCount * 1.2) + "em;");
                    } catch (NumberFormatException e) {
                        log.warn("Cannot set height of textarea. Expected 'rows' to be a double, but got: '" + rows + "'");
                    }
                    tag.put("rows", getRows());
                }
                super.onComponentTag(tag);
            }
        };
        addFormField(textArea);
    }

    public void setRows(String rows) {
        this.rows = rows;
    }

    public String getRows() {
        return rows;
    }

    @Deprecated
    public void setCols(String cols) {
        this.cols = cols;
    }

    @Deprecated
    public String getCols() {
        return cols;
    }
    
    public void addBehaviourOnFormComponent(IBehavior behavior){
        textArea.add(behavior);
    }

}
