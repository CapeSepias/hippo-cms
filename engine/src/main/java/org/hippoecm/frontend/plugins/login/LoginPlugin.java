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
package org.hippoecm.frontend.plugins.login;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.WebResponse;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.InvalidLoginPage;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.model.UserCredentials;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(LoginPlugin.class);

    public static final String DEFAULT_LOCALE = "en";

    // Sorted by alphabetical order of the language name (see i18n properties), for a more user-friendly form
    public final static String[] LOCALES = { "en", "fr", "nl", "it" };

    private static final long serialVersionUID = 1L;

    private static final String LOCALE_COOKIE = "loc";

    protected String username;
    protected String password;

    public LoginPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        add(createSignInForm("signInForm"));
        add(new Label("pinger"));
    }

    protected SignInForm createSignInForm(String id) {
        return new SignInForm(id);
    }

    @Override
    public void renderHead(HtmlHeaderContainer container) {
        super.renderHead(container);
        container.getHeaderResponse().renderOnLoadJavascript("document.forms.signInForm.username.focus();");
    }

    protected class SignInForm extends Form implements CallbackHandler {
        private static final long serialVersionUID = 1L;

        protected final DropDownChoice locale;

        public String selectedLocale;
        protected final RequiredTextField usernameTextField;
        protected final PasswordTextField passwordTextField;
        private Label userLabel;
        private Map parameters;

        public SignInForm(final String id) {
            super(id);

            parameters = RequestCycle.get().getRequest().getParameterMap();

            String[] localeArray = getPluginConfig().getStringArray("locales");
            if (localeArray == null) {
                localeArray = LOCALES;
            }
            final Set<String> locales = new HashSet<String>(Arrays.asList(localeArray));

            // by default, use the user's browser settings for the locale
            selectedLocale = DEFAULT_LOCALE;
            if (locales.contains(getSession().getLocale().getLanguage())) {
                selectedLocale = getSession().getLocale().getLanguage();
            }

            // check if user has previously selected a locale
            Cookie[] cookies = ((WebRequest) RequestCycle.get().getRequest()).getHttpServletRequest().getCookies();
            if (cookies != null) {
                for (int i = 0; i < cookies.length; i++) {
                    if (LOCALE_COOKIE.equals(cookies[i].getName())) {
                        if (locales.contains(cookies[i].getValue())) {
                            selectedLocale = cookies[i].getValue();
                            getSession().setLocale(new Locale(selectedLocale));
                        }
                    }
                }
            }

            add(usernameTextField = new RequiredTextField("username", new PropertyModel<String>(LoginPlugin.this,
                    "username")));
            add(passwordTextField = new PasswordTextField("password", new PropertyModel<String>(LoginPlugin.this,
                    "password")));
            add(locale = new DropDownChoice("locale", new PropertyModel<String>(this, "selectedLocale") {
                    @Override
                    public void setObject(final String object) {
                        super.setObject(locales.contains(object) ? object : DEFAULT_LOCALE);
                    }
                }, Arrays.asList(localeArray),
                // Display the language name from i18n properties
                new IChoiceRenderer<String>() {
                    public String getDisplayValue(String object) {
                        return new StringResourceModel(object, LoginPlugin.this, null).getString();
                    }
                    public String getIdValue(String object, int index) {
                        return object;
                    }
                }));

            passwordTextField.setResetPassword(false);

            locale.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                protected void onUpdate(AjaxRequestTarget target) {
                    //immediately set the locale when the user changes it
                    Cookie localeCookie = new Cookie(LOCALE_COOKIE, selectedLocale);
                    localeCookie.setMaxAge(365 * 24 * 3600); // expire one year from now
                    ((WebResponse) RequestCycle.get().getResponse()).addCookie(localeCookie);
                    getSession().setLocale(new Locale(selectedLocale));
                    setResponsePage(this.getFormComponent().getPage());
                }
            });

            usernameTextField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                protected void onUpdate(AjaxRequestTarget target) {
                    String username = this.getComponent().getDefaultModelObjectAsString();
                    HttpSession session = ((WebRequest) SignInForm.this.getRequest()).getHttpServletRequest()
                            .getSession(true);
                    if (ConcurrentLoginFilter.isConcurrentSession(session, username)) {
                        userLabel.setDefaultModel(new StringResourceModel("alreadylogin", LoginPlugin.this, null,
                                new Object[] { username }));
                    } else {
                        userLabel.setDefaultModel(new Model(""));
                    }
                    target.addComponent(userLabel);
                    LoginPlugin.this.username = username;
                }
            });

            passwordTextField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                protected void onUpdate(AjaxRequestTarget target) {
                    LoginPlugin.this.password = password;
                }
            });

            add(new FeedbackPanel("feedback").setEscapeModelStrings(false));
            add(userLabel = new Label("infouserlogin", ""));
            userLabel.setOutputMarkupId(true);
            Button submit = new Button("submit", new ResourceModel("submit-label"));
            add(submit);
        }

        @Override
        public void onDetach() {
            WebRequest webRequest = ((WebRequestCycle) RequestCycle.get()).getWebRequest();
            if (!webRequest.getHttpServletRequest().getMethod().equals("POST") && !webRequest.isAjax()) {
                ((UserSession) getSession()).releaseJcrSession();
            }
            super.onDetach();
        }

        @Override
        public void onSubmit() {
            PluginUserSession userSession = (PluginUserSession) getSession();
            String username = usernameTextField.getDefaultModelObjectAsString();
            HttpSession session = ((WebRequest) SignInForm.this.getRequest()).getHttpServletRequest().getSession(true);
            boolean success = userSession.login(new UserCredentials(this));
            ConcurrentLoginFilter.validateSession(session, username, false);
            userSession.setLocale(new Locale(selectedLocale));
            redirect(success);
        }

        protected void redirect(boolean success) {
            if (success == false) {
                Main main = (Main) Application.get();
                main.resetConnection();
                throw new RestartResponseException(InvalidLoginPage.class);
            }
            if (parameters != null) {
                visitFormComponents(new FormComponent.IVisitor() {
                    public Object formComponent(IFormVisitorParticipant formComponent) {
                        if (formComponent instanceof FormComponent) {
                            parameters.remove(((FormComponent<?>) formComponent).getInputName());
                        }

                        return Component.IVisitor.CONTINUE_TRAVERSAL;
                    }
                });
                parameters.remove(getHiddenFieldId());
                setResponsePage(Home.class, new PageParameters(parameters));
            } else {
                setResponsePage(Home.class);
            }
        }

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback callback : callbacks) {
                if (callback instanceof NameCallback) {
                    NameCallback nameCallback = (NameCallback) callback;
                    nameCallback.setName(username);
                } else if (callback instanceof PasswordCallback) {
                    PasswordCallback passwordCallback = (PasswordCallback) callback;
                    passwordCallback.setPassword(password.toCharArray());
                }
            }
        }
    }
}
