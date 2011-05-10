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

import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

public abstract class PanelPluginBreadCrumbPanel extends BreadCrumbPanel implements IPanelPluginParticipant {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id: PanelPluginBreadCrumbPanel.java 19365 2009-08-24 16:47:23Z bvdschans $";
    private static final long serialVersionUID = 1L;

    private FeedbackPanel feedback;

    public PanelPluginBreadCrumbPanel(final String id, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);
        // add feedback panel to show errors
        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);
    }

    public String getTitle() {
        return (String) getTitle(this).getObject();
    }

    /**
     * get the feedback panel, might be null
     * @return the feedback panel or null if not set
     */
    public FeedbackPanel getFeedbackPanel() {
        return feedback;
    }

}
