dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-resource-connector-lib"))
    // runtimeOnly project(path: ":azure-intellij-resource-connector-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-resource-connector-lib-java"))
    // runtimeOnly project(path: ":azure-intellij-resource-connector-lib-java", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-keyvault"))
    // runtimeOnly project(path: ":azure-intellij-plugin-keyvault", configuration: "instrumentedJar")
    implementation("com.microsoft.azure:azure-toolkit-keyvault-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-keyvault-lib")
    intellijPlatform {
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.plugins.yaml")
        bundledPlugin("com.intellij.properties")
    }
}