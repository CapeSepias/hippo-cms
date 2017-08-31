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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.editor.expr.model.sandbox.UserModel;
import org.junit.Before;
import org.junit.Test;

public class EditorExpressionEngineTest {

    private EditorExpressionEngine exprEngine;

    @Before
    public void setUp() throws Exception {
        JexlSandbox sandbox = EditorExpressionEngine.createDefaultJexlSandbox();
        sandbox.white(MockUserModel.class.getName());
        exprEngine = new EditorExpressionEngine(sandbox);
    }

    @Test
    public void testExpressionWithUser() throws Exception {
        final String userName = "editor";
        final Set<String> groupNames = new HashSet<>(Arrays.asList("author", "editor"));
        UserModel userModel = new MockUserModel(userName, groupNames);
        final JexlContext jexlContext = new MapContext();
        jexlContext.set("user", userModel);

        assertTrue((Boolean) exprEngine.evaluate(jexlContext, "user.name == 'editor'"));
        assertTrue((Boolean) exprEngine.evaluate(jexlContext, "user.isInAnyGroup('author,other')"));
        assertTrue((Boolean) exprEngine.evaluate(jexlContext, "user.isInAnyGroup('other,editor')"));
        assertFalse((Boolean) exprEngine.evaluate(jexlContext, "user.isInAnyGroup('other,admin')"));
    }

    @Test
    public void testBlacklistMode() throws Exception {
        try {
            exprEngine.evaluate("new(\"java.lang.Double\", 10) == null");
            fail("Any ad hoc object cannot be created in sandbox expression engine.");
        } catch (JexlException expected) {
        }
    }

    public static class MockUserModel extends UserModel {

        private final String userName;
        private final Set<String> groupNames;

        public MockUserModel(final String userName, final Set<String> groupNames) {
            this.userName = userName;
            this.groupNames = groupNames;
        }

        public String getName() {
            return userName;
        }

        public boolean isInAnyGroup(final String groupNamesCSV) {
            final Set<String> groupNameSet = new HashSet<>(Arrays.asList(StringUtils.split(groupNamesCSV, ",\t\r\n ")));

            for (String groupName : groupNames) {
                if (groupNameSet.contains(groupName)) {
                    return true;
                }
            }

            return false;
        }
    }
}
