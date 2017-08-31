/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.editor.expr.engine;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.hippoecm.editor.expr.model.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorExpressionEngine {

    private static Logger log = LoggerFactory.getLogger(EditorExpressionEngine.class);

    // following 'Initialization-on-demand holder idiom' pattern
    private static class LazyHolder {
        static final EditorExpressionEngine INSTANCE = new EditorExpressionEngine();
    }

    public static EditorExpressionEngine getInstance() {
        return LazyHolder.INSTANCE;
    }

    private JexlEngine jexl;

    private EditorExpressionEngine() {
        jexl = new JexlBuilder().create();
    }

    public Map<String, Object> createDefaultEditorContext() {
        Map<String, Object> contextMap = new HashMap<>();
        UserModel userModel = new UserModel();
        contextMap.put("user", userModel);
        return contextMap;
    }

    public Object evaluate(final Map<String, Object> context, final String expr) throws RuntimeException {
        JexlExpression jexlExpr = jexl.createExpression(expr);
        JexlContext jexlContext = new MapContext(context);
        return jexlExpr.evaluate(jexlContext);
    }

    public boolean evaluateBoolean(final Map<String, Object> context, final String expr, final boolean defaultValue) {
        try {
            Boolean ret = (Boolean) evaluate(context, expr);
            return ret.booleanValue();
        } catch (Exception e) {
            log.error("Failed to evaluate expression: '{}'", expr, e);
        }
        return defaultValue;
    }
}
