package com.microsoft.azure.toolkit.intellij.common

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.run.configurations.publishing.PublishRuntimeSettingsCoreHelper

fun Row.dotnetProjectComboBox(project: Project, canBePublishedAction: (PublishableProjectModel) -> Boolean): Cell<AzureDotnetProjectComboBox> {
    val component = AzureDotnetProjectComboBox(project, canBePublishedAction)
    return cell(component)
}

fun Row.configurationAndPlatformComboBox(project: Project): Cell<LabeledComponent<ComboBox<PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform?>>> {
    val comboBox = PublishRuntimeSettingsCoreHelper.createConfigurationAndPlatformComboBox(project)
    return cell(comboBox)
}