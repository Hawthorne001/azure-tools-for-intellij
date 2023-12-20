/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function

import com.microsoft.azure.toolkit.intellij.common.auth.IntelliJSecureStore
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils

private const val AZURE_FUNCTIONS_APP_SETTINGS = "Azure Functions App Settings"

// Known and supported list of tags from https://github.com/Azure/azure-functions-tooling-feed/blob/main/cli-feed-v4.json
val FUNCTIONS_CORE_TOOLS_KNOWN_SUPPORTED_VERSIONS = listOf("v2", "v3", "v4")

// Latest supported version by the Azure Toolkit for Rider
const val FUNCTIONS_CORE_TOOLS_LATEST_SUPPORTED_VERSION = "v4"

fun saveAppSettingsToSecurityStorage(key: String?, appSettings: Map<String, String>) {
    if (key.isNullOrEmpty()) {
        return
    }

    val appSettingsJsonValue = JsonUtils.toJson(appSettings)
    IntelliJSecureStore.getInstance().savePassword(AZURE_FUNCTIONS_APP_SETTINGS, key, null, appSettingsJsonValue)
}