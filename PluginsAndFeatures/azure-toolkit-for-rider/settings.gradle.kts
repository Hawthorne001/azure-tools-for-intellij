rootProject.name = "azure-toolkit-for-rider"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.jetbrains.rdgen") {
                useModule("com.jetbrains.rd:rd-gen:${requested.version}")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include(":protocol")
include(":azure-intellij-plugin-resharper-host")
include(":azure-intellij-plugin-lib")
project(":azure-intellij-plugin-lib").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-lib")
include(":azure-intellij-plugin-lib-dotnet")
include(":azure-intellij-plugin-guidance")
project(":azure-intellij-plugin-guidance").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-guidance")
include(":azure-intellij-resource-connector-lib")
project(":azure-intellij-resource-connector-lib").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-resource-connector-lib")
include(":azure-intellij-plugin-service-explorer")
project(":azure-intellij-plugin-service-explorer").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-service-explorer")
include(":azure-intellij-plugin-arm")
project(":azure-intellij-plugin-arm").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-arm")
include(":azure-intellij-plugin-monitor")
project(":azure-intellij-plugin-monitor").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-monitor")
include(":azure-intellij-plugin-appservice")
project(":azure-intellij-plugin-appservice").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-appservice")
include(":azure-intellij-plugin-appservice-dotnet")
include(":azure-intellij-plugin-database")
project(":azure-intellij-plugin-database").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-database")
include(":azure-intellij-plugin-redis")
project(":azure-intellij-plugin-redis").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-redis")
include(":azure-intellij-plugin-storage")
project(":azure-intellij-plugin-storage").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-storage")
include(":azure-intellij-plugin-keyvault")
project(":azure-intellij-plugin-keyvault").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-keyvault")
include(":azure-intellij-plugin-database-dotnet")
include(":azure-intellij-plugin-cloud-shell")
