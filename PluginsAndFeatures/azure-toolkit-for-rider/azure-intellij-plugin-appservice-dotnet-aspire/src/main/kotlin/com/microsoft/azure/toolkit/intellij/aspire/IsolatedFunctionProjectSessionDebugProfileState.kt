/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.aspire

import com.intellij.execution.CantRunException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.impl.ProcessListUtil
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.system.CpuArch
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.debugger.DebuggerHelperHost
import com.jetbrains.rider.debugger.DebuggerWorkerProcessHandler
import com.jetbrains.rider.model.debuggerHelper.PlatformArchitecture
import com.jetbrains.rider.run.AttachDebugProcessAwareProfileStateBase
import com.jetbrains.rider.run.ConsoleKind
import com.jetbrains.rider.run.DebugProfileStateBase
import com.jetbrains.rider.run.IDotNetDebugProfileState
import com.jetbrains.rider.run.configurations.RequiresPreparationRunProfileState
import com.jetbrains.rider.run.dotNetCore.DotNetCoreAttachProfileState
import com.jetbrains.rider.run.kill
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.dotNetCore.DotNetCoreRuntime
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.FunctionHostDebugLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IsolatedFunctionProjectSessionDebugProfileState(
    private val sessionId: String,
    private val dotnetExecutable: DotNetExecutable,
    private val dotnetRuntime: DotNetCoreRuntime,
    private val sessionProcessEventListener: ProcessListener,
    private val sessionProcessLifetime: Lifetime
) : IDotNetDebugProfileState, RequiresPreparationRunProfileState {
    companion object {
        private val LOG = logger<IsolatedFunctionProjectSessionDebugProfileState>()
    }

    private lateinit var functionHostExecutionResult: ExecutionResult
    private lateinit var wrappedState: AttachDebugProcessAwareProfileStateBase

    override val attached = false
    override val consoleKind = ConsoleKind.Normal

    override suspend fun prepareExecution(environment: ExecutionEnvironment) {
        val (executionResult, pid) = launchFunctionHostWaitingForDebugger(environment)
        functionHostExecutionResult = executionResult

        sessionProcessLifetime.onTermination {
            if (!executionResult.processHandler.isProcessTerminated && !executionResult.processHandler.isProcessTerminated) {
                LOG.trace("Killing Function session process handler (id: $sessionId)")
                executionResult.processHandler.kill()
            }
        }

        val targetProcess = withContext(Dispatchers.IO) {
            ProcessListUtil.getProcessList().firstOrNull { it.pid == pid }
        } ?: return

        val processArchitecture = getPlatformArchitecture(sessionProcessLifetime, pid, environment.project)

        wrappedState = DotNetCoreAttachProfileState(
            targetProcess,
            environment,
            processArchitecture
        )
    }

    private suspend fun getPlatformArchitecture(lifetime: Lifetime, pid: Int, project: Project): PlatformArchitecture {
        if (SystemInfo.isWindows) {
            return DebuggerHelperHost.Companion
                .getInstance(project)
                .getProcessArchitecture(lifetime, pid)
        }

        return when (CpuArch.CURRENT) {
            CpuArch.X86 -> PlatformArchitecture.X86
            CpuArch.X86_64 -> PlatformArchitecture.X64
            CpuArch.ARM64 -> PlatformArchitecture.Arm64
            else -> PlatformArchitecture.Unknown
        }
    }

    private suspend fun launchFunctionHostWaitingForDebugger(environment: ExecutionEnvironment): Pair<ExecutionResult, Int> {
        val launcher = FunctionHostDebugLauncher.Companion.getInstance(environment.project)
        val (executionResult, pid) =
            withBackgroundProgress(environment.project, "Waiting for Azure Functions host to start...") {
                withContext(Dispatchers.Default) {
                    launcher.startProcessWaitingForDebugger(
                        dotnetExecutable,
                        dotnetRuntime,
                        sessionProcessEventListener
                    )
                }
            }

        if (executionResult.processHandler.isProcessTerminating || executionResult.processHandler.isProcessTerminated) {
            LOG.warn("Azure Functions host process terminated before the debugger could attach")

            Notification(
                "Azure AppServices",
                "Azure Functions - debug",
                "Azure Functions host process terminated before the debugger could attach.",
                NotificationType.ERROR
            )
                .notify(environment.project)

            throw CantRunException("Azure Functions host process terminated before the debugger could attach")
        }

        if (pid == null || pid == 0) {
            LOG.warn("Azure Functions host did not return isolated worker process id")

            Notification(
                "Azure AppServices",
                "Azure Functions - debug",
                "Azure Functions host did not return isolated worker process id. Could not attach the debugger. Check the process output for more information.",
                NotificationType.ERROR
            )
                .notify(environment.project)

            throw CantRunException("Azure Functions host did not return isolated worker process id")
        }

        return executionResult to pid
    }

    override suspend fun createModelStartInfo(lifetime: Lifetime) =
        wrappedState.createModelStartInfo(lifetime)

    override suspend fun createWorkerRunInfo(
        lifetime: Lifetime,
        helper: DebuggerHelperHost,
        port: Int
    ) = DebugProfileStateBase.createWorkerRunInfoForLauncherInfo(
        consoleKind,
        port,
        getLauncherInfo(lifetime, helper),
        dotnetExecutable.executableType,
        dotnetExecutable.usePty
    )

    override suspend fun getLauncherInfo(
        lifetime: Lifetime,
        helper: DebuggerHelperHost
    ) = wrappedState.getLauncherInfo(lifetime, helper)

    override suspend fun execute(
        executor: Executor,
        runner: ProgramRunner<*>,
        workerProcessHandler: DebuggerWorkerProcessHandler
    ) = functionHostExecutionResult

    override suspend fun execute(
        executor: Executor,
        runner: ProgramRunner<*>,
        workerProcessHandler: DebuggerWorkerProcessHandler,
        lifetime: Lifetime
    ) = functionHostExecutionResult

    override fun execute(
        executor: Executor?,
        runner: ProgramRunner<*>
    ) = functionHostExecutionResult
}