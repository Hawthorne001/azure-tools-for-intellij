package com.microsoft.azure.toolkit.intellij.connector.sql;

import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.progress.ProgressIndicator;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceManager;
import com.microsoft.azure.toolkit.intellij.connector.database.DatabaseResource;
import com.microsoft.azure.toolkit.intellij.connector.database.DatabaseResourceConnection;
import org.jetbrains.annotations.NotNull;

public class SqlServerConnectionDefinitionPreloadingActivity extends PreloadingActivity {

    @Override
    public void preload(@NotNull ProgressIndicator progressIndicator) {
        final String resourceType = DatabaseResource.Definition.SQL_SERVER.getType();
        final String consumerType = ModuleResource.TYPE;
        ResourceManager.registerDefinition(DatabaseResource.Definition.SQL_SERVER);
        ConnectionManager.registerDefinition(resourceType, consumerType, DatabaseResourceConnection.Definition.MODULE_SQL);
    }
}
