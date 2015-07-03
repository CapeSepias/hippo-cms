/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.standardworkflow.validators;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.widgets.NameUriField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate localized name and url name upon adding a new document/folder
 */
public abstract class AddDocumentValidator extends DocumentFormValidator{
    /**
     * Error key messages. Component uses this validator must have these keys in its resource bundle
     */
    public static final String ERROR_SNS_NODE_EXISTS = "error-sns-node-exists";
    public static final String ERROR_LOCALIZED_NAME_EXISTS = "error-localized-name-exists";
    public static final String ERROR_VALIDATION_NAMES = "error-validation-names";
    public static final String ERROR_SNS_NAMES_EXIST = "error-sns-names-exist";

    static Logger log = LoggerFactory.getLogger(AddDocumentValidator.class);

    private final NameUriField nameUriContainer;
    private final WorkflowDescriptorModel workflowDescriptorModel;

    public AddDocumentValidator(NameUriField nameUriContainer, final WorkflowDescriptorModel workflowDescriptorModel) {
        this.nameUriContainer = nameUriContainer;
        this.workflowDescriptorModel = workflowDescriptorModel;
    }

    @Override
    public FormComponent<?>[] getDependentFormComponents() {
        return nameUriContainer.getComponents();
    }

    @Override
    public void validate(final Form<?> form) {
        final String newNodeName = nameUriContainer.getUrlComponent().getValue().toLowerCase();
        final String newLocalizedName = nameUriContainer.getNameComponent().getValue();
        try {
            final Node parentNode = workflowDescriptorModel.getNode();
            final boolean hasNodeWithSameName = parentNode.hasNode(newNodeName);
            final boolean hasNodeWithSameLocalizedName = hasChildWithLocalizedName(parentNode, newLocalizedName);

            if (hasNodeWithSameName && hasNodeWithSameLocalizedName) {
                showError(ERROR_SNS_NAMES_EXIST, newNodeName, newLocalizedName);
            } else if (hasNodeWithSameName) {
                showError(ERROR_SNS_NODE_EXISTS, newNodeName);
            } else if (hasNodeWithSameLocalizedName) {
                showError(ERROR_LOCALIZED_NAME_EXISTS, newLocalizedName);
            }
        } catch (RepositoryException e) {
            log.error("validation error: {}", e.getMessage());
            showError(ERROR_VALIDATION_NAMES);
        }
    }
}
