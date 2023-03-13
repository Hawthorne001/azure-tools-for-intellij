/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.creation;

import com.azure.resourcemanager.appplatform.models.RuntimeVersion;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.ConfigDialog;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;

import javax.annotation.Nullable;
import javax.swing.*;

public class SpringCloudAppCreationDialog extends ConfigDialog<SpringCloudAppConfig> {
    private final SpringCloudCluster cluster;
    private JPanel panel;
    private SpringCloudAppInfoBasicPanel basicForm;
    private SpringCloudAppInfoAdvancedPanel advancedForm;

    public SpringCloudAppCreationDialog(@Nullable SpringCloudCluster cluster) {
        this(cluster, null);
    }

    public SpringCloudAppCreationDialog(@Nullable SpringCloudCluster cluster, @Nullable Project project) {
        super(project);
        this.cluster = cluster;
        this.init();
        setFrontPanel(basicForm);
    }

    @Override
    protected AzureFormPanel<SpringCloudAppConfig> getAdvancedFormPanel() {
        return advancedForm;
    }

    @Override
    protected AzureFormPanel<SpringCloudAppConfig> getBasicFormPanel() {
        return basicForm;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Azure Spring App";
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.panel;
    }

    private void createUIComponents() {
        advancedForm = new SpringCloudAppInfoAdvancedPanel(this.cluster);
        basicForm = new SpringCloudAppInfoBasicPanel(this.cluster);
    }

    public void setDefaultRuntimeVersion(@Nullable Integer jdkVersion) {
        if (jdkVersion != null) {
            final RuntimeVersion runtime = jdkVersion <= 11 ? RuntimeVersion.JAVA_11 : RuntimeVersion.JAVA_17;
            final SpringCloudAppConfig config = this.getData();
            config.setRuntimeVersion(runtime.toString());
            config.getDeployment().setRuntimeVersion(runtime.toString());
            this.setData(config);
        }
    }
}
