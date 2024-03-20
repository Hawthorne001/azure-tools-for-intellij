/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.projectView.workspace.ProjectModelEntity
import com.jetbrains.rider.run.configurations.publishing.RiderContextPublishProvider
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.FunctionDeploymentConfiguration
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.FunctionDeploymentConfigurationType
import javax.swing.Icon

class FunctionAppContextPublishProvider : RiderContextPublishProvider {
    override val icon: Icon
        get() = IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE)
    override val name: String
        get() = "Publish to Azure"

    override fun getConfigurationForNode(
        project: Project,
        entity: ProjectModelEntity
    ): Pair<RunConfiguration, ConfigurationFactory> {
        val projectData = RiderContextPublishProvider.getProjectDataRecursive(project, entity)

        val factory = ConfigurationTypeUtil.findConfigurationType(FunctionDeploymentConfigurationType::class.java)
            .configurationFactories
            .single()
        val configurationName = projectData?.value?.projectName ?: entity.name
        val configuration = FunctionDeploymentConfiguration(project, factory, "Publish $configurationName to Azure")

        if (projectData != null) {
            configuration.publishableProjectPath = projectData.value.projectFilePath
        } else {
            val publishableProject = project.solution.publishableProjectsModel.publishableProjects.entries.first {
                isProjectPublishable(it.value)
            }.value
            configuration.publishableProjectPath = publishableProject.projectFilePath
        }

        return Pair(configuration, factory)
    }

    private fun isProjectPublishable(projectData: PublishableProjectModel) = projectData.isAzureFunction

    override fun isAvailable(project: Project, entity: ProjectModelEntity): Boolean {
        val projectData = RiderContextPublishProvider.getProjectData(project, entity)
        return projectData != null && isProjectPublishable(projectData.value)
    }

    override fun solutionHasAnyPublishableProjects(project: Project) =
        project.solution.publishableProjectsModel.publishableProjects.entries.any { isProjectPublishable(it.value) }
}