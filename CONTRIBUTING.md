## Azure Toolkit for Rider

The main project is located in the `PluginsAndFeatures/azure-toolkit-for-rider`.
All changes to the `Utils` and `PluginsAndFeatures/azure-toolkit-for-intellij` folders must be implemented in
the [base repository](https://github.com/microsoft/azure-tools-for-java).

### Prerequisites

- Install [Corretto JDK 8](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/downloads-list.html)
- Install [Corretto JDK 17](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html)

#### Windows

- If you receive errors related to exit code -1073741515 during the Utils project installation, make sure to
  install [Visual Studio 2010 C++ Redistributable](https://learn.microsoft.com/en-US/cpp/windows/latest-supported-vc-redist?view=msvc-170#visual-studio-2010-vc-100-sp1-no-longer-supported).

### Building

* **Using JDK 8**, Clone and
  Build [azure-maven-plugins/azure-toolkit-libs](https://github.com/microsoft/azure-maven-plugins/tree/develop/azure-toolkit-libs)
  first.
  Azure Toolkit for Intellij doesn't depend on our maven plugins, but they share some code together.
    ```
    $ git clone https://github.com/microsoft/azure-maven-plugins.git
    $ cd azure-maven-plugins
    $ ./mvnw clean install -f azure-toolkit-libs/pom.xml
    ```
* Clone the repository with HTTPS or SSH:
    ```
    $ git clone https://github.com/JetBrains/azure-tools-for-intellij.git
    $ cd azure-tools-for-intellij
    ```
* **Using JDK 17**, run the following command under the project base path (If you have problems, make sure `JAVA_HOME`
  environment variable points to `<JDK17>/bin`):
    ```
    $ ./mvnw clean install -DskipTests -f Utils/pom.xml
    ```

* **Using JDK 17**, run `Build Plugin` run configuration or use Gradle to build the plugin
    ```
    $ ./PluginsAndFeatures/azure-toolkit-for-rider/gradlew -b ./PluginsAndFeatures/azure-toolkit-for-rider/build.gradle.kts buildPlugin
    ```
  You can find the outputs under ```PluginsAndFeatures/azure-toolkit-for-rider/build/distributions```

### Run/Debug

* Open IntelliJ, open `PluginsAndFeatures/azure-toolkit-for-rider`.
* Run/Debug the plugin by using `Run Plugin` run configuration.
