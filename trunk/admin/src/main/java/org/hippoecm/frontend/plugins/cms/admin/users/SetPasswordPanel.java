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
package org.hippoecm.frontend.plugins.cms.admin.users;

import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.crumbs.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.groups.CreateGroupPanel;
import org.hippoecm.frontend.plugins.cms.admin.validators.PasswordStrengthValidator;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetPasswordPanel extends AdminBreadCrumbPanel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SetPasswordPanel.class);

    private final Form form;
    private final IModel model;
    private String password;
    private String checkPassword;
    

    public SetPasswordPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel, final IModel model) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);
        
        this.model = model;
        final User user = (User) model.getObject();

        // add form with markup id setter so it can be updated via ajax
        form = new Form("form");
        form.setOutputMarkupId(true);
        add(form);

        final PasswordTextField passwordField = new PasswordTextField("password", new PropertyModel(this, "password"));
        passwordField.setResetPassword(false);
        passwordField.add(new PasswordStrengthValidator());
        form.add(passwordField);

        final PasswordTextField passwordCheckField = new PasswordTextField("password-check", new PropertyModel(this, "checkPassword"));
        passwordCheckField.setModel(passwordField.getModel());
        passwordCheckField.setRequired(false);
        passwordCheckField.setResetPassword(false);
        form.add(passwordCheckField);

        form.add(new EqualPasswordInputValidator(passwordField, passwordCheckField));

        // add a button that can be used to submit the form via ajax
        form.add(new AjaxButton("set-button", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                String username = user.getUsername();
                try {
                    user.savePassword(password);
                    log.info("User '" + username + "' password set by "
                            + ((UserSession) Session.get()).getJcrSession().getUserID());
                    Session.get().info(getString("user-password-set", model));
                    // one up
                    List<IBreadCrumbParticipant> l = breadCrumbModel.allBreadCrumbParticipants();
                    breadCrumbModel.setActive(l.get(l.size() -2));
                } catch (RepositoryException e) {
                    Session.get().warn(getString("user-save-failed", model));
                    log.error("Unable to set password for user '" + username + "' : ", e);
                }
            }
            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                // make sure the feedback panel is shown
                target.addComponent(SetPasswordPanel.this);
            }
        });

        // add a button that can be used to submit the form via ajax
        form.add(new AjaxButton("cancel-button") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                // one up
                List<IBreadCrumbParticipant> l = breadCrumbModel.allBreadCrumbParticipants();
                breadCrumbModel.setActive(l.get(l.size() -2));
            }
        }.setDefaultFormProcessing(false));
    }

    public IModel getTitle(Component component) {
        return new StringResourceModel("user-set-password-title", component, model);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCheckPassword() {
        return checkPassword;
    }

    public void setCheckPassword(String checkPassword) {
        this.checkPassword = checkPassword;
    }
}
