/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.spring.properties;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJvmModifiersOwner;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.*;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ResourceManager;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SpringPropertyValueCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@Nonnull CompletionParameters parameters, @Nonnull ProcessingContext context, @Nonnull CompletionResultSet result) {
        final Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile());
        if (Objects.isNull(module)) {
            return;
        }
        final String key = parameters.getPosition().getParent().getFirstChild().getText();
        final List<? extends SpringSupported<?>> definitions = getSupportedDefinitions(key);
        if (!definitions.isEmpty()) {
            if (Azure.az(AzureAccount.class).isLoggedIn()) {
                final List<LookupElementBuilder> elements = definitions.stream().flatMap(d -> d.getResources(module.getProject()).stream().map(r -> LookupElementBuilder.create(r, r.getName())
                    .withIcon(IntelliJAzureIcons.getIcon(StringUtils.firstNonBlank(r.getDefinition().getIcon(), AzureIcons.Common.AZURE.getIconPath())))
                    .bold()
                    .withLookupStrings(Arrays.asList(r.getName(), ((AzResource) r.getData()).getResourceGroupName()))
                    .withInsertHandler(new PropertyValueInsertHandler(r))
                    .withTailText(" " + ((AzResource) r.getData()).getResourceTypeName())
                    .withTypeText(d.getSpringPropertyTypes().get(key)))).toList();
                elements.forEach(result::addElement);
                result.addLookupAdvertisement("Press enter to configure all required properties to connect Azure resource.");
            }
            // it's not safe to `stopHere()` immediately considering e.g. other cloud service may also
            // provide similar completion items for Redis/MongoDB related properties...
            result.runRemainingContributors(parameters, r -> {
                if (!(r.getLookupElement().getObject() instanceof PsiJvmModifiersOwner)) {
                    result.passResult(r);
                }
            });
        }
    }

    public static List<? extends SpringSupported<?>> getSupportedDefinitions(String key) {
        final List<ResourceDefinition<?>> definitions = ResourceManager.getDefinitions(ResourceDefinition.RESOURCE).stream()
            .filter(d -> d instanceof SpringSupported<?>).toList();
        return definitions.stream().map(d -> (SpringSupported<?>) d)
            .filter(d -> d.getSpringProperties().stream().anyMatch(p -> p.getKey().equals(key)))
            .toList();
    }

    @RequiredArgsConstructor
    protected static class PropertyValueInsertHandler implements InsertHandler<LookupElement> {

        @SuppressWarnings("rawtypes")
        private final Resource resource;

        @Override
        @ExceptionNotification
        @AzureOperation(name = "user/connector.insert_spring_properties")
        public void handleInsert(@Nonnull InsertionContext context, @Nonnull LookupElement lookupElement) {
            final PsiElement element = context.getFile().findElementAt(context.getStartOffset());
            if (Objects.nonNull(element)) {
                context.getDocument().deleteString(element.getTextOffset(), element.getTextOffset() + element.getTextLength());
            }
            final Project project = context.getProject();
            final Module module = ModuleUtil.findModuleForFile(context.getFile().getVirtualFile(), project);
            AzureTaskManager.getInstance().write(() -> Optional.ofNullable(module).map(AzureModule::from)
                .map(AzureModule::initializeWithDefaultProfileIfNot).map(Profile::getConnectionManager)
                .ifPresent(connectionManager -> connectionManager
                    .getConnectionsByConsumerId(module.getName()).stream()
                    .filter(c -> Objects.equals(resource, c.getResource())).findAny()
                    .ifPresentOrElse(c -> insert(c, context), () -> this.createAndInsert(module, context, connectionManager))));
        }

        private void createAndInsert(Module module, @Nonnull InsertionContext context, ConnectionManager connectionManager) {
            final Project project = context.getProject();
            if (this.resource.canConnectSilently()) {
                final Connection<?, ?> c = createSilently(module, connectionManager);
                WriteCommandAction.runWriteCommandAction(project, () -> insert(c, context));
            } else {
                AzureTaskManager.getInstance().runLater(() -> {
                    final var dialog = new ConnectorDialog(project);
                    dialog.setConsumer(new ModuleResource(module.getName()));
                    dialog.setResource(resource);
                    if (dialog.showAndGet()) {
                        final Connection<?, ?> c = dialog.getValue();
                        WriteCommandAction.runWriteCommandAction(project, () -> insert(c, context));
                    } else {
                        WriteCommandAction.runWriteCommandAction(project, () -> cancel(context));
                    }
                });
            }
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private Connection<?, ?> createSilently(Module module, ConnectionManager connectionManager) {
            final Resource consumer = ModuleResource.Definition.IJ_MODULE.define(module.getName());
            final ConnectionDefinition<?, ?> connectionDefinition = ConnectionManager.getDefinitionOrDefault(resource.getDefinition(), consumer.getDefinition());
            final Connection<?, ?> connection = connectionDefinition.define(this.resource, consumer);
            if (resource.getDefinition().isEnvPrefixSupported()) {
                connection.setEnvPrefix(connectionManager.getNewPrefix(resource, consumer));
            }
            final AzureTaskManager taskManager = AzureTaskManager.getInstance();
            taskManager.runLater(() -> taskManager.write(() -> {
                final Profile profile = connectionManager.getProfile().getModule().initializeWithDefaultProfileIfNot();
                profile.createOrUpdateConnection(connection);
                profile.save();
            }));
            return connection;
        }

        private static void cancel(@Nonnull InsertionContext context) {
        }

        public static void insert(Connection<?, ?> c, @Nonnull InsertionContext context) {
            final PsiElement element = context.getFile().findElementAt(context.getStartOffset());
            final List<Pair<String, String>> properties = SpringSupported.getProperties(c);
            if (properties.size() < 1 || Objects.isNull(element)) {
                return;
            }
            final String k0 = element.getParent().getFirstChild().getText().trim();
            properties.stream().filter(p -> p.getKey().equals(k0)).findAny().ifPresent(p -> {
                properties.remove(p);
                properties.add(0, p);
            });
            final StringBuilder result = new StringBuilder(properties.get(0).getValue()).append(StringUtils.LF);
            for (int i = 1; i < properties.size(); i++) {
                final Pair<String, String> p = properties.get(i);
                result.append(p.getKey()).append("=").append(p.getValue()).append(StringUtils.LF);
            }

            final CaretModel caretModel = context.getEditor().getCaretModel();
            context.getDocument().insertString(caretModel.getOffset(), result.toString());
        }
    }
}
