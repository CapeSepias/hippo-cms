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
package org.hippoecm.upgrade;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

class TextOutput extends AbstractOutput {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public TextOutput(PrintStream stream) {
        super(stream);
    }

    @Override
    void render(Item item) {
        render(item, "");
    }

    void render(Item item, String prefix) {
        if (item.containsKey("jcr:primaryType")) {
            out.println(prefix + "+ " + item.name + " [" + item.get("jcr:primaryType")[0] + "]");
        } else {
            out.println(prefix + "+ " + item.name);
        }
        for (Map.Entry<String, Value[]> entry : item.entrySet()) {
            if ("jcr:primaryType".equals(entry.getKey())) {
                continue;
            }
            out.println(prefix + CanonicalSv.INDENT + "- " + entry.getKey() + ": " + renderValue(entry.getValue()));
        }
        for (Map.Entry<String, Set<Item>> entry : item.children.entrySet()) {
            for (Item child : entry.getValue()) {
                render(child, prefix + CanonicalSv.INDENT);
            }
        }
    }

    String renderValue(Value[] values) {
        StringBuilder sb = new StringBuilder();
        boolean first = false;
        for (Value value : values) {
            if (first) {
                first = false;
            } else {
                sb.append(' ');
            }
            sb.append(value);
        }
        return sb.toString();
    }
}