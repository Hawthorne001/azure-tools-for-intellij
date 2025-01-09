/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.utils

import com.jetbrains.rider.model.RunnableProject
import com.jetbrains.rider.run.configurations.controls.LaunchProfile
import com.jetbrains.rider.run.configurations.launchSettings.LaunchSettingsJsonService
import kotlin.collections.asSequence
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.orEmpty

suspend fun LaunchSettingsJsonService.getProjectLaunchProfiles(runnableProject: RunnableProject): List<LaunchProfile> {
    val launchSettings = loadLaunchSettingsSuspend(runnableProject) ?: return emptyList()

    return launchSettings
        .profiles
        .orEmpty()
        .asSequence()
        .filter { it.value.commandName.equals("Project", true) }
        .map { (name, content) -> LaunchProfile(name, content) }
        .sortedBy { it.name }
        .toList()
}

suspend fun LaunchSettingsJsonService.getProjectLaunchProfileByName(
    runnableProject: RunnableProject,
    launchProfileName: String?
): LaunchProfile? {
    val profiles = getProjectLaunchProfiles(runnableProject)
    return profiles.find { it.name == launchProfileName }
}