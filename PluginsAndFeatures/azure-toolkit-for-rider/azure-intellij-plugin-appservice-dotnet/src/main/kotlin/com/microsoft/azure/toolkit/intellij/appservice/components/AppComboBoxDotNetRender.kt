/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.components

import com.intellij.ui.SimpleListCellRenderer
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import javax.swing.JList

class AppComboBoxDotNetRender : SimpleListCellRenderer<AppServiceConfig>() {
    override fun customize(
        list: JList<out AppServiceConfig>,
        config: AppServiceConfig?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ) {
        if (config == null) return

        text = config.appName
    }
}