package com.microsoft.azure.toolkit.intellij.legacy.webapp.action

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManagerEx
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.RunDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.common.auth.AzureLoginHelper
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.RiderWebAppConfigurationType
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig.RiderWebAppConfiguration
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager

class DeployWebAppAction : AnAction() {
    companion object {
        private val configType = RiderWebAppConfigurationType.getInstance()

        fun deploy(webApp: WebApp?, project: Project?) {
            if (webApp == null || project == null) return
            val settings = getOrCreateRunConfigurationSettings(project, webApp)
            runConfiguration(project, settings)
        }

        private fun deploy(project: Project) {
            val settings = getOrCreateRunConfigurationSettings(project, null)
            runConfiguration(project, settings)
        }

        private fun getOrCreateRunConfigurationSettings(project: Project, webApp: WebApp?): RunnerAndConfigurationSettings {
            val manager = RunManagerEx.getInstanceEx(project)
            val factory = configType.getWebAppConfigurationFactory()
            val name = webApp?.name ?: ""
            val runConfigurationName = "${factory.name}: ${project.name} $name"
            val settings = manager.findConfigurationByName(runConfigurationName)
                    ?: manager.createConfiguration(runConfigurationName, factory)
            val runConfiguration = settings.configuration
            if (runConfiguration is RiderWebAppConfiguration && webApp != null) {
                runConfiguration.setWebApp(webApp)
            }
            return settings
        }

        private fun runConfiguration(project: Project, settings: RunnerAndConfigurationSettings) {
            val manager = RunManagerEx.getInstanceEx(project)
            AzureTaskManager.getInstance().runLater {
                if (RunDialog.editConfiguration(project, settings, "Deploy To Azure", DefaultRunExecutor.getRunExecutorInstance())) {
                    settings.storeInLocalWorkspace()
                    manager.addConfiguration(settings)
                    manager.selectedConfiguration = settings
                    ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance())
                }
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        AzureTaskManager.getInstance().runLater {
            AzureLoginHelper.requireSignedIn(project) { deploy(project) }
        }
    }
}