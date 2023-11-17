/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.function.coreTools

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.ZipUtil
import com.intellij.util.system.CpuArch
import com.intellij.util.text.VersionComparatorUtil
import com.jetbrains.rd.util.concurrentMapOf
import com.microsoft.azure.toolkit.intellij.legacy.function.settings.AzureFunctionSettings
import java.io.File
import java.io.IOException
import java.net.UnknownHostException

@Service
class FunctionsCoreToolsManager {
    companion object {
        fun getInstance(): FunctionsCoreToolsManager = service()

        private val LOG = logger<FunctionsCoreToolsManager>()
    }

    private val releaseCache = concurrentMapOf<String, FunctionCoreToolsRelease>()

    suspend fun demandCoreToolsPathForVersion(
        azureFunctionsVersion: String,
        releaseFeedUrl: String,
        allowDownload: Boolean
    ): String? {
        val downloadRoot = resolveDownloadRoot()

        if (allowDownload && Registry.`is`("azure.function_app.core_tools.feed.enabled")) {
            ensureReleaseCacheFromFeed(releaseFeedUrl)

            val coreToolsPath = resolveDownloadInfoForRelease(azureFunctionsVersion, downloadRoot)
                ?.let { ensureReleaseDownloaded(it) }

            if (coreToolsPath != null) return coreToolsPath
        }

        val coreToolsPath = tryResolveExistingCoreToolsPath(azureFunctionsVersion, downloadRoot)
        if (coreToolsPath != null) return coreToolsPath

        return null
    }

    private fun resolveDownloadRoot(): File {
        val settings = AzureFunctionSettings.getInstance()
        val downloadRoot = File(settings.functionDownloadPath)

        if (!downloadRoot.exists()) {
            try {
                downloadRoot.mkdir()
            } catch (e: Exception) {
                LOG.error("Error while creating download root: ${downloadRoot.path}", e)
            }
        }

        return downloadRoot
    }

    private suspend fun ensureReleaseCacheFromFeed(releaseFeedUrl: String) {
        if (releaseCache.isNotEmpty()) return

        val releaseFilter = getReleaseFilter()

        LOG.debug("Azure Core Tools release filter: $releaseFilter")

        try {
            val feed = FunctionCoreToolsReleaseFeedService.getInstance().getReleaseFeed(releaseFeedUrl)

            val releaseTags = feed.tags
                .toSortedMap()
                .filterValues { !it.releaseQuality.isNullOrEmpty() && !it.release.isNullOrEmpty() }

            for ((releaseTagName, releaseTag) in releaseTags) {
                val releaseFromTag = releaseTag.release ?: continue
                val release = feed.releases[releaseFromTag] ?: continue

                val releaseCoreTools = getReleaseCoreTool(release, releaseFilter) ?: continue

                LOG.debug("Release for Azure Core Tools version ${releaseTagName.lowercase()}: ${releaseTag.release}; ${releaseCoreTools.downloadLink}")

                val releaseKey = releaseTagName.lowercase()
                releaseCache.putIfAbsent(
                    releaseKey,
                    FunctionCoreToolsRelease(releaseKey, releaseTag.release, releaseCoreTools.downloadLink ?: "")
                )
            }
        } catch (e: UnknownHostException) {
            LOG.warn("Could not download from Azure Functions Core Tools release feed URL at $releaseFeedUrl: $e")
        } catch (e: IOException) {
            LOG.warn("Could not download from Azure Functions Core Tools release feed URL at $releaseFeedUrl: $e")
        }
    }

    private fun getReleaseFilter() = when {
        SystemInfo.isWindows && CpuArch.isIntel64() -> AzureCoreToolsFeedReleaseFilter(
            "Windows",
            listOf("x64"),
            listOf("minified", "full")
        )

        SystemInfo.isWindows && CpuArch.isArm64() -> AzureCoreToolsFeedReleaseFilter(
            "Windows",
            listOf("arm64", "x64"),
            listOf("minified", "full")
        )

        SystemInfo.isWindows -> AzureCoreToolsFeedReleaseFilter(
            "Windows",
            listOf("x86"),
            listOf("minified", "full")
        )

        SystemInfo.isMac && CpuArch.isArm64() -> AzureCoreToolsFeedReleaseFilter(
            "MacOS",
            listOf("arm64", "x64"),
            listOf("full")
        )

        SystemInfo.isMac -> AzureCoreToolsFeedReleaseFilter(
            "MacOS",
            listOf("x64"),
            listOf("full")
        )

        SystemInfo.isLinux -> AzureCoreToolsFeedReleaseFilter(
            "Linux",
            listOf("x64"),
            listOf("full")
        )

        else -> AzureCoreToolsFeedReleaseFilter(
            "Unknown",
            listOf("x64"),
            listOf("full")
        )
    }

