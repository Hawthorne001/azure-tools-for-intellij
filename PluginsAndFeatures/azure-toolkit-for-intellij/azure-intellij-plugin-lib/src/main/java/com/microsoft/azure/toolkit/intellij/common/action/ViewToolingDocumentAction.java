/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import static com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor.OPEN_URL;
import static com.microsoft.azure.toolkit.lib.common.action.Action.EMPTY_PLACE;
import static com.microsoft.azure.toolkit.lib.common.action.Action.PLACE;

public class ViewToolingDocumentAction extends AnAction implements DumbAware {
    public static final String ID = "AzureToolkit.ViewToolingDocument";
    public static final String DOC_URL = "https://aka.ms/javaToolingDoc";

    public ViewToolingDocumentAction() {
        super(AllIcons.Actions.Annotate);
    }

    @Override
    @AzureOperation(name = "user/common.view_tooling_documents")
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        OperationContext.current().setTelemetryProperty(PLACE, StringUtils.firstNonBlank(anActionEvent.getPlace(), EMPTY_PLACE));
        AzureActionManager.getInstance().getAction(OPEN_URL).handle(DOC_URL);
    }
}
