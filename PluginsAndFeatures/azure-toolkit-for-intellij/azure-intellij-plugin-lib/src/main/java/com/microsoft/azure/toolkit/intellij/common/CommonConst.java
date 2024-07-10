/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;

import java.util.Objects;

public class CommonConst {
    public static final String USER_AGENT = "Azure Toolkit for IntelliJ, v%s, machineid:%s";
    public static final String SPARK_SUBMISSION_WINDOW_ID = "HDInsight Spark Submission";
    public static final String DEBUG_SPARK_JOB_WINDOW_ID = "Debug Remote Spark Job in Cluster";
    public static final String REMOTE_SPARK_JOB_WINDOW_ID = "Remote Spark Job in Cluster";
    public static final String PLUGIN_ID = "com.intellij.resharper.azure";
    public static final String PLUGIN_NAME = "azure-toolkit-for-rider";
    public static final String PLUGIN_VERSION = PluginManager.getPlugin(PluginId.getId(PLUGIN_ID)).getVersion();
    public static final String PLUGIN_PATH = Objects.requireNonNull(PluginManagerCore.getPlugin(PluginId.findId(PLUGIN_ID))).getPluginPath().toString();
    public static final String SPARK_APPLICATION_TYPE = "com.microsoft.azure.hdinsight.DefaultSparkApplicationType";

    public static final String LOADING_TEXT = "Loading...";
    public static final String EMPTY_TEXT = "Empty";
    public static final String REFRESH_TEXT = "Refreshing...";
    public static final String RESOURCE_WITH_RESOURCE_GROUP = "%s (Resource Group: %s)";
    public static final String NEW_CREATED_RESOURCE = "%s (New Created)";
}
