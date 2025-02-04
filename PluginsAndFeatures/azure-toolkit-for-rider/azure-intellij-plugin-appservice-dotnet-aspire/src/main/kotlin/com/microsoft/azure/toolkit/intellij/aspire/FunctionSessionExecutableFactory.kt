/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.aspire

import com.intellij.ide.browsers.StartBrowserSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.project.Project
import com.intellij.util.execution.ParametersListUtil
import com.intellij.util.io.systemIndependentPath
import com.jetbrains.rider.aspire.generated.SessionEnvironmentVariable
import com.jetbrains.rider.aspire.generated.SessionModel
import com.jetbrains.rider.aspire.run.AspireHostConfiguration
import com.jetbrains.rider.aspire.settings.AspireSettings
import com.jetbrains.rider.aspire.util.MSBuildPropertyService
import com.jetbrains.rider.aspire.util.MSBuildPropertyService.ProjectRunProperties
import com.jetbrains.rider.aspire.util.getStartBrowserAction
import com.jetbrains.rider.model.RunnableProject
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.configurations.launchSettings.LaunchSettingsJson
import com.jetbrains.rider.run.configurations.launchSettings.LaunchSettingsJsonService
import com.jetbrains.rider.run.environment.ExecutableParameterProcessor
import com.jetbrains.rider.run.environment.ExecutableRunParameters
import com.jetbrains.rider.run.environment.ProjectProcessOptions
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.dotNetCore.DotNetCoreRuntimeType
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.getApplicationUrl
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.getWorkingDirectory
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.FunctionLocalSettings
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.FunctionCoreToolsExecutableService
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

@Service(Service.Level.PROJECT)
class FunctionSessionExecutableFactory(private val project: Project) {
    companion object {
        fun getInstance(project: Project): FunctionSessionExecutableFactory = project.service()
        private val LOG = logger<FunctionSessionExecutableFactory>()

        private const val DOTNET_LAUNCH_PROFILE = "DOTNET_LAUNCH_PROFILE"
    }

    suspend fun createExecutable(
        sessionModel: SessionModel,
        hostRunConfiguration: AspireHostConfiguration?,
        addBrowserAction: Boolean
    ): DotNetExecutable? {
        val sessionProjectPath = Path(sessionModel.projectPath)
        val runnableProject = project.solution.runnableProjectsModel.findBySessionProject(sessionProjectPath)
        return if (runnableProject != null) {
            getExecutableForRunnableProject(
                sessionProjectPath,
                runnableProject,
                sessionModel,
                hostRunConfiguration,
                addBrowserAction
            )
        } else {
            getExecutableForExternalProject(
                sessionProjectPath,
                sessionModel,
                hostRunConfiguration,
                addBrowserAction
            )
        }
    }

    private suspend fun getExecutableForRunnableProject(
        sessionProjectPath: Path,
        runnableProject: RunnableProject,
        sessionModel: SessionModel,
        hostRunConfiguration: AspireHostConfiguration?,
        addBrowserAction: Boolean
    ): DotNetExecutable? {
        val output = runnableProject.projectOutputs.firstOrNull()
        if (output == null) {
            LOG.warn("Unable to find output for runnable project $sessionProjectPath")
            return null
        }

        val coreToolsExecutable = FunctionCoreToolsExecutableService.getInstance(project)
            .getCoreToolsExecutable(sessionProjectPath, output.tfm?.presentableName)
        if (coreToolsExecutable == null) {
            LOG.warn("Unable to find Function core tools executable for runnable project $sessionProjectPath")
            return null
        }
        val coreToolsExecutablePath = coreToolsExecutable.executablePath.absolutePathString()

        val launchProfile = getLaunchProfile(sessionModel, runnableProject)
        val workingDirectory = getWorkingDirectory(launchProfile, output)
        val arguments = mergeArguments(sessionModel.args, output.defaultArguments, launchProfile?.commandLineArgs)
        val envs = mergeEnvironmentVariables(sessionModel.envs, launchProfile?.environmentVariables)

        val processOptions = ProjectProcessOptions(
            File(runnableProject.projectFilePath),
            File(workingDirectory)
        )
        val runParameters = ExecutableRunParameters(
            coreToolsExecutablePath,
            workingDirectory,
            arguments,
            envs,
            true,
            output.tfm
        )
        val executableParams = ExecutableParameterProcessor
            .getInstance(project)
            .processEnvironment(runParameters, processOptions)

        val browserSettings =
            getStartBrowserSettings(launchProfile, arguments, coreToolsExecutable.localSettings, hostRunConfiguration)
        val launchBrowser = AspireSettings.getInstance().doNotLaunchBrowserForProjects.not()
        val browserAction =
            if (launchBrowser && addBrowserAction && hostRunConfiguration != null) {
                getStartBrowserAction(hostRunConfiguration, browserSettings)
            } else {
                { _, _, _ -> }
            }

        LOG.trace { "Executable parameters for runnable project (${runnableProject.projectFilePath}): $executableParams" }
        LOG.trace { "Browser settings for runnable project (${runnableProject.projectFilePath}): $browserSettings" }

        return DotNetExecutable(
            executableParams.executablePath ?: coreToolsExecutablePath,
            executableParams.tfm ?: output.tfm,
            executableParams.workingDirectoryPath ?: workingDirectory,
            executableParams.commandLineArgumentString ?: arguments,
            useMonoRuntime = false,
            useExternalConsole = false,
            executableParams.environmentVariables,
            true,
            browserAction,
            coreToolsExecutablePath,
            "",
            true,
            DotNetCoreRuntimeType,
            usePty = false
        )
    }

