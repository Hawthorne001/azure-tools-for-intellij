/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.projectexplorer;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.tree.LeafState;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@Slf4j
public class GenericResourceNode extends AbstractAzureFacetNode<ResourceId> implements IAzureFacetNode {
    private final String status;

    protected GenericResourceNode(@Nonnull Project project, @Nonnull ResourceId id, String status) {
        super(project, id);
        this.status = status;
    }

    @Nonnull
    @Override
    protected Collection<? extends AbstractAzureFacetNode<?>> buildChildren() {
        return Collections.emptyList();
    }

    @Override
    protected void buildView(@Nonnull PresentationData presentation) {
        presentation.setIcon(IntelliJAzureIcons.getIcon(AzureIcons.Resources.GENERIC_RESOURCE));
        presentation.addText(this.getValue().name(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        presentation.addText(StringUtils.SPACE + Optional.ofNullable(this.status).orElse(""), SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }

    @Override
    public @Nonnull LeafState getLeafState() {
        return LeafState.ALWAYS;
    }
}
