/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.projectexplorer;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@Slf4j
public class ExceptionNode extends AbstractAzureFacetNode<Throwable> {

    public ExceptionNode(@Nonnull Project project, final Throwable e) {
        super(project, e);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<? extends AbstractAzureFacetNode<?>> buildChildren() {
        final ArrayList<AbstractAzureFacetNode<?>> children = new ArrayList<>();
        final Throwable e = this.getValue();
        if (e instanceof AzureToolkitRuntimeException) {
            final Object[] actions = Optional.ofNullable(((AzureToolkitRuntimeException) e).getActions()).orElseGet(() -> new Object[0]);
            for (final Object action : actions) {
                if (action instanceof Action.Id) {
                    children.add(new ActionNode<>(this.getProject(), (Action.Id<Object>) action));
                } else if (action instanceof Action<?>) {
                    children.add(new ActionNode<>(this.getProject(), (Action<Object>) action));
                }
            }
        }
        return children;
    }

    @Override
    protected void buildView(@Nonnull final PresentationData presentation) {
        final String message = StringUtils.capitalize(StringUtils.firstNonBlank(this.getValue().getMessage(), "<no message>"));
        presentation.addText(message, SimpleTextAttributes.ERROR_ATTRIBUTES);
    }

    @Override
    public String toString() {
        return this.getValue().getMessage();
    }
}

