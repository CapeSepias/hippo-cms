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
package org.hippoecm.frontend.plugins.gallery.processor;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.frontend.editor.plugins.resource.ResourceException;
import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gallery processor that puts a resized version of the image in the primary item and provides hooks to
 * initialize additional properties and the other resource child nodes.
 */
public abstract class AbstractGalleryProcessor implements GalleryProcessor {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected static final String MIMETYPE_IMAGE_PREFIX = "image";

    private static final Logger log = LoggerFactory.getLogger(AbstractGalleryProcessor.class);
    private static final long serialVersionUID = 1L;

    public AbstractGalleryProcessor() {
        // do nothing
    }

    public void makeImage(Node node, InputStream stream, String mimeType, String fileName) throws GalleryException,
            RepositoryException {

        Node primaryChild = getPrimaryChild(node);
        if (primaryChild.isNodeType(HippoNodeType.NT_RESOURCE)) {
            log.debug("Setting JCR data of primary resource");
            primaryChild.setProperty("jcr:mimeType", mimeType);
            primaryChild.setProperty("jcr:data", stream);
        }
        validateResource(primaryChild, fileName);

        String primaryMimeType = primaryChild.getProperty("jcr:mimeType").getString();
        initGalleryNode(node, stream, primaryMimeType, fileName);

        // create all child resource nodes, reusing the stream of the primary child
        Calendar lastModified = primaryChild.getProperty("jcr:lastModified").getDate();
        for (NodeDefinition childDef : node.getPrimaryNodeType().getChildNodeDefinitions()) {
            NodeType childNodeType = childDef.getDefaultPrimaryType();
            if (childNodeType != null && childNodeType.isNodeType(HippoNodeType.NT_RESOURCE)) {
                String childName = childDef.getName();
                if (!node.hasNode(childName)) {
                    log.debug("Adding resource {}", childName);
                    Node child = node.addNode(childName);
                    InputStream primaryData = primaryChild.getProperty("jcr:data").getStream();
                    initGalleryResource(child, primaryData, primaryMimeType, fileName, lastModified);
                }
            }
        }

        // finally, create the primary resource node
        log.debug("Initializing primary resource");
        InputStream primaryData = primaryChild.getProperty("jcr:data").getStream();
        initGalleryResource(primaryChild, primaryData, primaryMimeType, fileName, lastModified);
    }

    protected Node getPrimaryChild(Node node) throws RepositoryException, GalleryException {
        Item result = null;
        try {
            result = node.getPrimaryItem();
        } catch (ItemNotFoundException ignored) {
            // ignore
        }

        if (result == null || !result.isNode()) {
            throw new GalleryException("Primary item is not a node");
        }

        return (Node) result;
    }

    /**
     * Validates a resource node. When validation of the primary child fails, the main gallery node is not
     * initialized further and the other resource nodes are left untouched.
     *
     * @param node the resource node to validate
     * @param fileName the file name of the uploaded resource
     *
     * @throws GalleryException when the node is not a valid resource node
     * @throws RepositoryException when repository access failed
     */
    public void validateResource(Node node, String fileName) throws GalleryException,
            RepositoryException {
        try {
            ResourceHelper.validateResource(node, fileName);
        } catch (ResourceException e) {
            throw new GalleryException("Invalid resource: " + fileName, e);
        }
    }

    /**
     * Checks whether the given MIME type indicates an image.
     *
     * @param mimeType the MIME type to check
     * @return true if the given MIME type indicates an image, false otherwise.
     */
    protected boolean isImageMimeType(String mimeType) {
        return mimeType.startsWith(MIMETYPE_IMAGE_PREFIX);
    }

    /**
     * Initializes properties of the main gallery node.
     *
     * @param node the main gallery node
     * @param data the uploaded data
     * @param mimeType the MIME type of the uploaded data
     * @param fileName the file name of the uploaded data
     *
     * @throws RepositoryException when repository access failed
     */
    public abstract void initGalleryNode(Node node, InputStream data, String mimeType, String fileName)
            throws RepositoryException;

    /**
     * Initializes a hippo:resource node of an the main gallery node. Such initialization happens at two times:
     * when a new image is uploaded to the gallery, and when an image in an existing imageset is replaced
     * by another image.
     *
     * @param node the hippo:resource node
     * @param data the uploaded data
     * @param mimeType the MIME type of the uploaded data
     * @param fileName the file name of the uploaded data
     *
     * @throws RepositoryException when repository access failed.
     */
    public abstract void initGalleryResource(Node node, InputStream data, String mimeType, String fileName,
            Calendar lastModified) throws GalleryException, RepositoryException;

}
