/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.sqlserver.common;

import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.toolkit.intellij.common.ValidationDebouncedTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.sqlserver.service.AzureSqlServer;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.regex.Pattern;

public class ServerNameTextField extends ValidationDebouncedTextInput {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]+[a-z0-9]$");
    private Subscription subscription;

    public void setSubscription(Subscription subscription) {
        if (!Objects.equals(subscription, this.subscription)) {
            this.subscription = subscription;
        }
    }

    /**
     * Server name should not contain reserved words.
     * Your server name can contain only lowercase letters, numbers, and '-', but can't start or end with '-' or have more than 63 characters.
     * The specified server name is already in use.
     */
    @Override
    @NotNull
    public AzureValidationInfo doValidateValue() {
        final AzureValidationInfo info = super.doValidateValue();
        if (!AzureValidationInfo.OK.equals(info)) {
            return info;
        }
        final String value = this.getValue();
        // validate length
        if (StringUtils.length(value) < 1 || StringUtils.length(value) > 63) {
            return AzureValidationInfo.builder().input(this).message("Server name must be at least 1 characters and at most 63 characters.")
                .type(AzureValidationInfo.Type.ERROR).build();
        }
        // validate special character
        if (!PATTERN.matcher(value).matches()) {
            return AzureValidationInfo.builder().input(this)
                .message("Your server name can contain only lowercase letters, numbers, and '-', but can't start or end with '-' or have more than 63 characters.")
                .type(AzureValidationInfo.Type.ERROR).build();
        }
        // validate availability
        if (!com.microsoft.azure.toolkit.lib.Azure.az(AzureSqlServer.class).checkNameAvailability(subscription.subscriptionId(), value)) {
            return AzureValidationInfo.builder().input(this).message("The specified server name is already in use.").type(AzureValidationInfo.Type.ERROR).build();
        }
        return AzureValidationInfo.OK;
    }

    @Override
    public boolean isRequired() {
        return true;
    }

}
