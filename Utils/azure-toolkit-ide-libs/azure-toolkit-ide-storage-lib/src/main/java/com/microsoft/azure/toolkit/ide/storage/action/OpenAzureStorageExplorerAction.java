/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.ide.storage.action;

import com.microsoft.azure.toolkit.ide.storage.action.explorer.AbstractAzureStorageExplorerHandler;
import com.microsoft.azure.toolkit.ide.storage.action.explorer.MacOSOpenAzureStorageExplorerHandler;
import com.microsoft.azure.toolkit.ide.storage.action.explorer.WindowsOpenAzureStorageExplorerHandler;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import org.apache.commons.lang3.SystemUtils;

public class OpenAzureStorageExplorerAction {
    private final AbstractAzureStorageExplorerHandler handler;

    public OpenAzureStorageExplorerAction() {
        this.handler = SystemUtils.IS_OS_WINDOWS ? new WindowsOpenAzureStorageExplorerHandler() : new MacOSOpenAzureStorageExplorerHandler();
    }

    public void openResource(final StorageAccount storageAccount) {
        this.handler.openResource(storageAccount);
    }
}
