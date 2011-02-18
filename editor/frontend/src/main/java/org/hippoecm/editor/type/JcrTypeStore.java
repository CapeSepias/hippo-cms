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
package org.hippoecm.editor.type;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.protocol.http.WebApplication;
import org.hippoecm.editor.repository.NamespaceWorkflow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.types.BuiltinTypeStore;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeLocator;
import org.hippoecm.frontend.types.TypeException;
import org.hippoecm.frontend.types.TypeLocator;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrTypeStore implements IStore<ITypeDescriptor>, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";

    private static final long serialVersionUID = 1L;

    static final String SUPERTYPE_QUERY = "";

    static final Logger log = LoggerFactory.getLogger(JcrTypeStore.class);

    private ITypeLocator locator;
    private Map<String, ITypeDescriptor> types = new HashMap<String, ITypeDescriptor>();
    private Map<String, ITypeDescriptor> currentTypes = new HashMap<String, ITypeDescriptor>();

    @SuppressWarnings("unchecked")
    public JcrTypeStore() {
        BuiltinTypeStore builtinTypeStore = new BuiltinTypeStore();
        locator = new TypeLocator(new IStore[] { this, builtinTypeStore });
        builtinTypeStore.setTypeLocator(locator);
    }

    public ITypeLocator getTypeLocator() {
        return this.locator;
    }

    /**
     * Set the type locator that will be used by type descriptors to resolve super
     * types.
     * @param locator
     */
    public void setTypeLocator(ITypeLocator locator) {
        this.locator = locator;
    }

    public ITypeDescriptor getTypeDescriptor(String name) {
        if ("rep:root".equals(name)) {
            // ignore the root node
            return null;
        }
        ITypeDescriptor result = types.get(name);
        if (result == null) {
            try {
                TypeVersion version = new TypeVersion(name);
                Node typeNode = version.getTypeNode(getJcrSession());
                if (typeNode != null) {
                    result = createTypeDescriptor(typeNode, name);
                    types.put(name, result);
                } else {
                    log.debug("No nodetype description found for " + name);
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            } catch (StoreException e) {
                log.error(e.getMessage());
            }
        }
        return result;
    }

    public void delete(ITypeDescriptor object) {
    }

    public Iterator<ITypeDescriptor> find(Map<String, Object> criteria) throws StoreException {
        Map<String, ITypeDescriptor> results = new TreeMap<String, ITypeDescriptor>();
        if (criteria.containsKey("supertype")) {
            if (!(criteria.get("supertype") instanceof List)) {
                throw new StoreException("Invalid criteria; supertype should be of type List<String>");
            }
            List<String> supertype = (List<String>) criteria.get("supertype");
            if (supertype.size() == 0) {
                throw new StoreException("No supertypes specified");
            }
            Session session = getJcrSession();
            try {
                QueryManager qm = session.getWorkspace().getQueryManager();
                StringBuilder sb = new StringBuilder();
                sb.append("//element(*,");
                sb.append(HippoNodeType.NT_NODETYPE);
                sb.append(")[@");
                sb.append(HippoNodeType.HIPPO_SUPERTYPE);
                sb.append("='");
                boolean first = true;
                for (String type : supertype) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append("' or @");
                        sb.append(HippoNodeType.HIPPO_SUPERTYPE);
                        sb.append("='");
                    }
                    sb.append(type);
                }
                sb.append("']");
                String queryStr = sb.toString();
                log.debug("Query for supertypes: {}", queryStr);
                Query query = qm.createQuery(queryStr, Query.XPATH);
                QueryResult result = query.execute();
                for (NodeIterator nodes = result.getNodes(); nodes.hasNext();) {
                    Node node = nodes.nextNode();
                    String realType = null;
                    if (node.hasProperty(HippoNodeType.HIPPOSYSEDIT_TYPE)) {
                        realType = node.getProperty(HippoNodeType.HIPPOSYSEDIT_TYPE).getString();
                    }
                    // assume /hippo:namespaces/<prefix>/<subType>/hipposysedit:nodetype/hipposysedit:nodetype
                    if (node.getDepth() < 5) {
                        continue;
                    }
                    node = node.getParent();
                    if (!node.isNodeType(HippoNodeType.NT_HANDLE)) {
                        continue;
                    }
                    node = node.getParent();
                    if (!node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                        continue;
                    }
                    String subType = node.getName();
                    node = node.getParent();
                    if (!node.isNodeType(HippoNodeType.NT_NAMESPACE)) {
                        continue;
                    }
                    String prefix = node.getName();
                    if ("system".equals(prefix)) {
                        continue;
                    }
                    String subTypeName = prefix + ":" + subType;
                    if (realType != null && !realType.equals(subTypeName)) {
                        continue;
                    }
                    if (!results.containsKey(subTypeName)) {
                        try {
                            ITypeDescriptor type = locator.locate(subTypeName);
                            results.put(subTypeName, type);
                        } catch (StoreException ex) {
                            // not found; continue
                            continue;
                        }
                    }
                }
            } catch (RepositoryException e) {
                log.error("Error searching for subtypes of " + supertype, e);
            }
        }
        return results.values().iterator();
    }

    public ITypeDescriptor load(String id) throws StoreException {
        ITypeDescriptor result = getTypeDescriptor(id);
        if (result == null) {
            throw new StoreException("Could not find type " + id);
        }
        return result;
    }

    public String save(ITypeDescriptor object) throws StoreException {
        if (object instanceof JcrTypeDescriptor) {
            ((JcrTypeDescriptor) object).save();
        } else {
            TypeInfo info;
            try {
                info = new TypeInfo(object.getType());
            } catch (RepositoryException ex) {
                throw new StoreException("unable to store type", ex);
            }
            String path = info.getPath();

            HippoSession session = (HippoSession) getJcrSession();
            try {
                if (session.itemExists(path)) {
                    throw new StoreException("type already exists");
                }
                Node nsNode = (Node) session.getItem(info.getNamespace().getPath());

                WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
                Workflow workflow = workflowManager.getWorkflow("editor", nsNode);

                ((NamespaceWorkflow) workflow).addCompoundType(info.subType);

                nsNode.refresh(false);

                ITypeDescriptor type = getDraftType(object.getType());
                if (type == null) {
                    throw new StoreException("Could not find newly created draft");
                }
                type.setSuperTypes(object.getSuperTypes());
                type.setIsNode(object.isNode());
                type.setIsMixin(object.isMixin());
                type.setIsValidationCascaded(object.isValidationCascaded());
                for (Map.Entry<String, IFieldDescriptor> entry : object.getFields().entrySet()) {
                    type.addField(entry.getValue());
                }

                return object.getName();

            } catch (WorkflowException ex) {
                log.error(ex.getMessage());
                throw new StoreException(ex);
            } catch (RemoteException ex) {
                log.error(ex.getMessage());
                throw new StoreException(ex);
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
                throw new StoreException(ex);
            } catch (TypeException ex) {
                log.error(ex.getMessage());
                throw new StoreException(ex);
            }
        }

        return object.getName();
    }

    public void detach() {
        for (ITypeDescriptor type : types.values()) {
            if (type instanceof IDetachable) {
                ((IDetachable) type).detach();
            }
        }
        for (ITypeDescriptor type : currentTypes.values()) {
            if (type instanceof IDetachable) {
                ((IDetachable) type).detach();
            }
        }
    }

    public ITypeDescriptor getDraftType(String name) throws StoreException {
        ITypeDescriptor result = currentTypes.get(name);
        if (result == null) {
            try {
                TypeVersion version = new TypeVersion(name);
                TypeInfo info = version.getTypeInfo();
                TypeVersion current = info.getDraft();
                Node typeNode = current.getTypeNode(getJcrSession());
                if (typeNode != null) {
                    result = createTypeDescriptor(typeNode, name);
                    currentTypes.put(name, result);
                } else {
                    log.debug("No nodetype description found for " + name);
                }
            } catch (RepositoryException e) {
                throw new StoreException("Could not find draft type descriptor", e);
            }
        }
        return result;
    }

    // Privates

    private Session getJcrSession() {
        return ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
    }

    private ITypeDescriptor createTypeDescriptor(Node typeNode, String type) throws RepositoryException, StoreException {
        if (typeNode.isNodeType(HippoNodeType.NT_NODETYPE)) {
            if (typeNode.hasProperty(HippoNodeType.HIPPOSYSEDIT_TYPE)) {
                String realType = typeNode.getProperty(HippoNodeType.HIPPOSYSEDIT_TYPE).getString();
                if (!realType.equals(type)) {
                    return new PseudoTypeDescriptor(type, locator.locate(realType));
                }
            }

            JcrTypeDescriptor result = new JcrTypeDescriptor(new JcrNodeModel(typeNode), locator);
            // do validation on type
            if (WebApplication.get() != null && "development".equals(WebApplication.get().getConfigurationType())) {
                result.validate();
            }
            return result;

        }
        return null;
    }

    private class TypeInfo {
        JcrNamespace nsInfo;
        String subType;

        TypeInfo(String type) throws RepositoryException {
            String prefix = "system";
            subType = type;
            if (type.indexOf(':') > 0) {
                prefix = type.substring(0, type.indexOf(':'));
                subType = NodeNameCodec.encode(type.substring(type.indexOf(':') + 1));
            }
            nsInfo = new JcrNamespace(getJcrSession(), prefix);
        }

        JcrNamespace getNamespace() {
            return nsInfo;
        }

        String getPath() {
            return nsInfo.getPath() + "/" + subType;
        }

        TypeVersion getDraft() throws RepositoryException {
            return new TypeVersion(this, true, null);
        }
    }

    private class TypeVersion {

        TypeInfo info;
        boolean draft;
        String uri;

        TypeVersion(String type) throws RepositoryException {
            info = new TypeInfo(type);
            draft = false;
            if (type.indexOf(':') > 0) {
                String prefix = type.substring(0, type.indexOf(':'));
                uri = getJcrSession().getWorkspace().getNamespaceRegistry().getURI(prefix);
            } else {
                uri = "internal";
            }
        }

        TypeVersion(TypeInfo info, boolean draft, String uri) {
            this.info = info;
            this.draft = draft;
            this.uri = uri;
        }

        TypeInfo getTypeInfo() {
            return info;
        }
        
        Node getTypeNode(Session session) throws RepositoryException {
            String path = info.getPath();
            if (!session.itemExists(path) || !session.getItem(path).isNode()) {
                return null;
            }

            NodeIterator iter = ((Node) session.getItem(path)).getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE).getNodes(
                    HippoNodeType.HIPPOSYSEDIT_NODETYPE);

            String prefix = info.getNamespace().getPrefix();
            boolean isHippoNs = "hippo".equals(prefix) || "hipposys".equals(prefix);
            while (iter.hasNext()) {
                Node node = iter.nextNode();
                if (!node.isNodeType(HippoNodeType.NT_REMODEL)) {
                    if (draft) {
                        return node;
                    } else if (node.hasProperty(HippoNodeType.HIPPOSYSEDIT_TYPE)) {
                        String realType = node.getProperty(HippoNodeType.HIPPOSYSEDIT_TYPE).getString();
                        String pseudoType;
                        if (!"system".equals(prefix)) {
                            pseudoType = prefix + ":" + info.subType;
                        } else {
                            pseudoType = info.subType;
                        }
                        if (!realType.equals(pseudoType)) {
                            return node;
                        }
                    }
                } else {
                    // ignore uris for hippo namespace
                    if (isHippoNs || node.getProperty(HippoNodeType.HIPPO_URI).getString().equals(uri)) {
                        return node;
                    }
                }
            }
            return null;
        }

    }

}
