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
package org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.breadcrumb.BreadCrumbBar;
import org.apache.wicket.extensions.breadcrumb.BreadCrumbLink;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class PanelPluginBreadCrumbBar extends BreadCrumbBar {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: PanelPluginBreadCrumbBar.java 22056 2010-03-09 16:35:24Z bvanhalderen $";

    private static final class BreadCrumbComponent extends Panel {
        private static final long serialVersionUID = 1L;

        public BreadCrumbComponent(String id, int index, IBreadCrumbModel breadCrumbModel,
                final IBreadCrumbParticipant participant, boolean enableLink) {
            super(id);
            add(new Label("sep", "").setRenderBodyOnly(true));
            BreadCrumbLink link = new BreadCrumbLink("link", breadCrumbModel) {
                private static final long serialVersionUID = 1L;

                protected IBreadCrumbParticipant getParticipant(String componentId) {
                    return participant;
                }
            };
            link.setEnabled(enableLink);
            add(link);

            IModel title;
            if (participant instanceof IPanelPluginParticipant) {
                title = ((IPanelPluginParticipant) participant).getTitle(this);
            } else {
                title = new Model(participant.getTitle());
            }
            link.add(new Label("label", title).setRenderBodyOnly(true));
        }
    }

    public PanelPluginBreadCrumbBar(String id) {
        super(id);
    }

    @Override
    protected Component newBreadCrumbComponent(String id, int index, int total,
            IBreadCrumbParticipant breadCrumbParticipant) {
        boolean enableLink = getEnableLinkToCurrent() || (index < (total - 1));
        return new BreadCrumbComponent(id, index, this, breadCrumbParticipant, enableLink);
    }
}
