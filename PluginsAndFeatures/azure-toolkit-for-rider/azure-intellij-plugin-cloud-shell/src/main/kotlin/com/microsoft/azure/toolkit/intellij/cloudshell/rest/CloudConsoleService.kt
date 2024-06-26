/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.rest

import com.google.gson.Gson
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*

@Service(Service.Level.APP)
class CloudConsoleService : Disposable {
    companion object {
        fun getInstance() = service<CloudConsoleService>()
    }

    private val client = HttpClient(CIO)

    suspend fun getUserSettings(resourceManagerEndpoint: String, accessToken: String): CloudConsoleUserSettings {
        val body: String =
            client.get("${resourceManagerEndpoint}providers/Microsoft.Portal/userSettings/cloudconsole?api-version=2023-02-01-preview") {
                headers {
                    append(HttpHeaders.Accept,ContentType.Application.Json.toString())
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            }.body()

        return Gson().fromJson(body, CloudConsoleUserSettings::class.java)
    }

    override fun dispose() = client.close()
}