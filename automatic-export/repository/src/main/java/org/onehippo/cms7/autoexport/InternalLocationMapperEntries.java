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
package org.onehippo.cms7.autoexport;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This is an INTERNAL USAGE ONLY class for {@link LocationMapper}.
 * <P>
 * Only for advanced use cases or experimental purpose, 
 * </P>
 */
public class InternalLocationMapperEntries {

    protected static final String NAME = "([^/\\u005B\\u005D\\|\\*]+(?:\\u005B\\d+\\u005D)?)";
    protected static final String ANY = "(.*)";

    private static final List<Entry> ENTRIES = new ArrayList<Entry>();

    static {
        // /hippo:namespaces/example
        String[] nodePatterns = new String[] {"/hippo:namespaces/" + NAME};
        String[] propertyPatterns = new String[] {"/hippo:namespaces/" + NAME + "/" + NAME};
        String contextNode = "/hippo:namespaces/$1";
        String file = "namespaces/$1.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
        // /hippo:namespaces/example/doctype
        nodePatterns = new String[] {"/hippo:namespaces/" + NAME + "/" + NAME, "/hippo:namespaces/" + NAME + "/" + NAME + "/" + ANY};
        propertyPatterns = new String[] {"/hippo:namespaces/" + NAME + "/" + NAME + "/" + ANY};
        contextNode = "/hippo:namespaces/$1/$2";
        file = "namespaces/$1/$2.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
        // /hst:hst/hst:sites
        nodePatterns = new String[] {"/hst:hst/hst:sites" + ANY};
        propertyPatterns = nodePatterns;
        contextNode = "/hst:hst/hst:sites";
        file = "hst/sites.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
        // /hst:hst/hst:hosts
        nodePatterns = new String[] {"/hst:hst/hst:hosts" + ANY};
        propertyPatterns = nodePatterns;
        contextNode = "/hst:hst/hst:hosts";
        file = "hst/hosts.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
        // /hst:hst/hst:configurations
        nodePatterns = new String[] {"/hst:hst/hst:configurations", "/hst:hst/hst:configurations/" + NAME};
        propertyPatterns = new String[] {"/hst:hst/hst:configurations/" + NAME, "/hst:hst/hst:configurations/" + NAME + "/" + NAME};
        contextNode = "/hst:hst/hst:configurations";
        file = "hst/configurations.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
        // /hst:hst/hst:configurations/project
        nodePatterns = new String[] {"/hst:hst/hst:configurations/" + NAME + "/" + NAME};
        propertyPatterns = new String[] {"/hst:hst/hst:configurations/" + NAME + "/" + NAME + "/" + NAME};
        contextNode = "/hst:hst/hst:configurations/$1/$2";
        file = "hst/configurations/$1/$2.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
        // /hst:hst/hst:configurations/project/hst:components|hst:pages|hst:abstractpages|hst:prototypepages
        nodePatterns = new String[] {"/hst:hst/hst:configurations/" + NAME + "/(hst:components|hst:pages|hst:abstractpages|hst:prototypepages)/" + NAME, "/hst:hst/hst:configurations/" + NAME + "/(hst:components|hst:pages|hst:abstractpages|hst:prototypepages)/" + NAME + "/" + ANY};
        propertyPatterns = new String [] {"/hst:hst/hst:configurations/" + NAME + "/(hst:components|hst:pages|hst:abstractpages|hst:prototypepages)/" + NAME + "/" + ANY};
        contextNode = "/hst:hst/hst:configurations/$1/$2/$3";
        file = "hst/configurations/$1/$2/$3.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
        // /hst:hst/hst:configurations/project
        nodePatterns = new String[] {"/hst:hst/hst:configurations/" + NAME + "/" + NAME + "/" + ANY};
        propertyPatterns = nodePatterns;
        contextNode = "/hst:hst/hst:configurations/$1/$2";
        file = "hst/configurations/$1/$2.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
        // /hst:hst/hst:blueprints
        nodePatterns = new String[] {"/hst:hst/hst:blueprints"};
        propertyPatterns = new String[] {"/hst:hst/hst:blueprints/" + NAME};
        contextNode = "/hst:hst/hst:blueprints";
        file = "hst/blueprints.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
        // /hst:hst/hst:blueprints/subsite
        nodePatterns = new String[] {"/hst:hst/hst:blueprints/" + NAME};
        propertyPatterns = new String[] {"/hst:hst/hst:blueprints/" + NAME + "/" + NAME};
        contextNode = "/hst:hst/hst:blueprints/$1";
        file = "hst/blueprints/$1.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
        // /hst:hst/hst:blueprints/subsite/subtree
        nodePatterns = new String[] {"/hst:hst/hst:blueprints/" + NAME + "/" + NAME, "/hst:hst/hst:blueprints/" + NAME + "/" + NAME + "/" + ANY};
        propertyPatterns = new String[] {"/hst:hst/hst:blueprints/" + NAME + "/" + NAME + "/" + NAME, "/hst:hst/hst:blueprints/" + NAME + "/" + NAME + "/" + NAME + "/" + ANY};
        contextNode = "/hst:hst/hst:blueprints/$1/$2";
        file = "hst/blueprints/$1/$2.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
        // /hippo:configuration
        nodePatterns = new String[] {"/hippo:configuration", "/hippo:configuration/" + NAME};
        propertyPatterns = new String[] {"/hippo:configuration/" + NAME, "/hippo:configuration/" + NAME + "/" + NAME};
        contextNode = "/hippo:configuration";
        file = "configuration.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
        // /hippo:configuration/hippo:queries
        nodePatterns = new String[] {"/hippo:configuration/hippo:queries/" + NAME};
        propertyPatterns = new String[] {"/hippo:configuration/hippo:queries/" + NAME + "/" + NAME};
        contextNode = "/hippo:configuration/hippo:queries/$1";
        file = "configuration/queries/$1.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
        // /hippo:configuration/hippo:queries/queryfolder/query
        nodePatterns = new String[] {"/hippo:configuration/hippo:queries/" + NAME + "/" + NAME, "/hippo:configuration/hippo:queries/" + NAME + "/" + NAME + "/" + ANY};
        propertyPatterns = new String[] {"/hippo:configuration/hippo:queries/" + NAME + "/" + NAME + "/" + ANY};
        contextNode = "/hippo:configuration/hippo:queries/$1/$2";
        file = "configuration/queries/$1/$2.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
        // /hippo:configuration/subnode/subsubnode
        nodePatterns = new String[] {"/hippo:configuration/" + NAME + "/" + NAME, "/hippo:configuration/" + NAME + "/" + NAME + "/" + ANY};
        propertyPatterns = new String[] {"/hippo:configuration/" + NAME + "/" + NAME + "/" + ANY};
        contextNode = "/hippo:configuration/$1/$2";
        file = "configuration/$1/$2.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
        // /content/taxonomies
        nodePatterns = new String[] {"/content/taxonomies" + ANY};
        propertyPatterns = nodePatterns;
        contextNode = "/content/taxonomies";
        file = "taxonomies.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
        // /content
        nodePatterns = new String[] {"/content", "/content/" + NAME};
        propertyPatterns = new String[] {"/content/" + NAME, "/content/" + NAME + "/" + NAME};
        contextNode = "/content";
        file = "content.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
        // /content/documents/myproject
        nodePatterns = new String[] {"/content/" + NAME + "/" + NAME};
        propertyPatterns = new String[] {"/content/" + NAME + "/" + NAME + "/" + NAME};
        contextNode = "/content/$1/$2";
        file = "content/$1/$2.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
        // /content/documents/myproject/common
        nodePatterns = new String[] {"/content/" + NAME + "/" + NAME + "/" + NAME, "/content/" + NAME + "/" + NAME + "/" + NAME + "/" + ANY};
        propertyPatterns = new String[] {"/content/" + NAME + "/" + NAME + "/" + NAME + "/" + ANY};
        contextNode = "/content/$1/$2/$3";
        file = "content/$1/$2/$3.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
        // catch all: /node
        nodePatterns = new String[] {"/" + NAME, "/" + NAME + "/" + NAME};
        propertyPatterns = new String[] {"/" + NAME + "/" + NAME, "/" + NAME + "/" + NAME + "/" + NAME};
        contextNode = "/$1";
        file = "$1.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
        // catch all: /node/subnode/subsubnode
        nodePatterns = new String[] {"/" + NAME + "/" + NAME + "/" + NAME, "/" + NAME + "/" + NAME + "/" + NAME + "/" + ANY};
        propertyPatterns = new String[] {"/" + NAME + "/" + NAME + "/" + NAME + "/" + ANY};
        contextNode = "/$1/$2/$3";
        file = "$1/$2/$3.xml";
        ENTRIES.add(createEntry(nodePatterns, propertyPatterns, contextNode, file));
    }

    protected static Entry createEntry(String[] nodePatterns, String[] propertyPatterns, String contextNode, String file) {
        return new Entry(nodePatterns, propertyPatterns, contextNode, file);
    }

    protected List<Entry> getEntries() {
        return ENTRIES;
    }

    protected static final class Entry {

        protected final Pattern[] nodePatterns;
        protected final Pattern[] propertyPatterns;
        protected final String contextNode;
        protected final String file;

        private Entry(String[] nodePatterns, String[] propertyPatterns, String contextNode, String file) {
            this.nodePatterns = new Pattern[nodePatterns.length];
            for (int i = 0; i < nodePatterns.length; i++) {
                this.nodePatterns[i] = Pattern.compile(nodePatterns[i]);
            }
            this.propertyPatterns = new Pattern[propertyPatterns.length];
            for (int i = 0; i < propertyPatterns.length; i++) {
                this.propertyPatterns[i] = Pattern.compile(propertyPatterns[i]);
            }
            this.contextNode = contextNode;
            this.file = file;
        }
    }

}