    private suspend fun getExecutableForExternalProject(
        sessionProjectPath: Path,
        sessionModel: SessionModel,
        hostRunConfiguration: AspireHostConfiguration?,
        addBrowserAction: Boolean
    ): DotNetExecutable? {
        val propertyService = MSBuildPropertyService.getInstance(project)
        val properties = propertyService.getProjectRunProperties(sessionProjectPath)
        if (properties == null) {
            LOG.warn("Unable to get MSBuild properties for project $sessionProjectPath")
            return null
        }

        val coreToolsExecutable = FunctionCoreToolsExecutableService.getInstance(project)
            .getCoreToolsExecutable(sessionProjectPath, properties.targetFramework.presentableName)
        if (coreToolsExecutable == null) {
            LOG.warn("Unable to find Function core tools executable for external project $sessionProjectPath")
            return null
        }
        val coreToolsExecutablePath = coreToolsExecutable.executablePath.absolutePathString()

        val launchProfile = getLaunchProfile(sessionModel, sessionProjectPath)
        val workingDirectory = getWorkingDirectory(launchProfile, properties)
        val arguments = mergeArguments(sessionModel.args, properties.arguments, launchProfile?.commandLineArgs)
        val envs = mergeEnvironmentVariables(sessionModel.envs, launchProfile?.environmentVariables)

        val processOptions = ProjectProcessOptions(
            sessionProjectPath.toFile(),
            File(workingDirectory)
        )
        val runParameters = ExecutableRunParameters(
            coreToolsExecutablePath,
            workingDirectory,
            arguments,
            envs,
            true,
            properties.targetFramework
        )

        val executableParams = ExecutableParameterProcessor
            .getInstance(project)
            .processEnvironment(runParameters, processOptions)

        val browserSettings =
            getStartBrowserSettings(launchProfile, arguments, coreToolsExecutable.localSettings, hostRunConfiguration)
        val launchBrowser = AspireSettings.getInstance().doNotLaunchBrowserForProjects.not()
        val browserAction =
            if (launchBrowser && addBrowserAction && hostRunConfiguration != null) {
                getStartBrowserAction(hostRunConfiguration, browserSettings)
            } else {
                { _, _, _ -> }
            }

        LOG.trace { "Executable parameters for external project (${sessionProjectPath.absolutePathString()}): $executableParams" }
        LOG.trace { "Browser settings for external project (${sessionProjectPath.absolutePathString()}): $browserSettings" }

        return DotNetExecutable(
            executableParams.executablePath ?: coreToolsExecutablePath,
            executableParams.tfm ?: properties.targetFramework,
            executableParams.workingDirectoryPath ?: workingDirectory,
            executableParams.commandLineArgumentString ?: arguments,
            useMonoRuntime = false,
            useExternalConsole = false,
            executableParams.environmentVariables,
            true,
            browserAction,
            coreToolsExecutablePath,
            "",
            true,
            DotNetCoreRuntimeType
        )
    }

