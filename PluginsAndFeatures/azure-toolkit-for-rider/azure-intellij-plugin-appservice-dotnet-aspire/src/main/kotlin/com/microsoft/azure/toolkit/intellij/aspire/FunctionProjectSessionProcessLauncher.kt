/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.aspire

import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.aspire.generated.SessionModel
import com.jetbrains.rider.aspire.run.AspireHostConfiguration
import com.jetbrains.rider.aspire.sessionHost.projectLaunchers.SessionProcessLauncherExtension
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.configurations.RunnableProjectKinds
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.DotNetRuntime
import com.jetbrains.rider.runtime.RiderDotNetActiveRuntimeHost
import com.jetbrains.rider.runtime.dotNetCore.DotNetCoreRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension

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
        LOG.trace { "Starting run session for ${sessionModel.projectPath}" }

        val executable = getDotNetExecutable(
            sessionModel,
            hostRunConfiguration,
            true,
            project
        ) ?: return
        val runtime = getDotNetRuntime(executable, project) ?: return

        val projectName = Path(sessionModel.projectPath).nameWithoutExtension
        val aspireHostProjectPath = hostRunConfiguration?.let { Path(it.parameters.projectFilePath) }

        val profile = getRunProfile(
            sessionId,
            projectName,
            executable,
            runtime,
            sessionProcessEventListener,
            sessionProcessLifetime,
            aspireHostProjectPath
        )

        val environment = ExecutionEnvironmentBuilder
            .createOrNull(project, DefaultRunExecutor.getRunExecutorInstance(), profile)
            ?.build()
        if (environment == null) {
            LOG.warn("Unable to create run execution environment")
            return
        }

        withContext(Dispatchers.EDT) {
            environment.runner.execute(environment)
        }
    }

    override suspend fun launchDebugProcess(
        sessionId: String,
        sessionModel: SessionModel,
        sessionProcessEventListener: ProcessListener,
        sessionProcessLifetime: Lifetime,
        hostRunConfiguration: AspireHostConfiguration?,
        project: Project
    ) {
        LOG.trace { "Starting debug session for project ${sessionModel.projectPath}" }

        val executable = getDotNetExecutable(
            sessionModel,
            hostRunConfiguration,
            true,
            project
        ) ?: return
        val runtime = getDotNetRuntime(executable, project) ?: return

        val projectName = Path(sessionModel.projectPath).nameWithoutExtension
        val aspireHostProjectPath = hostRunConfiguration?.let { Path(it.parameters.projectFilePath) }

        val profile = getDebugProfile(
            sessionId,
            projectName,
            executable,
            runtime,
            sessionProcessEventListener,
            sessionProcessLifetime,
            aspireHostProjectPath
        )
        val debugRunner = ProgramRunner.findRunnerById(FunctionProjectSessionDebugProgramRunner.ID)
        if (debugRunner == null) {
            LOG.warn("Unable to find runner: ${FunctionProjectSessionDebugProgramRunner.ID}")
            return
        }

        val environment = ExecutionEnvironmentBuilder
            .createOrNull(project, DefaultDebugExecutor.getDebugExecutorInstance(), profile)
            ?.runner(debugRunner)
            ?.build()
        if (environment == null) {
            LOG.warn("Unable to create debug execution environment")
            return
        }

        withContext(Dispatchers.EDT) {
            environment.runner.execute(environment)
        }
    }

    private suspend fun getDotNetExecutable(
        sessionModel: SessionModel,
        hostRunConfiguration: AspireHostConfiguration?,
        addBrowserAction: Boolean,
        project: Project
    ): DotNetExecutable? {
        val factory = FunctionSessionExecutableFactory.getInstance(project)
        val executable = factory.createExecutable(sessionModel, hostRunConfiguration, addBrowserAction)
        if (executable == null) {
            LOG.warn("Unable to create executable for project: ${sessionModel.projectPath}")
        }

        return executable
    }

    private fun getDotNetRuntime(executable: DotNetExecutable, project: Project): DotNetCoreRuntime? {
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

    private fun getRunProfile(
        sessionId: String,
        projectName: String,
        dotnetExecutable: DotNetExecutable,
        dotnetRuntime: DotNetCoreRuntime,
        sessionProcessEventListener: ProcessListener,
        sessionProcessLifetime: Lifetime,
        aspireHostProjectPath: Path?
    ) = FunctionProjectSessionRunProfile(
        sessionId,
        projectName,
        dotnetExecutable,
        dotnetRuntime,
        sessionProcessEventListener,
        sessionProcessLifetime,
        aspireHostProjectPath
    )

    private fun getDebugProfile(
        sessionId: String,
        projectName: String,
        dotnetExecutable: DotNetExecutable,
        dotnetRuntime: DotNetCoreRuntime,
        sessionProcessEventListener: ProcessListener,
        sessionProcessLifetime: Lifetime,
        aspireHostProjectPath: Path?
    ) = FunctionProjectSessionDebugProfile(
        sessionId,
        projectName,
        dotnetExecutable,
        dotnetRuntime,
        sessionProcessEventListener,
        sessionProcessLifetime,
        aspireHostProjectPath
    )
}