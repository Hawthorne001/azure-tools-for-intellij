/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.rider.model.ProjectOutput
import com.jetbrains.rider.model.RunnableProject
import com.jetbrains.rider.run.configurations.controls.LaunchProfile
import com.jetbrains.rider.run.configurations.launchSettings.LaunchSettingsJson
import com.jetbrains.rider.run.configurations.launchSettings.LaunchSettingsJsonService
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.localsettings.FunctionLocalSettings
import org.apache.http.client.utils.URIBuilder

internal fun getLaunchProfiles(runnableProject: RunnableProject): List<LaunchProfile> {
    val launchSettings = LaunchSettingsJsonService.loadLaunchSettings(runnableProject)
    return launchSettings?.profiles
        .orEmpty()
        .asSequence()
        .filter { it.value.commandName.equals("Project", true) }
        .map { (name, content) -> LaunchProfile(name, content) }
        .sortedBy { it.name }
        .toList()
}

internal fun getLaunchProfileByName(
    runnableProject: RunnableProject,
    launchProfileName: String?
): LaunchProfile? {
    if (launchProfileName == null) return null
    val launchProfiles = LaunchSettingsJsonService.loadLaunchSettings(runnableProject)
        ?.profiles
        ?.filter { it.value.commandName.equals("Project", true) }
        ?: return null
    val profileByName = launchProfiles[launchProfileName] ?: return null

    return LaunchProfile(launchProfileName, profileByName)
}

internal fun getArguments(profile: LaunchSettingsJson.Profile?, projectOutput: ProjectOutput?): String {
    val defaultArguments = projectOutput?.defaultArguments
    return if (defaultArguments.isNullOrEmpty()) profile?.commandLineArgs ?: ""
    else {
        val parametersList = ParametersListUtil.join(defaultArguments)
        val commandLineArgs = profile?.commandLineArgs
        if (commandLineArgs != null) {
            "$parametersList $commandLineArgs"
        } else {
            parametersList
        }
    }
}

internal fun getWorkingDirectory(profile: LaunchSettingsJson.Profile?, projectOutput: ProjectOutput?): String {
    return profile?.workingDirectory ?: projectOutput?.workingDirectory ?: ""
}

internal fun getEnvironmentVariables(profile: LaunchSettingsJson.Profile): Map<String, String> {
    val environmentVariables = profile.environmentVariables
        ?.mapNotNull { it.value?.let { value -> it.key to value } }
        ?.toMap()
        ?: mapOf()

    return environmentVariables
}

private val portRegex = Regex("(--port|-p) (\\d+)", RegexOption.IGNORE_CASE)

internal fun getApplicationUrl(
    profile: LaunchSettingsJson.Profile,
    projectOutput: ProjectOutput?,
    functionLocalSettings: FunctionLocalSettings?
): String {
    val applicationUrl = profile.applicationUrl
    if (!applicationUrl.isNullOrEmpty()) {
        return applicationUrl.substringBefore(';')
    }

    val arguments = getArguments(profile, projectOutput)
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