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
package org.hippoecm.editor.expr.model.sandbox;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.onehippo.repository.security.Group;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;

/**
 * Sandboxed User model that can be used in editor expression as <code>user</code> variable.
 */
public class UserModel {

    /**
     * Return the current editing user's name.
     * @return the current editing user's name
     */
    public String getName() {
        try {
            return UserSession.get().getJcrSession().getUserID();
        } catch (Exception e) {
            throw new RuntimeException("Cannot check user ID.", e);
        }
    }

    /**
     * Return true if the current editing user is in any of the given groups by the comma or whitespace separated {@code groupNames}.
     * @param groupNames comma separated group names
     * @return true if the current editing user is in any of the given groups by the comma or whitespace separated {@code groupNames}
     */
    public boolean isInAnyGroup(final String groupNames) {
        if (StringUtils.isBlank(groupNames)) {
            throw new IllegalArgumentException("group names are blank.");
        }

        try {
            final Set<String> groupNameSet = new HashSet<>(Arrays.asList(StringUtils.split(groupNames, ",\t\r\n ")));
            final SecurityService securityService = ((HippoWorkspace) UserSession.get().getJcrSession().getWorkspace()).getSecurityService();
            final String userID = UserSession.get().getJcrSession().getUserID();
            final User user = securityService.getUser(userID);

            if (user == null) {
                throw new RuntimeException("Cannot find the current user from security service: " + userID);
            }

            for (Group group : user.getMemberships()) {
                if (groupNameSet.contains(group.getId())) {
                    return true;
                }
            }

            return false;
        } catch (RepositoryException e) {
            throw new RuntimeException("Cannot check group membership.", e);
        }
    }
}
