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
package org.hippoecm.frontend.plugin.config.impl;

import java.util.AbstractList;
import java.util.List;

import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class InheritingPluginConfig extends AbstractPluginDecorator {
    final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private IPluginConfig fallback;

    public InheritingPluginConfig(IPluginConfig upstream, IPluginConfig fallback) {
        super(upstream);
        this.fallback = fallback;
    }

    @Override
    protected Object decorate(Object object) {
        if (object instanceof String) {
            String value = (String) object;
            if (value.startsWith("${") && value.endsWith("}")) {
                return fallback.get(value.substring(2, value.length() - 1));
            } else {
                return value;
            }
        } else if (object instanceof IPluginConfig) {
            return new InheritingPluginConfig((IPluginConfig) object, fallback);
        } else if (object instanceof List<?>) {
            final List<?> list = (List<?>) object;
            return new AbstractList<Object>() {
                @Override
                public Object get(int index) {
                    return decorate(list.get(index));
                }

                @Override
                public int size() {
                    return list.size();
                }
            };
        }
        return object;
    }
}
