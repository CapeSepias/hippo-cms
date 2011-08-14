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

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageInputStream;

import org.apache.wicket.util.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Creates a scaled version of an image. The given scaling parameters define a bounding box with
 * a certain width and height. Images that do not fit in this box (i.e. are too large) are always scaled
 * down such that they do fit. If the aspect ratio of the original image differs from that of the
 * bounding box, either the width or the height of scaled image will be less than that of the box.</p>
 * <p>
 * Smaller images are scaled up in the same way as large images are scaled down, but only if upscaling is
 * true. When upscaling is false and the image is smaller than the bounding box, the scaled image
 * will be equal to the original.</p>
 * <p>
 * If the width or height of the scaling parameters is 0 or less, that side of the bounding box does not
 * exist (i.e. is unbounded). If both sides of the bounding box are unbounded, the scaled image will be
 * equal to the original.</p>
 */
public class ScaleImageOperation extends AbstractImageOperation {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(ScaleImageOperation.class);

    private final int width;
    private final int height;
    private final boolean upscaling;
    private InputStream scaledData;
    private int scaledWidth;
    private int scaledHeight;

    /**
     * Creates a image scaling operation, defined by the bounding box of a certain width and height.
     *
     * @param width the width of the bounding box in pixels
     * @param height the height of the bounding box in pixels
     * @param upscaling whether to enlarge images that are smaller than the bounding box
     */
    public ScaleImageOperation(int width, int height, boolean upscaling) {
        this.width = width;
        this.height = height;
        this.upscaling = upscaling;
    }

    /**
     * Creates a scaled version of an image. The given scaling parameters define a bounding box with
     * a certain width and height. Images that do not fit in this box (i.e. are too large) are always scaled
     * down such that they do fit. If the aspect ratio of the original image differs from that of the
     * bounding box, either the width or the height of scaled image will be less than that of the box.</p>
     * <p>
     * Smaller images are scaled up in the same way as large images are scaled down, but only if upscaling is
     * true. When upscaling is false and the image is smaller than the bounding box, the scaled image
     * will be equal to the original.</p>
     * <p>
     * If the width or height of the scaling parameters is 0 or less, that side of the bounding box does not
     * exist (i.e. is unbounded). If both sides of the bounding box are unbounded, the scaled image will be
     * equal to the original.</p>
     *
     * @param data the original image data
     * @param reader reader for the image data
     * @param writer writer for the image data
     */
    public void execute(InputStream data, ImageReader reader, ImageWriter writer) throws IOException {
        MemoryCacheImageInputStream mciis = new MemoryCacheImageInputStream(data);
        reader.setInput(mciis);

        try {
            final int originalWidth = reader.getWidth(0);
            final int originalHeight = reader.getHeight(0);
            final double resizeRatio = calculateResizeRatio(originalWidth, originalHeight, width, height);
            final int targetWidth = (int)Math.max(originalWidth * resizeRatio, 1);
            final int targetHeight = (int)Math.max(originalHeight * resizeRatio, 1);

            if (log.isDebugEnabled()) {
                log.debug("Resizing from " + originalWidth + "x" + originalHeight + " to " + targetWidth + "x"
                        + targetHeight);
            }

            BufferedImage originalImage = reader.read(0);
            BufferedImage scaledImage = null;
            if (resizeRatio < 1.0d) {
                // scale down
                scaledImage = ImageUtils.scaleImage(originalImage, targetWidth, targetHeight,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
                scaledWidth = targetWidth;
                scaledHeight = targetHeight;
            } else if (upscaling) {
                // scale up
                scaledImage = ImageUtils.scaleImage(originalImage, targetWidth, targetHeight,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR, false);
                scaledWidth = targetWidth;
                scaledHeight = targetHeight;
            } else {
                // do not scale this image up, but keep the original size
                scaledImage = originalImage;
                scaledWidth = originalWidth;
                scaledHeight = originalHeight;
            }

            ByteArrayOutputStream bytes = ImageUtils.writeImage(writer, scaledImage);
            scaledData = new ByteArrayInputStream(bytes.toByteArray());
        } finally {
            mciis.close();
        }
    }

    protected double calculateResizeRatio(double originalWidth, double originalHeight, int targetWidth,
            int targetHeight) {
        double widthRatio = 1;
        double heightRatio = 1;

        if (targetWidth >= 1) {
            widthRatio = targetWidth / originalWidth;
        }
        if (targetHeight >= 1) {
            heightRatio = targetHeight / originalHeight;
        }

        // If the image has to be scaled down we should return the largest negative ratio.
        // If the image has to be scaled up, and we should take the smallest positive ratio.
        return Math.min(widthRatio, heightRatio);
    }

    /**
     * @return the scaled image data
     */
    public InputStream getScaledData() {
        return scaledData;
    }

    /**
     * @return the width of this scaled image
     */
    public int getScaledWidth() {
        return scaledWidth;
    }

    /**
     * @return the height of this scaled image
     */
    public int getScaledHeight() {
        return scaledHeight;
    }

}
