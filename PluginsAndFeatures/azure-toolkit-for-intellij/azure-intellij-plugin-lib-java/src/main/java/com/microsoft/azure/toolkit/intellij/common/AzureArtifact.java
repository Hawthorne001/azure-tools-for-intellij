/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.externalSystem.model.project.ExternalProjectPojo;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.microsoft.azure.toolkit.intellij.common.utils.JdkUtils;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.intellij.util.GradleUtils;
import com.microsoft.intellij.util.MavenUtils;
import icons.GradleIcons;
import icons.OpenapiIcons;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.plugins.gradle.model.ExternalProject;
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

@Getter
@ToString(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
public class AzureArtifact {
    private final AzureArtifactType type;
    @ToString.Include
    private final String name;
    private final Object referencedObject;
    private final Project project;

    @Nullable
    public static AzureArtifact createFromFile(@Nullable String path, Project project) {
        if (StringUtils.isEmpty(path)) {
            return null;
        }
        return Optional.ofNullable(LocalFileSystem.getInstance().findFileByPath(path))
            .map(virtualFile -> createFromFile(virtualFile, project)).orElse(null);
    }

    public static AzureArtifact createFromFile(@Nonnull VirtualFile virtualFile, Project project) {
        return new AzureArtifact(AzureArtifactType.File, virtualFile.getName(), virtualFile, project);
    }

    public static AzureArtifact createFromArtifact(@Nonnull Artifact artifact, Project project) {
        return new AzureArtifact(AzureArtifactType.Artifact, artifact.getName(), artifact, project);
    }

    public static AzureArtifact createFromMavenProject(MavenProject mavenProject, Project project) {
        return new AzureArtifact(AzureArtifactType.Maven, mavenProject.getMavenId().getArtifactId(), mavenProject, project);
    }

    public static AzureArtifact createFromGradleProject(ExternalProjectPojo projectPojo, Project project) {
        return new AzureArtifact(AzureArtifactType.Gradle, projectPojo.getName(), projectPojo, project);
    }

    @Nullable
    public String getIdentifier() {
        return switch (this.getType()) {
            case Gradle -> {
                final ExternalProjectPojo pojo = (ExternalProjectPojo) this.getReferencedObject();
                final ExternalProject externalProject = ExternalProjectDataCache.getInstance(project).getRootExternalProject(pojo.getPath());
                yield Objects.nonNull(externalProject) ? externalProject.getQName() : null;
            }
            case Maven -> this.getReferencedObject().toString();
            case Artifact -> ((Artifact) this.getReferencedObject()).getOutputFilePath();
            case File -> this.getFileForDeployment();
        };
    }

    @AzureOperation(name = "internal/common.get_artifact_file.artifact", params = {"this.getName()"})
    public String getFileForDeployment() {
        return switch (this.getType()) {
            case Gradle -> GradleUtils.getTargetFile(project, (ExternalProjectPojo) this.getReferencedObject());
            case Maven -> MavenUtils.getSpringBootFinalJarFilePath(project, (MavenProject) this.getReferencedObject());
            case Artifact -> ((Artifact) this.getReferencedObject()).getOutputFilePath();
            case File -> ((VirtualFile) this.getReferencedObject()).getPath();
        };
    }

    public String getPackaging() {
        return switch (this.getType()) {
            case Gradle -> FileNameUtils.getExtension(GradleUtils.getTargetFile(project,
                (ExternalProjectPojo) this.getReferencedObject()));
            case Maven -> ((MavenProject) this.getReferencedObject()).getPackaging();
            case Artifact -> FileNameUtils.getExtension(((Artifact) this.getReferencedObject()).getOutputFilePath());
            case File -> FileNameUtils.getExtension(this.getFileForDeployment());
        };
    }

    public Icon getIcon() {
        return switch (type) {
            case Gradle -> GradleIcons.Gradle;
            case Maven -> OpenapiIcons.RepositoryLibraryLogo;
            case Artifact -> ((Artifact) referencedObject).getArtifactType().getIcon();
            case File -> AllIcons.FileTypes.Archive;
        };
    }

    @Nullable
    @AzureOperation(name = "boundary/common.get_artifact_module.artifact", params = {"this.getName()"})
    public Module getModule() {
        if (this.getReferencedObject() == null) {
            return null;
        }
        return switch (type) {
            case Gradle -> {
                final Path path = Paths.get(((ExternalProjectPojo) referencedObject).getPath());
                yield Optional.ofNullable(VfsUtil.findFile(path, true))
                    .map(f -> ModuleUtil.findModuleForFile(f, this.project)).orElse(null);
            }
            case Maven -> MavenProjectsManager.getInstance(this.project).findModule((MavenProject) referencedObject);
            case Artifact -> Optional.ofNullable(((Artifact) referencedObject).getOutputPath())
                .map(Path::of).map(FileUtils::getNearestExistingParent)
                .map(p -> VfsUtil.findFile(p, true))
                .map(f -> ProjectFileIndex.getInstance(project).getModuleForFile(f)).orElse(null);
            case File -> null;
        };
    }

    public Integer getBytecodeTargetLevel() {
        final Module module = this.getModule();
        Integer level = null;
        if (Objects.nonNull(module)) {
            level = JdkUtils.getTargetBytecodeLanguageLevel(module);
            if (Objects.isNull(level)) {
                level = JdkUtils.getJdkLanguageLevel(module);
            }
        } else if (type == AzureArtifactType.File) {
            level = JdkUtils.getBytecodeLanguageLevel(((VirtualFile) this.getReferencedObject()).toNioPath().toFile());
        }
        if (Objects.nonNull(level)) {
            return level;
        }
        return JdkUtils.getJdkLanguageLevel(this.project);
    }
}
