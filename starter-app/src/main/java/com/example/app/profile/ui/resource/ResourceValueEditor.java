/*
 * Copyright (c) Interactive Information R & D (I2RD) LLC.
 * All Rights Reserved.
 *
 * This software is confidential and proprietary information of
 * I2RD LLC ("Confidential Information"). You shall not disclose
 * such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered
 * into with I2RD.
 */

package com.example.app.profile.ui.resource;

import com.example.app.profile.model.resource.Resource;
import com.example.app.profile.model.resource.ResourceType;
import com.example.app.profile.model.resource.ResourceVisibility;
import com.example.app.profile.service.ProfileUIService;
import com.example.app.support.service.AppUtil;
import com.example.app.support.ui.vtcrop.VTCropPictureEditor;
import com.example.app.support.ui.vtcrop.VTCropPictureEditorConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.proteusframework.cms.label.Label;
import net.proteusframework.cms.label.LabelDomainProvider;
import net.proteusframework.core.html.HTMLElement;
import net.proteusframework.core.locale.LocalizedNamedObjectComparator;
import net.proteusframework.core.locale.TextSources;
import net.proteusframework.core.locale.annotation.I18N;
import net.proteusframework.core.locale.annotation.I18NFile;
import net.proteusframework.core.locale.annotation.L10N;
import net.proteusframework.core.metric.PixelMetric;
import net.proteusframework.core.notification.Notifiable;
import net.proteusframework.data.filesystem.http.FileEntityFileItem;
import net.proteusframework.ui.miwt.component.Component;
import net.proteusframework.ui.miwt.component.ComponentImpl;
import net.proteusframework.ui.miwt.component.composite.CustomCellRenderer;
import net.proteusframework.ui.miwt.component.composite.editor.ComboBoxValueEditor;
import net.proteusframework.ui.miwt.component.composite.editor.ListComponentValueEditor;
import net.proteusframework.ui.miwt.component.composite.editor.LocalizedTextEditor;
import net.proteusframework.ui.miwt.component.composite.editor.TemplateCompositeValueEditor;
import net.proteusframework.ui.miwt.component.composite.editor.TextEditor;
import net.proteusframework.ui.miwt.component.composite.editor.ValueEditor;
import net.proteusframework.ui.miwt.component.template.TemplateDataSource;
import net.proteusframework.ui.miwt.util.CommonButtonText;

import static com.example.app.profile.model.resource.Resource.*;
import static com.example.app.profile.ui.resource.ResourceText.LABEL_AUTHOR;
import static com.example.app.profile.ui.resource.ResourceValueEditorLOK.*;
import static com.i2rd.miwt.util.CSSUtil.CSS_INSTRUCTIONS;

/**
 * {@link ValueEditor} for a {@link Resource} to be sub-classed based on the {@link ResourceType}
 *
 * @param <R> the resource type of the Resource to be edited
 *
 * <br><br>
 * Implementations of this should be {@link Configurable}
 *
 * @author Alan Holt (aholt@venturetech.net)
 * @since 11/16/15 4:47 PM
 */
@I18NFile(
    symbolPrefix = "com.example.app.profile.ui.resource.ResourceValueEditor",
    i18n = {
        @I18N(symbol = "Label Is Visible", l10n = @L10N("Visibility")),
        @I18N(symbol = "Label Categories", l10n = @L10N("Categories")),
        @I18N(symbol = "Label Type", l10n = @L10N("Type")),
        @I18N(symbol = "Label Name", l10n = @L10N("Name")),
        @I18N(symbol = "Label Description", l10n = @L10N("Description")),
        @I18N(symbol = "Placeholder Tags", l10n = @L10N("Click To Add Tags")),
        @I18N(symbol = "Instructions Picture Editor FMT",
            l10n = @L10N("Resource image is optional.  Optimal size for image is {0} x {1}."))
    }
)
public abstract class ResourceValueEditor<R extends Resource> extends TemplateCompositeValueEditor<R>
{
    /** The classname for the Resource Picture Editor */
    public static final String PICTURE_EDITOR_CLASS_NAME = "resource-picture";

    private LabelDomainProvider _categoryLabelProvider;
    private LabelDomainProvider _tagLabelProvider;
    private VTCropPictureEditorConfig _pictureEditorConfig;
    private AppUtil _appUtil;

    private VTCropPictureEditor _resourcePictureEditor;

    /**
     * Instantiate a new instance
     *
     * @param clazz the class of the resource for this editor
     * @param value the resource for this editor
     * @param dataSource the template data source.
     */
    public ResourceValueEditor(Class<R> clazz, @Nullable R value, TemplateDataSource dataSource)
    {
        super(clazz, dataSource);
        setInternalValue(value);
        addClassName("resource");
    }

    /**
     * Get the Resource Image Editor
     *
     * @return the Resource Image Editor
     */
    @SuppressWarnings("unused")
    @Nonnull
    public VTCropPictureEditor getPictureEditor()
    {
        return Optional.ofNullable(_resourcePictureEditor).orElseThrow(() -> new IllegalStateException(
            "PictureEditor was null.  Do not call getPictureEditor before initialization."));
    }

    /**
     * Set the Category Label Provider for this ResourceValueEditor
     *
     * @param uiService the Profile UI Service
     */
    @Autowired
    public void setCategoryLabelProvider(ProfileUIService uiService)
    {
        _categoryLabelProvider = uiService.getSelectedCompany().getResourceCategoriesLabelProvider();
    }

    /**
     * Set the LRLabsUtil for this ResourceValueEditor
     *
     * @param appUtil the App Util
     */
    @Autowired
    public void setLRLabsUtil(AppUtil appUtil)
    {
        _appUtil = appUtil;
    }

