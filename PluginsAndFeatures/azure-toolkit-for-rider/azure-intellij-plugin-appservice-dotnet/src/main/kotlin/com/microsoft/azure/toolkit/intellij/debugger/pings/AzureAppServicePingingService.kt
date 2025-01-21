/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.debugger.pings

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.impl.XDebugSessionImpl
import com.jetbrains.rd.util.concurrentMapOf
import com.jetbrains.rider.run.AttachSshDebugProfileStateBase
import com.jetbrains.rider.util.idea.getService
import com.microsoft.azure.toolkit.intellij.debugger.attachHosts.AppServiceAttachHost
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
class AzureAppServicePingingService(private val cs: CoroutineScope) {
    private val map = concurrentMapOf<XDebugProcess, FunctionPinger>()

    companion object {
        fun getInstance(project: Project) = project.getService<AzureAppServicePingingService>()
    }

    fun debuggingStarted(debugProcess: XDebugProcess) {
        val functionApp = getFunctionApp(debugProcess) ?: return

        map.computeIfAbsent(debugProcess) {
            FunctionPinger(functionApp, cs).apply { start() }
        }
    }

    fun debuggingStopped(debugProcess: XDebugProcess) {
        map.remove(debugProcess)?.stop()
    }

    private fun getFunctionApp(debugProcess: XDebugProcess): FunctionAppBase<*, *, *>? {
        val state = (debugProcess.session as? XDebugSessionImpl)
            ?.executionEnvironment
            ?.state as? AttachSshDebugProfileStateBase ?: return null

        return (state.attachHost as? AppServiceAttachHost<FunctionApp>)?.appServiceApp
    }
}

