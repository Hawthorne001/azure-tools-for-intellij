/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.azuresdk.model.module;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkArtifactEntity;
import com.microsoft.intellij.util.MavenUtils;
import icons.MavenIcons;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.utils.MavenArtifactUtilKt;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode
public class MavenProjectModule implements ProjectModule {
    private final Project project;
    @Nonnull
    private final MavenProject mavenProject;

    public MavenProjectModule(@Nonnull final Project project, @Nonnull final MavenProject mavenProject) {
        this.project = project;
        this.mavenProject = mavenProject;
    }

    public static List<MavenProjectModule> listMavenModules(@Nonnull Project project) {
        return MavenUtils.getMavenProjects(project).stream().map(mavenProject -> new MavenProjectModule(project, mavenProject)).collect(Collectors.toList());
    }

    public MavenArtifact getMavenDependency(final String groupId, final String artifactId) {
        return mavenProject.getDependencies().stream()
                .filter(MavenArtifactUtilKt::resolved)
                .filter(dependency -> StringUtils.equalsIgnoreCase(groupId, dependency.getGroupId()) &&
                        StringUtils.equalsIgnoreCase(artifactId, dependency.getArtifactId())).findFirst().orElse(null);
    }

    @Override
    public ComparableVersion getDependencyVersion(AzureSdkArtifactEntity entity) {
        return Optional.ofNullable(entity)
                .map(e -> this.getMavenDependency(e.getGroupId(), e.getArtifactId()))
                .map(MavenArtifact::getVersion)
                .map(ComparableVersion::new).orElse(null);
    }

    @Override
    public String getName() {
        return Optional.ofNullable(mavenProject.getMavenId().getArtifactId()).orElseGet(mavenProject::getName);
    }

    @Override
    public Icon getIcon() {
        return MavenIcons.MavenProject;
    }
}
