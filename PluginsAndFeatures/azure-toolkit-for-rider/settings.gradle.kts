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

fun intellijModule(name: String) {
    include(":$name")
    project(":$name").projectDir = file("../azure-toolkit-for-intellij/$name")
}
fun riderModule(name: String) {
    include(":$name")
}

riderModule("protocol")
riderModule("azure-intellij-plugin-resharper-host")
intellijModule("azure-intellij-plugin-lib")
riderModule("azure-intellij-plugin-lib-dotnet")
intellijModule("azure-intellij-plugin-guidance")
intellijModule("azure-intellij-resource-connector-lib")
intellijModule("azure-intellij-plugin-service-explorer")
intellijModule("azure-intellij-plugin-arm")
intellijModule("azure-intellij-plugin-monitor")
intellijModule("azure-intellij-plugin-appservice")
riderModule("azure-intellij-plugin-appservice-dotnet")
riderModule("azure-intellij-plugin-appservice-dotnet-aspire")
intellijModule("azure-intellij-plugin-database")
riderModule("azure-intellij-plugin-database-dotnet")
intellijModule("azure-intellij-plugin-redis")
riderModule("azure-intellij-plugin-redis-dotnet")
intellijModule("azure-intellij-plugin-storage")
riderModule("azure-intellij-plugin-storage-dotnet")
intellijModule("azure-intellij-plugin-keyvault")
riderModule("azure-intellij-plugin-keyvault-dotnet")
intellijModule("azure-intellij-plugin-cloud-shell")
intellijModule("azure-intellij-plugin-servicebus")
intellijModule("azure-intellij-plugin-eventhubs")
intellijModule("azure-intellij-plugin-vm")
