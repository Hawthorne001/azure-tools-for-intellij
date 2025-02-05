/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.aspire

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.rider.aspire.run.AspireHostConfiguration
import com.jetbrains.rider.aspire.run.AspireHostConfigurationType
import com.jetbrains.rider.run.configurations.RunnableProjectKinds
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.DotNetRuntime
import com.jetbrains.rider.runtime.RiderDotNetActiveRuntimeHost
import com.jetbrains.rider.runtime.dotNetCore.DotNetCoreRuntime

//TODO: Remove after an appropriate EAP

private val LOG = Logger.getInstance("#com.microsoft.azure.toolkit.intellij.aspire.TempFunctionProjectSessionProcessLauncherUtils")

fun getAspireHostRunConfiguration(name: String?, project: Project): AspireHostConfiguration? {
    if (name == null) return null

    val configurationType = ConfigurationTypeUtil.findConfigurationType(AspireHostConfigurationType::class.java)
    val runConfiguration = RunManager.getInstance(project)
        .getConfigurationsList(configurationType)
        .singleOrNull { it is AspireHostConfiguration && it.name == name }
    if (runConfiguration == null) {
        LOG.warn("Unable to find Aspire run configuration type: $name")
    }

    return runConfiguration as? AspireHostConfiguration
}

fun getDotNetRuntime(executable: DotNetExecutable, project: Project): DotNetCoreRuntime? {
    val runtime = DotNetRuntime.detectRuntimeForProject(
        project,
        RunnableProjectKinds.DotNetCore,
        RiderDotNetActiveRuntimeHost.getInstance(project),
        executable.runtimeType,
        executable.exePath,
        executable.projectTfm
    )?.runtime as? DotNetCoreRuntime
    if (runtime == null) {
        LOG.warn("Unable to detect runtime for executable: ${executable.exePath}")
    }

    return runtime
}