/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.jetbrains.rider.model.PublishableProjectModel

class DotNetFunctionDeployModel : FunctionDeployModel() {
    var publishableProject: PublishableProjectModel? = null
    var projectConfiguration: String = ""
    var projectPlatform: String = ""
}