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
package org.hippoecm.frontend.plugins.gallery.imageutil;

import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;

import org.hippoecm.frontend.plugins.gallery.model.GalleryException;

public abstract class AbstractImageOperation {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public AbstractImageOperation() {
        // do nothing
    }

    public void execute(InputStream data, String mimeType) throws GalleryException {
        ImageReader reader = null;
        ImageWriter writer = null;
        try {
            reader = ImageUtils.getImageReader(mimeType);
            if (reader == null) {
                throw new GalleryException("Unsupported MIME type for reading: " + mimeType);
            }
            writer = ImageUtils.getImageWriter(mimeType);
            if (writer == null) {
                throw new GalleryException("Unsupported MIME type for writing: " + mimeType);
            }
            execute(data, reader, writer);
        } catch (IOException e) {
            throw new GalleryException("Could not resize image", e);
        } finally {
            // the != null checks are unnecessary, but otherwise Sonar will report critical issues
            if (reader != null) {
                reader.dispose();
            }
            if (writer != null) {
                writer.dispose();
            }
        }
    }

    public abstract void execute(InputStream data, ImageReader reader, ImageWriter writer) throws IOException;

}
