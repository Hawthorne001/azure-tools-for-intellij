/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.aspire

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.rider.run.configurations.RunnableProjectKinds
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.DotNetRuntime
import com.jetbrains.rider.runtime.RiderDotNetActiveRuntimeHost
import com.jetbrains.rider.runtime.dotNetCore.DotNetCoreRuntime

//TODO: Remove after an appropriate EAP

private val LOG = Logger.getInstance("#com.microsoft.azure.toolkit.intellij.aspire.TempFunctionProjectSessionProcessLauncherUtils")

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