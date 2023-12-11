# Install and Build Instructions
The library is not available on Maven Central; to use, you must build and deploy locally.

To build the library locally and deploy to your local Maven repository, clone the git repository and run the following command:
```shell
./gradlew publishToMavenLocal
```

The library will be available in your local Maven repository (typically at `~/.m2/repository/` for macOS and Linux systems) under the group ID `me.tb` and the artifact ID `cashu-client`. You can import it in your project as you would any other Maven dependency, provided you have your local Maven repository (`mavenLocal()`) configured as a dependency source:
```kotlin
// root-level settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}
```

```kotlin
// app-level build.gradle.kts
implementation("me.tb.cashu-client:0.0.1-SNAPSHOT")
```
