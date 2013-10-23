/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.jcr;

import javax.jcr.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorator for the 'href' attribute of link ('a') tags that replaces the 'href' attribute of internal links.
 * The 'href' attribute should be the name of a child hippo:facetselect node of the document node, and will be
 * replaced with a 'uuid' attribute that contains the docbase of the facetselect (i.e. the node it refers to).
 * When no such facetselect node exists, the 'uuid' attribute is set to an empty string.
 */
class LinkHrefToUuidDecorator extends InternalLinkDecorator {

    private static final Logger log = LoggerFactory.getLogger(LinkHrefToUuidDecorator.class);

    private final Node documentNode;

    LinkHrefToUuidDecorator(Node documentNode) {
        super("href");
        this.documentNode = documentNode;
    }

    @Override
    public String internalLink(final String href) {
        final String childFacetNodeName = href;
        final String uuidOrNull = RichTextFacetHelper.getChildDocBaseOrNull(this.documentNode, childFacetNodeName);
        final String uuid = uuidOrNull == null ? "" : uuidOrNull;
        return "uuid=\"" + uuid + "\"";
    }

}
