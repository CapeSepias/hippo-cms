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
package org.hippoecm.frontend.translation.components;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.hippoecm.frontend.translation.components.document.DocumentTranslationPage;
import org.hippoecm.frontend.translation.components.folder.FolderTranslationPage;

public class WicketApplication extends WebApplication {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    @Override
    public Class<? extends WebPage> getHomePage() {
        return Index.class;
    }

    @Override
    protected void init() {
        super.init();
        mountBookmarkablePage("documents", DocumentTranslationPage.class);
        mountBookmarkablePage("folders", FolderTranslationPage.class);
    }
}
