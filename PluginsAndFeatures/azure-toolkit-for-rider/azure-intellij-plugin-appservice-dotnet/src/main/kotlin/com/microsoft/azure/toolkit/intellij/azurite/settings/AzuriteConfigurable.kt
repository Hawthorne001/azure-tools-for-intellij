/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.azurite.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.selectedValueIs
import java.io.File

class AzuriteConfigurable : BoundConfigurable("Azurite") {
    companion object {
        private const val IP_ADDRESS_PATTERN = "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\$"
    }

    private val ipAddressRegex = Regex(IP_ADDRESS_PATTERN)

    private val settings get() = AzuriteSettings.getInstance()

    private lateinit var workspaceComboBox: Cell<ComboBox<AzuriteLocationMode>>

    override fun createPanel() = panel {
        group("Azurite Executable") {
            row("Azurite executable path:") {
                textFieldWithBrowseButton(
                    "Browse For Azurite Executable",
                    null,
                    FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor()
                )
                    .align(Align.FILL)
                    .bindText(settings::executablePath)
                    .validationOnInput { validationForPath(it) }
            }
        }
        group("General Settings") {
            row("Azurite workspace:") {
                workspaceComboBox =
                    comboBox(AzuriteLocationMode.entries, SimpleListCellRenderer.create("") { it.description })
                        .bindItem(settings::locationMode.toNullableProperty())
            }
            row("Path:") {
                textFieldWithBrowseButton(
                    "Browse For Workspace Location",
                    null,
                    FileChooserDescriptorFactory.createSingleFolderDescriptor()
                )
                    .align(Align.FILL)
                    .bindText(settings::workspacePath)
                    .validationOnInput { validationForPath(it) }
            }
                .visibleIf(workspaceComboBox.component.selectedValueIs(AzuriteLocationMode.Custom))
            row {
                checkBox("Enable loose mode (ignores unsupported headers and parameters)")
                    .bindSelected(settings::looseMode)
            }
        }
        group("Host/Port Settings") {
            row {
                label("Blob host:")
                textField()
                    .bindText(settings::blobHost)
                    .validationOnInput { validationForIpAddress(it) }
                label("Port:")
                intTextField(1024..65535)
                    .bindIntText(settings::blobPort)
            }.layout(RowLayout.PARENT_GRID)
            row {
                label("Queue host:")
                textField()
                    .bindText(settings::queueHost)
                    .validationOnInput { validationForIpAddress(it) }
                label("Port:")
                intTextField(1024..65535)
                    .bindIntText(settings::queuePort)
            }.layout(RowLayout.PARENT_GRID)
            row {
                label("Table host:")
                textField()
                    .bindText(settings::tableHost)
                    .validationOnInput { validationForIpAddress(it) }
                label("Port:")
                intTextField(1024..65535)
                    .bindIntText(settings::tablePort)
            }.layout(RowLayout.PARENT_GRID)
        }
        group("Certificate Settings") {
            row("Certificate path:") {
                textFieldWithBrowseButton(
                    "Select *.pem",
                    null,
                    FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                )
                    .align(Align.FILL)
                    .comment("Path to a locally-trusted pem or pfx certificate file path to enable HTTPS mode.")
                    .bindText(settings::certificatePath)
                    .validationOnInput { validationForPath(it) }
            }
            row("Certificate key path:") {
                textFieldWithBrowseButton(
                    "Select *.key",
                    null,
                    FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                )
                    .align(Align.FILL)
                    .comment("Path to a locally-trusted pem key file, required when cert points to a pem file.")
                    .bindText(settings::certificateKeyPath)
                    .validationOnInput { validationForPath(it) }
            }
            row("Certificate password:") {
                passwordField()
                    .align(Align.FILL)
                    .comment("Password for pfx file, required when cert points to a pfx file.")
                    .bindText(settings::certificatePassword)
            }
        }
    }

    private fun validationForPath(textField: TextFieldWithBrowseButton) =
        if (textField.text.isNotEmpty() && !File(textField.text).exists()) {
            ValidationInfo("Not a valid path.", textField)
        } else {
            null
        }

    private fun validationForIpAddress(textField: JBTextField) =
        if (textField.text.isNullOrEmpty() || !ipAddressRegex.matches(textField.text)) {
            ValidationInfo("Not a valid IP address.", textField)
        } else {
            null
        }
}