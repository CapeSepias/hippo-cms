/*
 * Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.ckeditor;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.wicket.behavior.Behavior;
import org.onehippo.ckeditor.HippoPicker;
import org.hippoecm.frontend.plugins.richtext.dialog.images.ImagePickerBehavior;
import org.hippoecm.frontend.plugins.richtext.dialog.links.LinkPickerBehavior;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.ckeditor.Json;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link CKEditorPanelPickerExtension}.
 */
public class CKEditorPanelPickerExtensionTest {

    private LinkPickerBehavior linkPickerBehavior;
    private ImagePickerBehavior imagePickerBehavior;
    private CKEditorPanelPickerExtension extension;

    @Before
    public void setUp() {
        linkPickerBehavior = createMock(LinkPickerBehavior.class);
        imagePickerBehavior = createMock(ImagePickerBehavior.class);
        extension = new CKEditorPanelPickerExtension(linkPickerBehavior, imagePickerBehavior);
    }

    @Test
    public void callbackUrlsAreAddedToCKEditorConfiguration() throws Exception {
        final String linkPickerCallbackUrl = "./linkpicker/callback";
        final String imagePickerCallbackUrl = "./imagepicker/callback";

        expect(linkPickerBehavior.getCallbackUrl()).andReturn(linkPickerCallbackUrl);
        expect(imagePickerBehavior.getCallbackUrl()).andReturn(imagePickerCallbackUrl);

        replay(linkPickerBehavior, imagePickerBehavior);

        final ObjectNode editorConfig = Json.object();
        extension.addConfiguration(editorConfig);

        verify(linkPickerBehavior, imagePickerBehavior);

        assertTrue("CKEditor config has configuration for the hippopicker plugin", editorConfig.has(HippoPicker.CONFIG_KEY));
        JsonNode hippoPickerConfig = editorConfig.get(HippoPicker.CONFIG_KEY);

        assertTrue("hippopicker config is an object", hippoPickerConfig.isObject());

        JsonNode internalLinkPickerConfig = hippoPickerConfig.get(HippoPicker.InternalLink.CONFIG_KEY);
        assertNotNull("hippopicker configuration has configuration for the internal link picker", internalLinkPickerConfig);
        assertTrue("internal link picker config is an object", internalLinkPickerConfig.isObject());
        assertEquals(linkPickerCallbackUrl, internalLinkPickerConfig.get(HippoPicker.InternalLink.CONFIG_CALLBACK_URL).asText());

        JsonNode imagePickerConfig = hippoPickerConfig.get(HippoPicker.Image.CONFIG_KEY);
        assertNotNull("hippopicker configuration has configuration for the image picker", imagePickerConfig);
        assertTrue("image picker config is an object", imagePickerConfig.isObject());
        assertEquals(imagePickerCallbackUrl, imagePickerConfig.get(HippoPicker.Image.CONFIG_CALLBACK_URL).asText());
    }

    @Test
    public void testGetBehaviors() throws Exception {
        List behaviors = new ArrayList();
        for (Behavior behavior : this.extension.getBehaviors()) {
            behaviors.add(behavior);
        }
        assertEquals(2, behaviors.size());
        assertTrue(behaviors.contains(linkPickerBehavior));
        assertTrue(behaviors.contains(imagePickerBehavior));
    }

}
