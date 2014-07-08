/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.console.behavior;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.IAjaxSettings;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.yui.YuiNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathHistoryBehavior extends AbstractYuiAjaxBehavior implements YuiNamespace, IObserver {

    private static final Logger log = LoggerFactory.getLogger(PathHistoryBehavior.class);

    public static final String URL_PARAMETER_PATH = "path";
    public static final String URL_PARAMETER_UUID = "uuid";

    private IModelReference reference;

    public PathHistoryBehavior(IAjaxSettings settings, IModelReference reference) {
        super(settings);

        this.reference = reference;

        setReferenceModelFromRequest();
    }

    private void setReferenceModelFromRequest() {
        final RequestCycle requestCycle = RequestCycle.get();
        String path = requestCycle.getRequest().getParameter(URL_PARAMETER_PATH);
        if (path != null) {
            reference.setModel(new JcrNodeModel(path));
        } else {
            String uuid = requestCycle.getRequest().getParameter(URL_PARAMETER_UUID);
            if (uuid != null) {
                try {
                    reference.setModel(new JcrNodeModel(
                            UserSession.get().getJcrSession().getNodeByIdentifier(uuid)));
                } catch (RepositoryException e) {
                    log.warn("Could not find node by uuid: {}", uuid);
                }
            }
        }
    }

    @Override
    public void addHeaderContribution(final IYuiContext context) {
        context.addModule(this, "pathhistory");
        context.addTemplate(PathHistoryBehavior.class, "js/init.js", getParameters());
    }

    private Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("callbackUrl", getCallbackUrl());
        params.put("callbackFunction", getCallbackFunction());
        return params;
    }

    @Override
    protected void respond(final AjaxRequestTarget target) {
        setReferenceModelFromRequest();
    }

    @Override
    public String getPath() {
        return "js/";
    }

    @Override
    public IObservable getObservable() {
        return reference;
    }

    @Override
    public void onEvent(final Iterator events) {
        JcrNodeModel nodeModel = (JcrNodeModel) reference.getModel();
        String path = nodeModel.getItemModel().getPath();
        AjaxRequestTarget ajax = AjaxRequestTarget.get();
        ajax.addJavascript("YAHOO.hippo.PathHistory.setPath('" + path + "')");
    }
}
