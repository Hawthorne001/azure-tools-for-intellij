/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.util;

import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.microsoft.intellij.ui.libraries.AILibraryHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

@Slf4j
public class MethodUtils {

    /**
     * Method scans all open Maven or Dynamic web projects form workspace
     * and returns name of project who is using specific key.
     */
    public static String getModuleNameAsPerKey(Project project, String keyToRemove) {
        final String name = "";
        try {
            final Module[] modules = ModuleManager.getInstance(project).getModules();
            for (final Module module : modules) {
                if (module != null && module.isLoaded()
                        && JavaModuleType.getModuleType().getId().equals(module.getOptionValue(Module.ELEMENT_TYPE))) {
                    final String aiXMLPath = String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, message("aiXMLPath"));
                    final String webXMLPath = String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, message("xmlPath"));
                    final AILibraryHandler handler = new AILibraryHandler();
                    if (new File(aiXMLPath).exists() && new File(webXMLPath).exists()) {
                        handler.parseWebXmlPath(webXMLPath);
                        handler.parseAIConfXmlPath(aiXMLPath);
                        // if application insights configuration is enabled.
                        if (handler.isAIWebFilterConfigured()) {
                            final String key = handler.getAIInstrumentationKey();
                            if (key != null && !key.isEmpty() && key.equals(keyToRemove)) {
                                return module.getName();
                            }
                        }
                    }
                }
            }
        } catch (final Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return name;
    }
}
