/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.addon.workflow;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public abstract class ActionDescription extends Panel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public abstract class ActionDisplay extends Fragment {
        private static final long serialVersionUID = 1L;

        protected ActionDisplay(String id) {
            super(id, id, ActionDescription.this, ActionDescription.this.getDefaultModel());
        }

        abstract protected void initialize();

        void substantiate() {
            initialize();
        }
    }

    public ActionDescription(String id) {
        super(id);
    }

    public ActionDescription(String id, IModel<?> model) {
        super(id, model);
    }

    public final MarkupContainer add(final Fragment component) {
        String id = component.getId();
        if (get(id) != null) {
            return addOrReplace(component);
        } else {
            return super.add(component);
        }
    }

    Map<String, ActionDisplay> actions = new HashMap<String, ActionDisplay>();

    public final MarkupContainer add(final ActionDisplay component) {
        actions.put(component.getId(), component);
        return null;
    }

    public Component getFragment(String id) {
        if (actions.containsKey(id)) {
            return actions.get(id);
        } else {
            return super.get(id);
        }
    }

    public void run() {
        invoke();
    }

    protected abstract void invoke();

}