    /**
     * Set the PictureEditorConfig for this ResourceValueEditor
     *
     * @param resourceConfig the ResourceConfig from which to retrieve the PictureEditorConfig
     */
    @Autowired
    public void setPictureEditorConfig(ResourceConfig resourceConfig)
    {
        _pictureEditorConfig = resourceConfig.resourcePictureEditorConfig();
    }

    /**
     * Set the Type Label Provider for this ResourceValueEditor
     *
     * @param uiService the Profile UI Service
     */
    @Autowired
    public void setTagLabelProvider(ProfileUIService uiService)
    {
        _tagLabelProvider = uiService.getSelectedCompany().getResourceTagsLabelProvider();
    }

    @Override
    public void init()
    {
        _resourcePictureEditor = new VTCropPictureEditor(_pictureEditorConfig);
        _resourcePictureEditor.addClassName(PICTURE_EDITOR_CLASS_NAME);
        _resourcePictureEditor.setValue(Optional.ofNullable(getValue())
            .map(Resource::getImage)
            .map(FileEntityFileItem::new)
            .orElse(null));
        _resourcePictureEditor.setDefaultResource(_appUtil.getDefaultResourceImage());
        _resourcePictureEditor.setComponentName("resource-picture-editor");

        super.init();

        net.proteusframework.ui.miwt.component.Label pictureInstructions = new net.proteusframework.ui.miwt.component
            .Label(TextSources.createText(INSTRUCTIONS_PICTURE_EDITOR_FMT(),
            _pictureEditorConfig.getCropWidth(),
            _pictureEditorConfig.getCropHeight()));
        pictureInstructions.addClassName(CSS_INSTRUCTIONS);
        pictureInstructions.addClassName(PICTURE_EDITOR_CLASS_NAME);
        pictureInstructions.withHTMLElement(HTMLElement.div);
        pictureInstructions.setComponentName("resource-picture-instructions");

        add(_resourcePictureEditor);
        add(pictureInstructions);

        addEditorForProperty(() -> {
            LocalizedTextEditor e = new LocalizedTextEditor(LABEL_NAME(), null);
            e.setRequiredValueValidator();
            e.setComponentName("name");
            return e;
        }, NAME_COLUMN_PROP);

        addEditorForProperty(() -> {
            LocalizedTextEditor editor = new LocalizedTextEditor(LABEL_DESCRIPTION(), null);
            editor.setDisplayHeight(5);
            editor.setDisplayWidth(40);
            editor.setComponentName("description");
            return editor;
        }, DESCRIPTION_COLUMN_PROP);

        addEditorForProperty(() -> {
            TextEditor e = new TextEditor(LABEL_AUTHOR(), null);
            e.setRequiredValueValidator();
            e.setComponentName("author-property");
            return e;
        }, AUTHOR_COLUMN_PROP);

        addEditorForProperty(() -> {
            ComboBoxValueEditor<ResourceVisibility> e = new ComboBoxValueEditor<>(
                LABEL_IS_VISIBLE(), ResourceVisibility.getValuesForCombo(), null);
            e.setRequiredValueValidator();
            e.setCellRenderer(new CustomCellRenderer(CommonButtonText.PLEASE_SELECT));
            e.setComponentName("visibility-property");
            return e;
        }, VISIBILITY_COLUMN_PROP);

        addEditorForProperty(() -> {
            List<Label> rlsTags = _tagLabelProvider.getEnabledLabels(Optional.empty())
                .stream().sorted(new LocalizedNamedObjectComparator(getLocaleContext())).collect(Collectors.toList());
            ListComponentValueEditor<Label> editor = new ListComponentValueEditor<>(
                _tagLabelProvider.getLabelDomain().getName(), rlsTags, null);
            final Component valueComponent = editor.getValueComponent();
            editor.addClassName("categories");
            valueComponent.setAttribute("data-placeholder",
                PLACEHOLDER_TAGS().getText(getLocaleContext()).toString());
            final ComponentImpl impl = (ComponentImpl) valueComponent;
            impl.setWidth(new PixelMetric(200));
            editor.setComponentName("tags-property");
            return editor;
        }, TAGS_PROP);

        addEditorForProperty(() -> {
            List<Label> rlsTypes = new ArrayList<>(_categoryLabelProvider.getEnabledLabels(Optional.empty())
                .stream().sorted(new LocalizedNamedObjectComparator(getLocaleContext())).collect(Collectors.toList()));
            rlsTypes.add(0, null);
            ComboBoxValueEditor<Label> e = new ComboBoxValueEditor<>(_categoryLabelProvider.getLabelDomain().getName(),
                 rlsTypes, null);
            e.setCellRenderer(new CustomCellRenderer(CommonButtonText.PLEASE_SELECT));
            e.setRequiredValueValidator();
            e.setComponentName("category-property");
            return e;
        }, CATEGORY_PROP);

        setValue(getValue());
        setEditable(isEditable());
    }



    @Override
    public void setValue(@Nullable R value)
    {
        super.setValue(value);

        if (isInited())
        {
            _resourcePictureEditor.setValue(Optional.ofNullable(value)
                .map(Resource::getImage)
                .map(FileEntityFileItem::new)
                .orElse(null));
        }
    }

    @Override
    public void setEditable(boolean b)
    {
        super.setEditable(b);

        if (isInited())
        {
            _resourcePictureEditor.setEditable(b);
        }
    }

    @Override
    public boolean validateUIValue(Notifiable notifiable)
    {
        boolean valid = super.validateUIValue(notifiable);
        valid = _resourcePictureEditor.validateUIValue(notifiable) && valid;
        return valid;
    }

    @Override
    public ModificationState getModificationState()
    {
        return AppUtil.getModificationStateForComponent(this);
    }
}
