/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.editor.expr.model;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.onehippo.repository.security.Group;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;

public class UserModel {

    public String getName() {
        try {
            return UserSession.get().getJcrSession().getUserID();
        } catch (Exception e) {
            throw new RuntimeException("Cannot check user ID.", e);
        }
    }

    public boolean isInGroup(String groupName) {
        try {
            SecurityService securityService = ((HippoWorkspace) UserSession.get().getJcrSession().getWorkspace()).getSecurityService();
            final String userID = UserSession.get().getJcrSession().getUserID();
            User user = securityService.getUser(userID);

            if (user == null) {
                throw new RuntimeException("Cannot find the current user from security service: " + userID);
            }

            for (Group group : user.getMemberships()) {
                if (StringUtils.equals(groupName, group.getId())) {
                    return true;
                }
            }

            return false;
        } catch (RepositoryException e) {
            throw new RuntimeException("Cannot check group membership.", e);
        }
    }
}
