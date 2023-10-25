/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.completion.resource.function;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiJavaElementPattern;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.storage.completion.resource.AzureStorageJavaCompletionContributor;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.storage.queue.Queue;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;

public class FunctionQueueNameCompletionProvider extends CompletionProvider<CompletionParameters> {
    public static final String[] QUEUE_ANNOTATIONS = new String[]{
            "com.microsoft.azure.functions.annotation.QueueTrigger",
            "com.microsoft.azure.functions.annotation.QueueOutput"
    };
    public static final PsiJavaElementPattern<?, ?> QUEUE_NAME_PATTERN = psiElement().withSuperParent(2,
            PsiJavaPatterns.psiNameValuePair().withName("queueName").withParent(
                    PlatformPatterns.psiElement(PsiAnnotationParameterList.class).withParent(
                            PsiJavaPatterns.psiAnnotation().with(new PatternCondition<>("queueAnnotation") {
                                @Override
                                public boolean accepts(@NotNull final PsiAnnotation psiAnnotation, final ProcessingContext context) {
                                    return StringUtils.equalsAnyIgnoreCase(psiAnnotation.getQualifiedName(), QUEUE_ANNOTATIONS);
                                }
                            }))));

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        final PsiElement element = parameters.getPosition();
        final String fullPrefix = element.getText().split(AzureStorageJavaCompletionContributor.DUMMY_IDENTIFIER)[0].replace("\"", "").trim();
        final Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile());
        if (Objects.isNull(module) || !Azure.az(AzureAccount.class).isLoggedIn()) {
            return;
        }
        final PsiAnnotation annotation = PsiTreeUtil.getParentOfType(parameters.getPosition(), PsiAnnotation.class);
        final StorageAccount account = Optional.ofNullable(annotation).map(FunctionUtils::getBindingStorageAccount).orElse(null);
        if (Objects.isNull(account)) {
            return;
        }
        account.getQueueModule().list().stream()
                .filter(queue -> StringUtils.startsWithIgnoreCase(queue.getName(), fullPrefix))
                .map(this::createLookupElement)
                .forEach(result::addElement);
    }

    private LookupElement createLookupElement(Queue queue) {
        return LookupElementBuilder.create(queue.getName())
                .withBoldness(true)
                .withInsertHandler(new FunctionBlobPathCompletionProvider.MyInsertHandler(false, queue.getParent()))
                .withCaseSensitivity(false)
                .withTypeText("Queue")
                .withTailText(" " + queue.getParent().getName())
                .withIcon(IntelliJAzureIcons.getIcon(AzureIcons.StorageAccount.QUEUES));
    }
}
