///**
// * Copyright (c) 2019-2020 JetBrains s.r.o.
// * <p/>
// * All rights reserved.
// * <p/>
// * MIT License
// * <p/>
// * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
// * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
// * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
// * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// * <p/>
// * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
// * the Software.
// * <p/>
// * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
// * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
// * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// * SOFTWARE.
// */
//
//package com.microsoft.intellij.ui.component
//
//import com.intellij.openapi.ui.ComboBox
//import com.microsoft.azure.management.appservice.RuntimeStack
//import com.microsoft.intellij.ui.extension.fillComboBox
//import com.microsoft.intellij.ui.extension.getSelectedValue
//import com.microsoft.intellij.ui.extension.setDefaultRenderer
//import net.miginfocom.swing.MigLayout
//import org.jetbrains.plugins.azure.RiderAzureBundle.message
//import javax.swing.JLabel
//import javax.swing.JPanel
//
//@Suppress("unused")
//class RuntimeStackSelector :
//        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]")),
//        AzureComponent {
//
//    private val lblRuntimeStack = JLabel(message("run_config.publish.form.runtime.label"))
//    val cbRuntimeStack = ComboBox<RuntimeStack>()
//
//    var lastSelectedRuntimeStack: RuntimeStack? = null
//
//    init {
//        initRuntimeStackComboBox()
//
//        add(lblRuntimeStack)
//        add(cbRuntimeStack, "growx")
//    }
//
//    fun fillRuntimeStack(runtimeStacks: List<RuntimeStack>, defaultRuntime: RuntimeStack? = null) =
//            cbRuntimeStack.fillComboBox(runtimeStacks, defaultRuntime)
//
//    private fun initRuntimeStackComboBox() {
//        cbRuntimeStack.setDefaultRenderer(message("run_config.publish.form.runtime.empty_message")) { it.stack() }
//        cbRuntimeStack.addActionListener {
//            lastSelectedRuntimeStack = cbRuntimeStack.getSelectedValue()
//        }
//    }
//}