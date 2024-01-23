/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice;

import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.insights.ApplicationInsightsComboBox;
import com.microsoft.azure.toolkit.lib.appservice.model.ApplicationInsightsConfig;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import lombok.Getter;
import org.apache.commons.lang.BooleanUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;

@Getter
public class ApplicationInsightsPanel implements AzureFormPanel<ApplicationInsightsConfig> {
	private JPanel pnlRoot;
	private JLabel lblInsightsEnable;
	private JRadioButton rdoDisableApplicationInsights;
	private JRadioButton rdoEnableApplicationInsights;
	private JLabel lblApplicationInsights;
	private ApplicationInsightsComboBox applicationInsightsComboBox;

	public ApplicationInsightsPanel() {
		$$$setupUI$$$();
		init();
	}

	private void init() {
		final ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(rdoEnableApplicationInsights);
		buttonGroup.add(rdoDisableApplicationInsights);

		rdoEnableApplicationInsights.addActionListener(e -> {
			applicationInsightsComboBox.setEnabled(true);
			applicationInsightsComboBox.setRequired(true);
			applicationInsightsComboBox.revalidate();
		});

		rdoDisableApplicationInsights.addActionListener(e -> {
			applicationInsightsComboBox.setEnabled(false);
			applicationInsightsComboBox.setRequired(false);
			applicationInsightsComboBox.revalidate();
		});
	}

	@Override
	public void setVisible(final boolean visible) {
		this.pnlRoot.setVisible(visible);
	}

	@Override
	public void setValue(final ApplicationInsightsConfig data) {
		applicationInsightsComboBox.setValue(data);
		rdoEnableApplicationInsights.setSelected(BooleanUtils.isNotTrue(data.getDisableAppInsights()));
		rdoDisableApplicationInsights.setSelected(BooleanUtils.isTrue(data.getDisableAppInsights()));
	}

	@Override
	@Nullable
	public ApplicationInsightsConfig getValue() {
		return applicationInsightsComboBox.getValue();
	}

	@Override
	public List<AzureFormInput<?>> getInputs() {
		return List.of(this.applicationInsightsComboBox);
	}
}
