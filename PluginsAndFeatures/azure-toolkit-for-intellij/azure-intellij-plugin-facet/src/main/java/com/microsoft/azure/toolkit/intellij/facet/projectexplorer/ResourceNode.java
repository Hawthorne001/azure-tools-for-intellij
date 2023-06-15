/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet.projectexplorer;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class ResourceNode extends AbstractTreeNode<Node<?>> implements IAzureFacetNode, Node.ChildrenChangedListener, Node.ViewChangedListener {
    public ResourceNode(@Nonnull Project project, final Node<?> node) {
        super(project, node);
        node.setViewChangedListener(this);
        node.setChildrenChangedListener(this);
    }

    @Override
    @Nonnull
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
        final Node<?> node = this.getValue();
        final ArrayList<AbstractTreeNode<?>> children = new ArrayList<>(node.getChildren().stream().map(n -> new ResourceNode(this.getProject(), n)).toList());
        if (node.hasMoreChildren()) {
            final Action<Object> loadMoreAction = new Action<>(Action.Id.of("user/common.load_more"))
                .withHandler(i -> node.loadMoreChildren())
                .withLabel("load more")
                .withAuthRequired(true);
            children.add(new ActionNode<>(this.myProject, loadMoreAction));
        }
        return children;
    }

    @Override
    protected void update(@Nonnull final PresentationData presentation) {
        final Node<?> node = this.getValue();
        final Node.View view = node.getView();
        presentation.setIcon(IntelliJAzureIcons.getIcon(view.getIcon()));
        presentation.addText(view.getLabel(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        presentation.setTooltip(view.getTips());
        Optional.ofNullable(view.getDescription()).ifPresent(d -> presentation.addText(" " + d, SimpleTextAttributes.GRAYED_ATTRIBUTES));
    }

    @Override
    @Nullable
    public Object getData(@Nonnull String dataId) {
        if (StringUtils.equalsIgnoreCase(dataId, Action.SOURCE)) {
            return Optional.ofNullable(getValue()).map(Node::getData).orElse(null);
        }
        return null;
    }

    @Override
    public void onDoubleClicked(Object event) {
        Optional.ofNullable(this.getValue()).ifPresent(n -> n.doubleClick(event));
    }

    @Override
    public void onClicked(Object event) {
        Optional.ofNullable(this.getValue()).ifPresent(n -> n.click(event));
    }

    @Override
    @Nullable
    public IActionGroup getActionGroup() {
        return Optional.ofNullable(getValue()).map(Node::getActions).orElse(null);
    }

    @Override
    public void onViewChanged() {
        rerender(false);
    }

    @Override
    public void onChildrenChanged(boolean... incremental) {
        rerender(true);
    }

    @Override
    public String toString() {
        return this.getValue().buildLabel();
    }
}