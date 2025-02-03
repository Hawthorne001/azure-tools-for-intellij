/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.aspire

import com.intellij.execution.process.ProcessListener
import com.intellij.ide.browsers.StartBrowserSettings
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.aspire.generated.SessionModel
import com.jetbrains.rider.aspire.run.AspireHostConfiguration
import com.jetbrains.rider.aspire.sessionHost.projectLaunchers.SessionProcessLauncherExtension
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.runtime.DotNetExecutable
import kotlin.io.path.Path

class FunctionProjectSessionProcessLauncher : SessionProcessLauncherExtension {
    companion object {
        private val LOG = logger<FunctionProjectSessionProcessLauncher>()
    }

    override val priority = 3

    override suspend fun isApplicable(
        projectPath: String,
        project: Project
    ): Boolean {
        val path = Path(projectPath)
        val runnableProject = project.solution.runnableProjectsModel.findBySessionProject(path)
        return runnableProject != null
    }

    override suspend fun launchRunProcess(
        sessionId: String,
        sessionModel: SessionModel,
        sessionProcessEventListener: ProcessListener,
        sessionProcessLifetime: Lifetime,
        hostRunConfiguration: AspireHostConfiguration?,
        project: Project
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun launchDebugProcess(
        sessionId: String,
        sessionModel: SessionModel,
        sessionProcessEventListener: ProcessListener,
        sessionProcessLifetime: Lifetime,
        hostRunConfiguration: AspireHostConfiguration?,
        project: Project
    ) {
        TODO("Not yet implemented")
    }

    private suspend fun getDotNetExecutable(
        sessionModel: SessionModel,
        hostRunConfiguration: AspireHostConfiguration?,
        addBrowserAction: Boolean,
        project: Project
    ): Pair<DotNetExecutable, StartBrowserSettings?>? {
        val factory = FunctionSessionExecutableFactory.getInstance(project)
        val executable = factory.createExecutable(sessionModel, hostRunConfiguration, addBrowserAction)
        if (executable == null) {
            LOG.warn("Unable to create executable for project: ${sessionModel.projectPath}")
        }

        return executable
    }
}