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

package org.hippoecm.frontend.plugins.xinha.dialog.images;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.i18n.types.TypeChoiceRenderer;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.plugins.xinha.dialog.AbstractBrowserDialog;
import org.hippoecm.frontend.plugins.xinha.services.images.XinhaImage;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.ThrottledTextFieldWidget;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.gallery.GalleryWorkflow;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageBrowserDialog extends AbstractBrowserDialog<XinhaImage> implements IHeaderContributor {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(ImageBrowserDialog.class);

    public final static List<String> ALIGN_OPTIONS = Arrays.asList("top", "middle", "bottom", "left", "right");
    private static final String CONFIG_KEY_PREFERRED_RESOURCE_NAMES = "preferred.resource.names";
    private static final String GALLERY_TYPE_SELECTOR_ID = "galleryType";

    DropDownChoice<String> type;

    private LinkedHashMap<String, String> nameTypeMap;

    private IModel<XinhaImage> imageModel;
    private boolean uploadSelected;
    private AjaxButton uploadButton;
    private Component uploadTypeSelector;
    private final LoadableDetachableModel<List<String>> galleryTypesModel;
    private String galleryType;

    public ImageBrowserDialog(IPluginContext context, final IPluginConfig config, final IModel<XinhaImage> model) {
        super(context, config, model);
        imageModel = model;

        galleryTypesModel = new LoadableDetachableModel<List<String>>() {
            @Override
            protected List<String> load() {
                return loadGalleryTypes();
            }
        };

        add(createUploadForm());

        if (nameTypeMap == null) {
            nameTypeMap = new  LinkedHashMap<String, String>();
        }
        type = new DropDownChoice<String>("type", new StringPropertyModel(model, XinhaImage.TYPE), new ArrayList<String>(nameTypeMap.keySet()), new IChoiceRenderer<String>() {
            private static final long serialVersionUID = 1L;

            public Object getDisplayValue(String object) {
                return nameTypeMap.get(object);
            }

            public String getIdValue(String object, int index) {
                return object;
            }

        });
        type.add(new AjaxFormComponentUpdatingBehavior("onChange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                checkState();
            }
        });
        type.setOutputMarkupId(true);
        type.setNullValid(false);
        add(type);

        add(new ThrottledTextFieldWidget("alt", new StringPropertyModel(model, XinhaImage.ALT)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                checkState();
            }
        });

        DropDownChoice<String> align = new DropDownChoice<String>("align", new StringPropertyModel(model,
                XinhaImage.ALIGN), ALIGN_OPTIONS, new IChoiceRenderer<String>() {
            private static final long serialVersionUID = 1L;

            public Object getDisplayValue(String object) {
                return new StringResourceModel(object, ImageBrowserDialog.this, null).getString();
            }

            public String getIdValue(String object, int index) {
                return object;
            }

        });
        align.add(new AjaxFormComponentUpdatingBehavior("onChange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                checkState();
            }
        });

        align.setOutputMarkupId(true);
        align.setNullValid(false);
        add(align);

        checkState();
    }

    protected void onModelSelected(IModel<Node> model) {
        setTypeChoices(model.getObject());

        if (type != null) {
            // if an preexisting image is selected, the constructor is not yet called so the class members are not initialized
            setPreferredTypeChoice();
            AjaxRequestTarget target = AjaxRequestTarget.get();
            if (target != null) {
                target.addComponent(type);
            }
        }
    }

    /**
     * This is the callback to enable/disable the OK button of the image browser dialog.
     * We abuse it here as a signal that a new folder may have been selected. If so, the
     * drop-down menu for selecting the target gallery type for uploaded images may need to
     * be adjusted.
     *
     * @param isset pass-through parameter
     */
    @Override
    protected void setOkEnabled(boolean isset) {
        super.setOkEnabled(isset);

        // function is called by parent constructor, when our constructor hasn't run yet...
        if (uploadTypeSelector != null && uploadButton != null) {

            AjaxRequestTarget target = AjaxRequestTarget.get();
            if (target != null) {
                target.addComponent(uploadTypeSelector);
                target.addComponent(uploadButton);
            }
        }
    }

    private void setPreferredTypeChoice() {
        IPluginConfig config = getPluginConfig();
        if (config.containsKey(CONFIG_KEY_PREFERRED_RESOURCE_NAMES)) {
            String[] preferredType = config.getStringArray(CONFIG_KEY_PREFERRED_RESOURCE_NAMES);
            if (preferredType.length > 0 && nameTypeMap.containsKey(preferredType[0])) {
                imageModel.getObject().setType(preferredType[0]);
            }
        }
        if (StringUtils.isBlank(imageModel.getObject().getType())) {
            log.warn("The preferred image variant configuration of the xinha plugin is not correct. Configure one of the available variants '{}'.", nameTypeMap.keySet());
            if (nameTypeMap.size() > 0) {
                String firstType = nameTypeMap.keySet().iterator().next();
                imageModel.getObject().setType(firstType);
            }
        }
    }

    private void setTypeChoices(final Node imageSetNode) {
        if (nameTypeMap == null) {
             nameTypeMap = new LinkedHashMap<String, String>();
        } else {
            nameTypeMap.clear();
        }

        Set<Map.Entry<String, String>> sortedEntries = new TreeSet<Map.Entry<String, String>>(new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(final Map.Entry<String, String> o1, final Map.Entry<String, String> o2) {
                return (o1.getValue() == null || o2.getValue() == null) ? -1 : o1.getValue().compareTo(o2.getValue());
            }
        });

        try {
            Node tmpImageSetNode = imageSetNode;
            if (tmpImageSetNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                tmpImageSetNode = tmpImageSetNode.getNode(tmpImageSetNode.getName());
            }
            TypeTranslator typeTranslator = new TypeTranslator(new JcrNodeTypeModel(tmpImageSetNode.getPrimaryNodeType()));
            NodeIterator childNodes = tmpImageSetNode.getNodes();
            while (childNodes.hasNext()) {
                Node childNode = childNodes.nextNode();
                if (childNode.isNodeType("hippogallery:image")) {
                    String childNodeName = childNode.getName();
                    sortedEntries.add(new AbstractMap.SimpleEntry<String, String>(childNodeName, typeTranslator.getPropertyName(childNodeName).getObject()));
                }
            }
        } catch (RepositoryException repositoryException) {
            log.error("Error updating the available image variants.", repositoryException);
        }

        for(Map.Entry<String, String> entry : sortedEntries){
            nameTypeMap.put(entry.getKey(), entry.getValue());
        }

        if (type != null) {
            type.setChoices(new ArrayList<String>(nameTypeMap.keySet()));
            type.updateModel();
        }
    }

    @SuppressWarnings("unchecked")
    private Component createUploadForm() {
        Form<?> uploadForm = new Form("uploadForm");

        uploadForm.setOutputMarkupId(true);
        final FileUploadField uploadField = new FileUploadField("uploadField");
        uploadField.setOutputMarkupId(true);

        // we use a container to enable Ajax-based (in)visibility while not meddling with the selected upload file.
        uploadTypeSelector = new WebMarkupContainer("uploadTypeSelector").add(createTypeSelector());
        uploadTypeSelector.setOutputMarkupId(true);

        uploadButton = new AjaxButton("uploadButton", uploadForm) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                if (uploadSelected) {
                    List<String> galleryTypes = galleryTypesModel.getObject();
                    return galleryTypes.size() > 0; // disable upload if the current folder has no gallery types at all
                }
                return false;
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                final FileUpload upload = uploadField.getFileUpload();
                if (upload != null) {
                    try {
                        String filename = upload.getClientFileName();
                        String mimetype;


                        mimetype = upload.getContentType();
                        InputStream istream = upload.getInputStream();
                        WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                        HippoNode node = null;
                        try {
                            //Get the selected folder from the folderReference Service
                            Node folderNode = getFolderModel().getObject();

                            //TODO replace shortcuts with custom workflow category(?)
                            GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow("gallery", folderNode);
                            String nodeName = getNodeNameCodec().encode(filename);
                            String localName = getLocalizeCodec().encode(filename);
                            Document document = workflow.createGalleryItem(nodeName, getGalleryType());
                            node = (HippoNode) (((UserSession) Session.get())).getJcrSession().getNodeByUUID(document.getIdentity());
                            DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                            if (!node.getLocalizedName().equals(localName)) {
                                defaultWorkflow.localizeName(localName);
                            }
                        } catch (WorkflowException ex) {
                            log.error(ex.getMessage());
                            error(ex);
                        } catch (MappingException ex) {
                            log.error(ex.getMessage());
                            error(ex);
                        } catch (RepositoryException ex) {
                            log.error(ex.getMessage());
                            error(ex);
                        }
                        if (node != null) {
                            try {
                                getGalleryProcessor().makeImage(node, istream, mimetype, filename);
                                node.getSession().save();
                                uploadField.setModel(null);
                                target.addComponent(uploadField);
                            } catch (RepositoryException ex) {
                                log.error(ex.getMessage());
                                error(ex);
                                try {
                                    DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                                    defaultWorkflow.delete();
                                } catch (WorkflowException e) {
                                    log.error(e.getMessage());
                                } catch (MappingException e) {
                                    log.error(e.getMessage());
                                } catch (RepositoryException e) {
                                    log.error(e.getMessage());
                                }
                                try {
                                    node.getSession().refresh(false);
                                } catch (RepositoryException e) {
                                    // deliberate ignore
                                }
                            } catch (GalleryException ex) {
                                log.error(ex.getMessage());
                                error(ex);
                            }
                        }
                    } catch (IOException ex) {
                        log.info("upload of image truncated");
                        error("Unable to read the uploaded image");
                    }
                } else {
                    error("Please select a file to upload");
                }
            }
        };

        uploadSelected = false;

        uploadButton.setOutputMarkupId(true);
        uploadField.add(new AjaxEventBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                uploadSelected = true;
                target.addComponent(uploadButton);
            }
        });
        uploadForm.add(uploadField);
        uploadForm.add(uploadTypeSelector);
        uploadForm.add(uploadButton);

        //OMG: ugly workaround.. Input[type=file] is rendered differently on OSX in all browsers..
        WebRequestCycle requestCycle = (WebRequestCycle) RequestCycle.get();
        HttpServletRequest httpServletReq = requestCycle.getWebRequest().getHttpServletRequest();
        String ua = httpServletReq.getHeader("User-Agent");
        if (ua.contains("Macintosh")) {
            uploadField.add(new AttributeAppender("class", true, new Model<String>("browse-button-osx"), " "));
            uploadButton.add(new AttributeAppender("class", true, new Model<String>("upload-button-osx"), " "));
        }

        return uploadForm;
    }

    @Override
    protected void onOk() {
        XinhaImage image = getModelObject();

        boolean imageIsValid = false;
        String facetSelect = image.getFacetSelectPath();
        if (facetSelect != null && facetSelect.indexOf('/') > 0) {
            String basePath = facetSelect.substring(0, facetSelect.lastIndexOf('/') + 1);
            String imageType = image.getType();
            image.setFacetSelectPath(basePath + imageType);
            imageIsValid = image.isValid();
            if (!imageIsValid) {
                image.setFacetSelectPath(facetSelect);
                imageIsValid = image.isValid();
            }
        }

        if (imageIsValid) {
            image.save();
        } else {
            error("Please select an image");
        }
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        final String IMAGE_BROWSER_DIALOG_CSS = "ImageBrowserDialog.css";
        ResourceReference dialogCSS = new ResourceReference(ImageBrowserDialog.class, IMAGE_BROWSER_DIALOG_CSS);
        response.renderCSSReference(dialogCSS);
    }

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=855,height=525");
    }

    protected GalleryProcessor getGalleryProcessor() {
        IPluginContext context = getPluginContext();
        GalleryProcessor processor = context.getService(getPluginConfig().getString("gallery.processor.id",
                "service.gallery.processor"), GalleryProcessor.class);
        if (processor != null) {
            return processor;
        }
        return new DefaultGalleryProcessor();
    }

    private StringCodec getNodeNameCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID, ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.node");
    }

    private StringCodec getLocalizeCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID, ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.display");
    }

    private String getGalleryType() {
        List<String> galleryTypes = galleryTypesModel.getObject();

        if (galleryType == null || galleryTypes.indexOf(galleryType) < 0) {
            if (galleryTypes.size() > 0) {
                galleryType = galleryTypes.get(0);
            }
        }
        return galleryType;
    }

    /**
     * Create the galleryTypeSelector, only shown in the UI if there actually is something to choose from.
     * Send changes to the backend using Ajax, in order to remember old choices while navigating through the
     * gallery.
     *
     * @return the type selector component
     */
    @SuppressWarnings("unchecked")
    private Component createTypeSelector() {
        getGalleryType(); // initialize the galleryType value
        return new DropDownChoice<String>(GALLERY_TYPE_SELECTOR_ID, new PropertyModel(this, "galleryType"),
                galleryTypesModel, new TypeChoiceRenderer(this)) {
            @Override
            public boolean isVisible() {
                return getChoices().size() > 1;
            }
        }
        .setNullValid(false)
        .add(new AjaxFormComponentUpdatingBehavior("onchange") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                // required because abstract, but all we need is to have galleryType set, which happens underwater.
            }
        });
    }

    /**
     * Load gallery types from repo-based configuration (target folder)
     * @return list of supported type names for the current folder.
     */
    private List<String> loadGalleryTypes() {
        List<String> types = new ArrayList<String>();
        WorkflowManager manager = UserSession.get().getWorkflowManager();

        try {
            Node folderNode = getFolderModel().getObject();
            GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow("gallery", folderNode);
            types = workflow.getGalleryTypes();
        } catch (Exception e) {
            log.error("Failed to load gallery types", e);
        }

        return types;
    }
}