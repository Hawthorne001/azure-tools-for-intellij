/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet.projectexplorer;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijAzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ActionNode<T> extends AbstractTreeNode<Action<T>> implements IAzureFacetNode {
    @Nullable
    private final T source;

    protected ActionNode(@Nonnull Project project, Action<T> action) {
        super(project, action);
        this.source = null;
    }

    protected ActionNode(@Nonnull Project project, Action.Id<T> actionId) {
        super(project, IntellijAzureActionManager.getInstance().getAction(actionId));
        this.source = null;
    }

    protected ActionNode(@Nonnull Project project, Action<T> action, @Nullable T source) {
        super(project, action);
        this.source = source;
    }

    protected ActionNode(@Nonnull Project project, Action.Id<T> actionId, @Nullable T source) {
        super(project, IntellijAzureActionManager.getInstance().getAction(actionId));
        this.source = source;
    }

    @Override
    public @Nonnull Collection<? extends AbstractTreeNode<?>> getChildren() {
        return Collections.emptyList();
    }

    @Override
    protected void update(@Nonnull PresentationData presentation) {
        final IView.Label view = this.getValue().getView(this.source);
        presentation.addText(StringUtils.capitalize(view.getLabel()), SimpleTextAttributes.LINK_ATTRIBUTES);
        presentation.setTooltip(view.getDescription());
    }

    @Override
    public void onClicked(Object event) {
        this.getValue().handle(this.source, event);
    }

    @Nullable
    @Override
    public IActionGroup getActionGroup() {
        return new IActionGroup() {
            @Override
            public IView.Label getView() {
                return null;
            }

            @Override
            public List<Object> getActions() {
                return Arrays.asList(ActionNode.this.getValue());
            }

            @Override
            public void addAction(Object action) {
                // do nothing here
            }
        };
    }

    @Override
    public @Nullable Object getData(@Nonnull String dataId) {
        return StringUtils.equalsIgnoreCase(dataId, Action.SOURCE) ? this.source : null;
    }

    @Override
    public int getWeight() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String toString() {
        return this.getValue().toString();
    }
}
