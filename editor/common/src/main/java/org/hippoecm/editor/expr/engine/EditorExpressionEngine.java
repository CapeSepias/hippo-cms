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

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.hippoecm.editor.expr.model.sandbox.UserModel;
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

    // NOTE: package private only for unit testing.
    static JexlSandbox createDefaultJexlSandbox() {
        // Set blacklist mode.
        JexlSandbox sandbox = new JexlSandbox(false);
        sandbox.white(UserModel.class.getName());
        return sandbox;
    }

    private JexlEngine jexl;

    private EditorExpressionEngine() {
        this(createDefaultJexlSandbox());
    }

    // NOTE: package private only for unit testing.
    EditorExpressionEngine(JexlSandbox sandbox) {
        jexl = new JexlBuilder().sandbox(sandbox).strict(true).create();
    }

    public Object evaluate(final String expr) throws RuntimeException {
        return evaluate(createDefaultEditorJexlContext(), expr);
    }

    public boolean evaluateBoolean(final String expr, final boolean defaultValue) {
        try {
            Boolean ret = (Boolean) evaluate(expr);
            return ret.booleanValue();
        } catch (Exception e) {
            log.error("Failed to evaluate expression: '{}'", expr, e);
        }
        return defaultValue;
    }

    // NOTE: protected only for unit testing at the moment.
    protected Object evaluate(final JexlContext jexlContext, final String expr) throws RuntimeException {
        JexlExpression jexlExpr = jexl.createExpression(expr);
        return jexlExpr.evaluate(jexlContext);
    }

    private JexlContext createDefaultEditorJexlContext() {
        JexlContext jexlContext = new MapContext();
        UserModel userModel = new UserModel();
        jexlContext.set("user", userModel);
        return jexlContext;
    }

}
