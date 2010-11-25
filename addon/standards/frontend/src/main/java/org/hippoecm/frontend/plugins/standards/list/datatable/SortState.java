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
package org.hippoecm.frontend.plugins.standards.list.datatable;

import org.apache.wicket.IClusterable;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;

public class SortState implements ISortState, IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    private String property;
    private int sortState = ISortState.NONE;

    public void setPropertySortOrder(String property, int sortState) {
        this.property = property;
        this.sortState = sortState;
    }

    public int getPropertySortOrder(String param) {
        if (param == null || property == null || !param.equals(this.property)) {
            return NONE;
        } else {
            return sortState;
        }
    }

    public boolean isAscending() {
        return sortState == ISortState.ASCENDING;
    }

    public boolean isDescending() {
        return sortState == ISortState.DESCENDING;
    }

    public boolean isSorted() {
        return sortState != ISortState.NONE;
    }

    public String getProperty() {
        return property;
    }

}