    private fun getReleaseCoreTool(release: Release, releaseFilter: AzureCoreToolsFeedReleaseFilter) =
        release.coreTools
            .asSequence()
            .filter {
                it.os.equals(releaseFilter.os, ignoreCase = true) &&
                        !it.downloadLink.isNullOrEmpty()
            }
            .sortedWith(
                compareBy<ReleaseCoreTool> {
                    releaseFilter.architectures.indexOfFirst { architecture ->
                        it.architecture.equals(architecture, ignoreCase = true)
                    }.let { rank -> if (rank >= 0) rank else 9999 }
                }.thenBy {
                    releaseFilter.sizes.indexOfFirst { size ->
                        it.size.equals(size, ignoreCase = true)
                    }.let { rank -> if (rank >= 0) rank else 9999 }
                })
            .firstOrNull()

    private fun resolveDownloadInfoForRelease(
        azureFunctionsVersion: String,
        downloadRoot: File
    ): FunctionCoreToolsDownloadInfo? {
        val releaseInfo = releaseCache[azureFunctionsVersion.lowercase()]
        if (releaseInfo == null) {
            LOG.warn("Could not determine Azure Functions Core Tools release. Azure Functions version: '$azureFunctionsVersion'")
            return null
        }

        val downloadFolderForTag = downloadRoot.resolve(releaseInfo.functionsVersion)
        val downloadFolderForTagRelease = downloadFolderForTag.resolve(releaseInfo.coreToolsVersion)

        LOG.debug(
            "Found Azure Functions Core Tools release from feed. " +
                    "Azure Functions version: '${releaseInfo.functionsVersion}'; " +
                    "Core Tools Version: ${releaseInfo.coreToolsVersion}; " +
                    "Expected download path: ${downloadFolderForTagRelease.path}"
        )

        return FunctionCoreToolsDownloadInfo(
            downloadFolderForTag,
            downloadFolderForTagRelease,
            releaseInfo
        )
    }

    private fun ensureReleaseDownloaded(downloadInfo: FunctionCoreToolsDownloadInfo): String? {
        if (downloadInfo.downloadFolderForTagAndRelease.exists()) {
            return downloadInfo.downloadFolderForTagAndRelease.path
        }
        downloadRelease(downloadInfo)

        if (downloadInfo.downloadFolderForTagAndRelease.exists()) {
            return downloadInfo.downloadFolderForTagAndRelease.path
        }

        return null
    }

    private fun downloadRelease(downloadInfo: FunctionCoreToolsDownloadInfo) {
        val tempFile = FileUtil.createTempFile(
            File(FileUtil.getTempDirectory()),
            "AzureFunctions-${downloadInfo.release.functionsVersion}-${downloadInfo.release.coreToolsVersion}",
            "download",
            true,
            true
        )

        HttpRequests
            .request(downloadInfo.release.coreToolsArtifactUrl)
            .saveToFile(tempFile, null)

        try {
            if (downloadInfo.downloadFolderForTag.exists()) {
                downloadInfo.downloadFolderForTag.deleteRecursively()
            }
        } catch (e: Exception) {
            LOG.error("Error while removing directory ${downloadInfo.downloadFolderForTag.path}", e)
        }

        try {
            ZipUtil.extract(tempFile.toPath(), downloadInfo.downloadFolderForTagAndRelease.toPath(), null)
        } catch (e: Exception) {
            LOG.error(
                "Error while extracting ${tempFile.path} to ${downloadInfo.downloadFolderForTagAndRelease.path}",
                e
            )
        }

        try {
            if (tempFile.exists()) tempFile.delete()
        } catch (e: Exception) {
            LOG.error("Error while removing temporary file ${tempFile.path}", e)
        }
    }

    private fun tryResolveExistingCoreToolsPath(azureFunctionsVersion: String, downloadRoot: File): String? {
        val downloadFolderForTag = downloadRoot.resolve(azureFunctionsVersion.lowercase())
        if (!downloadFolderForTag.exists()) return null

        val downloadFolderForTagRelease = downloadFolderForTag.listFiles(File::isDirectory)
            ?.asSequence()
            ?.sortedWith { first, second -> VersionComparatorUtil.compare(first?.name, second?.name) }
            ?.lastOrNull { it.exists() && it.listFiles { file -> file.isFunctionTool() }?.any() == true }

        if (downloadFolderForTagRelease != null) {
            LOG.debug(
                "Found existing Azure Functions Core Tools path. " +
                        "Azure Functions version: '${azureFunctionsVersion.lowercase()}'; " +
                        "Download path: ${downloadFolderForTagRelease.path}"
            )

            return downloadFolderForTagRelease.path
        }

        LOG.warn(
            "Could not determine existing Azure Functions Core Tools path. " +
                    "Azure Functions version: '${azureFunctionsVersion.lowercase()}'"
        )

        return null
    }

    private data class AzureCoreToolsFeedReleaseFilter(
        val os: String,
        val architectures: List<String>,
        val sizes: List<String>
    )

    private data class FunctionCoreToolsRelease(
        val functionsVersion: String,
        val coreToolsVersion: String,
        val coreToolsArtifactUrl: String
    )

    private data class FunctionCoreToolsDownloadInfo(
        val downloadFolderForTag: File,
        val downloadFolderForTagAndRelease: File,
        val release: FunctionCoreToolsRelease
    )
}