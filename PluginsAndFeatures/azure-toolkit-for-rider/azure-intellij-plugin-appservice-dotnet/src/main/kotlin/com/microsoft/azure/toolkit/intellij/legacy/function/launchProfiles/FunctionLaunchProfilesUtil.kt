/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles

import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.rider.model.ProjectOutput
import com.jetbrains.rider.run.configurations.launchSettings.LaunchSettingsJson
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.FunctionLocalSettings
import org.apache.http.client.utils.URIBuilder

internal fun getArguments(profile: LaunchSettingsJson.Profile?, projectOutput: ProjectOutput?) = buildString {
    val defaultArguments = projectOutput?.defaultArguments
    val commandLineArgs = profile?.commandLineArgs

    if (defaultArguments.isNullOrEmpty()) {
        append(commandLineArgs ?: "")
    } else {
        if (defaultArguments.isNotEmpty()) {
            append(ParametersListUtil.join(defaultArguments))
            append(" ")
        }
        if (commandLineArgs != null) {
            append(commandLineArgs)
        }
    }
}

fun getWorkingDirectory(profile: LaunchSettingsJson.Profile?, projectOutput: ProjectOutput?): String {
    return profile?.workingDirectory ?: projectOutput?.workingDirectory ?: ""
}

internal fun getEnvironmentVariables(profile: LaunchSettingsJson.Profile?) = profile
    ?.environmentVariables
    ?.mapNotNull { it.value?.let { value -> it.key to value } }
    ?.toMap()
    ?: emptyMap()

private val portRegex = Regex("(--port|-p) (\\d+)", RegexOption.IGNORE_CASE)

internal fun getApplicationUrl(
    profile: LaunchSettingsJson.Profile?,
    projectOutput: ProjectOutput?,
    functionLocalSettings: FunctionLocalSettings?
): String {
    val arguments = getArguments(profile, projectOutput)
    return getApplicationUrl(profile, arguments, functionLocalSettings)
}

fun getApplicationUrl(
    profile: LaunchSettingsJson.Profile?,
    arguments: String,
    functionLocalSettings: FunctionLocalSettings?
): String {
    val applicationUrl = profile?.applicationUrl
    if (!applicationUrl.isNullOrEmpty()) {
        return applicationUrl.substringBefore(';')
    }

    if (arguments.isNotEmpty()) {
        val argumentPort = portRegex.find(arguments)?.groupValues?.getOrNull(2)?.toIntOrNull()
        if (argumentPort != null) {
            return URIBuilder("http://localhost").setPort(argumentPort).build().toString()
        }
    }

    val localHttpPort = functionLocalSettings?.host?.localHttpPort
    if (localHttpPort != null) {
        return URIBuilder("http://localhost").setPort(localHttpPort).build().toString()
    }

    return ""
}