    //See: https://github.com/dotnet/aspire/blob/main/docs/specs/IDE-execution.md#launch-profile-processing-project-launch-configuration
    private suspend fun getLaunchProfile(
        sessionModel: SessionModel,
        runnableProject: RunnableProject
    ): LaunchSettingsJson.Profile? {
        val launchProfileKey = getLaunchProfileKey(sessionModel) ?: return null

        val launchSettings = LaunchSettingsJsonService
            .getInstance(project)
            .loadLaunchSettingsSuspend(runnableProject)
            ?: return null

        return launchSettings.profiles?.get(launchProfileKey)
    }

    //See: https://github.com/dotnet/aspire/blob/main/docs/specs/IDE-execution.md#launch-profile-processing-project-launch-configuration
    private suspend fun getLaunchProfile(
        sessionModel: SessionModel,
        sessionProjectPath: Path
    ): LaunchSettingsJson.Profile? {
        val launchProfileKey = getLaunchProfileKey(sessionModel) ?: return null

        val launchSettingsFile =
            LaunchSettingsJsonService.getLaunchSettingsFileForProject(sessionProjectPath.toFile()) ?: return null
        val launchSettings =
            LaunchSettingsJsonService.getInstance(project).loadLaunchSettingsSuspend(launchSettingsFile) ?: return null

        return launchSettings.profiles?.get(launchProfileKey)
    }

    private fun getLaunchProfileKey(sessionModel: SessionModel): String? {
        if (sessionModel.disableLaunchProfile) {
            LOG.trace { "Launch profile disabled" }
            return null
        }

        val launchProfileKey =
            if (!sessionModel.launchProfile.isNullOrEmpty()) {
                sessionModel.launchProfile
            } else {
                sessionModel.envs?.firstOrNull { it.key.equals(DOTNET_LAUNCH_PROFILE, false) }?.value
            }

        LOG.trace { "Found launch profile key: $launchProfileKey" }

        return launchProfileKey
    }

    internal fun getWorkingDirectory(profile: LaunchSettingsJson.Profile?, projectProperties: ProjectRunProperties?): String {
        return profile?.workingDirectory ?: projectProperties?.workingDirectory?.systemIndependentPath ?: ""
    }

    //See: https://github.com/dotnet/aspire/blob/main/docs/specs/IDE-execution.md#launch-profile-processing-project-launch-configuration
    private fun mergeArguments(
        sessionArguments: Array<String>?,
        defaultArguments: List<String>,
        launchProfileArguments: String?
    ) = buildString {
        if (defaultArguments.isNotEmpty()) {
            append(ParametersListUtil.join(defaultArguments))
            append(" ")
        }
        if (sessionArguments != null) {
            if (sessionArguments.isNotEmpty()) {
                append(ParametersListUtil.join(sessionArguments.toList()))
            }
        } else {
            if (!launchProfileArguments.isNullOrEmpty()) {
                append(launchProfileArguments)
            }
        }
    }

    //See: https://github.com/dotnet/aspire/blob/main/docs/specs/IDE-execution.md#launch-profile-processing-project-launch-configuration
    private fun mergeEnvironmentVariables(
        sessionEnvironmentVariables: Array<SessionEnvironmentVariable>?,
        launchProfileEnvironmentVariables: Map<String, String?>?
    ) = buildMap {
        if (launchProfileEnvironmentVariables?.isNotEmpty() == true) {
            launchProfileEnvironmentVariables.forEach {
                it.value?.let { value -> put(it.key, value) }
            }
        }

        if (sessionEnvironmentVariables?.isNotEmpty() == true) {
            sessionEnvironmentVariables.associateTo(this) { it.key to it.value }
        }
    }

    private fun getStartBrowserSettings(
        launchProfile: LaunchSettingsJson.Profile?,
        arguments: String,
        functionLocalSettings: FunctionLocalSettings?,
        hostRunConfiguration: AspireHostConfiguration?
    ): StartBrowserSettings {
        val applicationUrl = getApplicationUrl(launchProfile, arguments, functionLocalSettings)
        val webBrowser = hostRunConfiguration?.parameters?.startBrowserParameters?.browser
        val withJavaScriptDebugger =
            hostRunConfiguration?.parameters?.startBrowserParameters?.withJavaScriptDebugger == true

        return StartBrowserSettings().apply {
            browser = webBrowser
            isSelected = launchProfile?.launchBrowser == true
            url = applicationUrl
            isStartJavaScriptDebugger = withJavaScriptDebugger
        }
    }
}