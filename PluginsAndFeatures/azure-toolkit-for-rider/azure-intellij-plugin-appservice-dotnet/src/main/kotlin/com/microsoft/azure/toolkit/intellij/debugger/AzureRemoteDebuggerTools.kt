/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.debugger

import com.intellij.openapi.project.Project
import com.intellij.remote.RemoteCredentials
import com.jetbrains.rider.PathInfo
import com.jetbrains.rider.debugger.attach.remoting.machines.RemoteMachine
import com.jetbrains.rider.debugger.attach.remoting.tools.remote.RemoteDebuggerToolsBase

class AzureRemoteDebuggerTools(remoteMachine: RemoteMachine, project: Project) :
    RemoteDebuggerToolsBase(remoteMachine, project) {

    companion object {
        suspend fun create(credentials: RemoteCredentials, project: Project): AzureRemoteDebuggerTools {
            val remoteMachine = RemoteMachine.guess(credentials)
            return AzureRemoteDebuggerTools(remoteMachine, project)
        }
    }

    override suspend fun getBaseDirectory(): PathInfo {
        return remoteMachine.getHomeFolder()
    }
}