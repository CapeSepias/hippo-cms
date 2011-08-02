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
package org.hippoecm.frontend.plugins.cms.admin.plugins;

import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.admin.password.validation.IPasswordValidationService;
import org.hippoecm.frontend.plugins.cms.admin.password.validation.PasswordValidationStatus;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.cms.admin.widgets.PasswordWidget;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangePasswordShortcutPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(ChangePasswordShortcutPlugin.class);

    private static final long serialVersionUID = 1L;

    private String username;
    private JcrNodeModel userModel;

    private String currentPassword;
    private String newPassword;
    private String checkPassword;

    public ChangePasswordShortcutPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        AjaxLink link = new AjaxLink("link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                currentPassword = "";
                newPassword = "";
                checkPassword = "";
                username = ((UserSession) Session.get()).getJcrSession().getUserID();
                IDialogService dialogService = getDialogService();
                if (setUserNode() && canChangePassword()) {
                    dialogService.show(new ChangePasswordShortcutPlugin.Dialog(context, config));
                } else {
                    dialogService.show(new ChangePasswordShortcutPlugin.CannotChangeDialog(context, config));
                }
            }
        };
        add(link);
    }

    /**
     * Set the user node. This really should be looked up by some UserManager. For now
     * use the same code as in the AbstractUserManager
     * @see org.hippoecm.repository.security.user.AbstractUserManager#getUser(String)
     * @param userId
     * @return
     */
    private boolean setUserNode() {
        javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();

        try {
            StringBuilder statement = new StringBuilder();
            statement.append("//element");
            statement.append("(*, ").append(HippoNodeType.NT_USER).append(")");
            statement.append('[').append("fn:name() = ").append("'").append(NodeNameCodec.encode(username, true))
            .append("'").append(']');
            Query q = session.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.XPATH);
            QueryResult result = q.execute();
            NodeIterator nodeIter = result.getNodes();
            if (nodeIter.hasNext()) {
                userModel = new JcrNodeModel(nodeIter.nextNode());
                return true;
            } else {
                return false;
            }
        } catch (RepositoryException e) {
            log.error("Error while trying to get user node.", e);
            return false;
        }
    }

    /**
     * Check if the user can change the password
     * @param user
     * @return
     */
    private boolean canChangePassword() {
        try {
            return !getUser().isExternal();
        } catch (RepositoryException e) {
            log.error("Error while checking primary type", e);
            return false;
        }
    }

    /**
     * Check the password
     * @return
     */
    private boolean checkPassword(char[] password) {
        try {
            return getUser().checkPassword(password);
        } catch (RepositoryException e) {
            log.error("Error while checking user password", e);
        }
        return false;
    }

    /**
     * Set the user password
     * @param password
     * @return
     */
    private boolean setPassword(char[] password) {
        try {
            getUser().savePassword(new String(password));
            return true;
        } catch (RepositoryException e) {
            log.error("Error while setting user password", e);
        }
        return false;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getCheckPassword() {
        return checkPassword;
    }

    public void setCheckPassword(String checkPassword) {
        this.checkPassword = checkPassword;
    }

    private User getUser() throws RepositoryException {
        Node user = userModel.getNode();
        if (user == null) {
            throw new ItemNotFoundException();
        }
        return new User(user);
    }

    public class Dialog extends AbstractDialog {

        private static final long serialVersionUID = 1L;

        private PasswordWidget currentWidget;
        
        private IPasswordValidationService passwordValidationService; 

        public Dialog(final IPluginContext context, final IPluginConfig config) {
            setOkLabel(new StringResourceModel("change-label", ChangePasswordShortcutPlugin.this, null));

            replace(feedback = new FeedbackPanel("feedback"));
            // FIXME: [UH] Why set output markup id?
            feedback.setOutputMarkupId(true);

            currentWidget = new PasswordWidget("current-password", new PropertyModel(ChangePasswordShortcutPlugin.this,
                    "currentPassword"), new StringResourceModel("old-password-label",
                    ChangePasswordShortcutPlugin.this, null));
            currentWidget.setResetPassword(true);
            currentWidget.setOutputMarkupId(true);
            add(currentWidget);
            setFocus(currentWidget);

            final PasswordWidget newWidget = new PasswordWidget("new-password", new PropertyModel(
                    ChangePasswordShortcutPlugin.this, "newPassword"), new StringResourceModel("new-password-label",
                            ChangePasswordShortcutPlugin.this, null));
            newWidget.setResetPassword(false);
            add(newWidget);

            final PasswordWidget checkWidget = new PasswordWidget("check-password", new PropertyModel(
                    ChangePasswordShortcutPlugin.this, "checkPassword"), new StringResourceModel(
                            "new-password-label-again", ChangePasswordShortcutPlugin.this, null));
            checkWidget.setResetPassword(false);
            add(checkWidget);
            
            passwordValidationService = context.getService(IPasswordValidationService.class.getName(), IPasswordValidationService.class);
            
        }

        @Override
        public void onOk() {
            boolean ok = true;
            if (currentPassword == null || currentPassword.length() == 0) {
                error(new StringResourceModel("current-password-missing", ChangePasswordShortcutPlugin.this, null)
                .getString());
                ok = false;
            } else if (!checkPassword(currentPassword.toCharArray())) {
                error(new StringResourceModel("current-password-invalid", ChangePasswordShortcutPlugin.this, null)
                .getString());
                ok = false;
            }

            if (newPassword == null) {
                error(new StringResourceModel("new-password-missing", ChangePasswordShortcutPlugin.this, null)
                .getString());
                ok = false;
            } else {
                if (passwordValidationService != null) {
                    try {
                        List<PasswordValidationStatus> statuses = passwordValidationService.checkPassword(newPassword, getUser());
                        for (PasswordValidationStatus status : statuses) {
                            if (!status.accepted()) {
                                error(status.getMessage());
                                ok = false;
                            }
                        }
                    }
                    catch (RepositoryException e) {
                        log.error("Failure validating password using password validation service", e);
                        ok = false;
                    }
                }
                // fallback on pre 7.7 behavior
                else if (newPassword.length() < 4) {
                    error(new StringResourceModel("new-password-invalid", ChangePasswordShortcutPlugin.this, null).getString());
                    ok = false;
                }
            }

            if (checkPassword == null) {
                error(new StringResourceModel("confirm-password-missing", ChangePasswordShortcutPlugin.this, null)
                .getString());
                ok = false;
            }

            if (newPassword != null && !newPassword.equals(checkPassword)) {
                error(new StringResourceModel("passwords-do-not-match", ChangePasswordShortcutPlugin.this, null)
                .getString());
                ok = false;
            }

            if (ok) {
                if (!setPassword(newPassword.toCharArray())) {
                    error(new StringResourceModel("error-setting-password", ChangePasswordShortcutPlugin.this, null)
                    .getString());
                    log.warn("Setting the password by user '" + username + "' failed.");
                } else {
                    log.info("Password changed by user '" + username + "'.");
                }
            }

            // empty the current password
            currentPassword = "";
            setFocus(currentWidget);
            AjaxRequestTarget target = AjaxRequestTarget.get();
            if (target != null) {
                target.addComponent(currentWidget);
            }
        }

        public IModel getTitle() {
            return new StringResourceModel("change-password-label", ChangePasswordShortcutPlugin.this, null);
        }

        @Override
        public IValueMap getProperties() {
            return SMALL;
        }
    }

    public class CannotChangeDialog extends AbstractDialog {

        private static final long serialVersionUID = 1L;

        public CannotChangeDialog(final IPluginContext context, final IPluginConfig config) {
            info(new StringResourceModel("cannot-change-passoword", ChangePasswordShortcutPlugin.this, null)
            .getString());
            setCancelVisible(false);
        }

        public IModel getTitle() {
            return new StringResourceModel("change-password-label", ChangePasswordShortcutPlugin.this, null);
        }
    }
}
