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
package org.hippoecm.frontend.plugins.login;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class ConcurrentLoginListener implements  HttpSessionListener {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public void sessionCreated(HttpSessionEvent evt) {
    }

    public void sessionDestroyed(HttpSessionEvent evt) {
        HttpSession session = evt.getSession();
        ConcurrentLoginFilter.destroySession(session);
    }
}
