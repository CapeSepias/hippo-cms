/*
 *  Copyright 2011-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.autoexport;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.onehippo.cms7.autoexport.InternalLocationMapperEntries.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This utility class is responsible for mapping repository paths to context nodes and export files.
 */
final class LocationMapper {

    private static Logger log = LoggerFactory.getLogger(LocationMapper.class);

    private LocationMapper() {}

    /**
     * The wrapper for the internal mapper entries, responsible for providing mapping entries.
     * <P>
     * <EM>Note:<EM> This wrapper can be overriden through the system property,
     * <code>-Dorg.onehippo.cms7.autoexport.InternalLocationMapperEntries=...</code>, by specifying a custom
     * class extending {@link InternalLocationMapperEntries} class. However, please note that kind of custom class
     * must be the responsibility of the end project which decided to use the customization approach.
     * </P>
     */
    private static InternalLocationMapperEntries internalMapperEntries = new InternalLocationMapperEntries();

    static {
        String prop = null;
        try {
            prop = System.getProperty(InternalLocationMapperEntries.class.getName());
            if (prop != null) {
                internalMapperEntries = (InternalLocationMapperEntries) Class.forName(prop).newInstance();
            }
        } catch (Throwable th) {
            log.error("Invalid custom InternalLocationMapperEntries class property: '{}'", prop, th);
        }
    }

    // cache the result of the last invocation 
    private static CachedItem lastResult = new CachedItem(null, null, null);

    static String contextNodeForPath(String path, boolean isNode) {
        if (!path.equals(lastResult.path)) {
            lastResult = matchPath(path, isNode);
        }
        return lastResult.contextNode;
    }

    static String fileForPath(String path, boolean isNode) {
        if (!path.equals(lastResult.path)) {
            lastResult = matchPath(path, isNode);
        }
        return lastResult.file;
    }

    private static CachedItem matchPath(String path, boolean isNode) {
        for (Entry entry : internalMapperEntries.getEntries()) {
            if (isNode) {
                for (Pattern pattern : entry.nodePatterns) {
                    Matcher matcher = pattern.matcher(path);
                    if (matcher.matches()) {
                        String contextNode = entry.contextNode;
                        for (int i = 1; i <= matcher.groupCount(); i++) {
                            contextNode = contextNode.replace("$" + i, matcher.group(i));
                        }
                        String file = entry.file;
                        for (int i = 1; i <= matcher.groupCount(); i++) {
                            String qName = matcher.group(i);
                            int indexOfColon = qName.indexOf(':');
                            String name = indexOfColon == -1 ? qName : qName.substring(indexOfColon + 1);
                            file = file.replace("$" + i, name);
                        }
                        return new CachedItem(path, contextNode, file);
                    }
                }
            } else {
                for (Pattern pattern : entry.propertyPatterns) {
                    Matcher matcher = pattern.matcher(path);
                    if (matcher.matches()) {
                        String contextNode = entry.contextNode;
                        for (int i = 1; i <= matcher.groupCount(); i++) {
                            contextNode = contextNode.replace("$" + i, matcher.group(i));
                        }
                        String file = entry.file;
                        for (int i = 1; i <= matcher.groupCount(); i++) {
                            String qName = matcher.group(i);
                            int indexOfColon = qName.indexOf(':');
                            String name = indexOfColon == -1 ? qName : qName.substring(indexOfColon + 1);
                            file = file.replace("$" + i, name);
                        }
                        return new CachedItem(path, contextNode, file);
                    }
                }
            }
        }
        return new CachedItem(null, null, null);
    }

    private static final class CachedItem {
        private final String path;
        private final String contextNode;
        private final String file;

        private CachedItem(String path, String contextNode, String file) {
            this.path = path;
            this.contextNode = contextNode;
            this.file = file;
        }
    }
}
