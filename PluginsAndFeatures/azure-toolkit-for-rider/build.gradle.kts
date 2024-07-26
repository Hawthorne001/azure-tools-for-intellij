/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

import com.jetbrains.plugin.structure.base.utils.isFile
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.Constants
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intelliJPlatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.changelog)
    alias(libs.plugins.qodana)
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

val azureToolkitVersion by extra { properties("azureToolkitVersion").get() }
val platformVersion by extra { properties("platformVersion").get() }

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
    mavenLocal()

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    implementation("com.microsoft.azure:azure-toolkit-libs:$azureToolkitVersion")
    implementation("com.microsoft.azure:azure-toolkit-ide-libs:$azureToolkitVersion")
    implementation("com.microsoft.hdinsight:azure-toolkit-ide-hdinsight-libs:0.1.1")

    implementation("com.microsoft.azure:azure-toolkit-common-lib:$azureToolkitVersion")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib:$azureToolkitVersion")

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-plugin-lib-dotnet"))
    implementation(project(path = ":azure-intellij-plugin-service-explorer"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(project(path = ":azure-intellij-plugin-guidance"))
    implementation(project(path = ":azure-intellij-plugin-arm"))
    implementation(project(path = ":azure-intellij-plugin-monitor"))
    implementation(project(path = ":azure-intellij-plugin-appservice"))
    implementation(project(path = ":azure-intellij-plugin-appservice-dotnet"))
    implementation(project(path = ":azure-intellij-plugin-database"))
    implementation(project(path = ":azure-intellij-plugin-database-dotnet"))
    implementation(project(path = ":azure-intellij-plugin-cloud-shell"))

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        rider(platformVersion, false)

        jetbrainsRuntime()

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(properties("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(properties("platformPlugins").map { it.split(',') })

        instrumentationTools()
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Bundled)
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        version = properties("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = properties("pluginUntilBuild")
        }
    }

    buildSearchableOptions  = false

    signing {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = environment("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = properties("pluginVersion").map {
            listOf(
                it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" })
        }
        hidden = true
    }

    verifyPlugin {
        ides {
            recommended()
        }
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    val rdGen = ":protocol:rdgen"

    val dotnetBuildConfiguration = properties("dotnetBuildConfiguration").get()
    val compileDotNet by registering {
        dependsOn(rdGen)
        doLast {
            exec {
                executable("dotnet")
                args("build", "-c", dotnetBuildConfiguration, "/clp:ErrorsOnly", "ReSharper.Azure.sln")
            }
        }
    }

    withType<KotlinCompile> {
        dependsOn(rdGen)
    }

    buildPlugin {
        dependsOn(compileDotNet)
    }

    withType<PrepareSandboxTask> {
        dependsOn(compileDotNet)

        val outputFolder = file("$projectDir/src/dotnet/ReSharper.Azure")

        val dllFiles = listOf(
            "$outputFolder/Azure.Project/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Project.dll",
            "$outputFolder/Azure.Project/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Project.pdb",
            "$outputFolder/Azure.Psi/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Psi.dll",
            "$outputFolder/Azure.Psi/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Psi.pdb",
            "$outputFolder/Azure.Intellisense/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Intellisense.dll",
            "$outputFolder/Azure.Intellisense/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Intellisense.pdb",
            "$outputFolder/Azure.Daemon/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Daemon.dll",
            "$outputFolder/Azure.Daemon/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Daemon.pdb",
            "$outputFolder/Azure.Daemon/bin/$dotnetBuildConfiguration/NCrontab.Signed.dll",
            "$outputFolder/Azure.Daemon/bin/$dotnetBuildConfiguration/CronExpressionDescriptor.dll"
        )

        for (f in dllFiles) {
            from(f) { into("${rootProject.name}/dotnet") }
        }

        doLast {
            for (f in dllFiles) {
                val file = file(f)
                if (!file.exists()) throw RuntimeException("File \"$file\" does not exist")
            }
        }
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }
}

val riderModel: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(riderModel.name, provider {
        intellijPlatform.platformPath.resolve("lib/rd/rider-model.jar").also {
            check(it.isFile) {
                "rider-model.jar is not found at $riderModel"
            }
        }
    }) {
        builtBy(Constants.Tasks.INITIALIZE_INTELLIJ_PLATFORM_PLUGIN)
    }
}
