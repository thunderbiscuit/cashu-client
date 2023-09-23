# Install and Build Instructions
To build the library locally and deploy to your local Maven repository, clone the git repository and run the following command:
```shell
./gradlew publishToMavenLocal
```

The library will be available in your local Maven repository (typically at `~/.m2/repository/` for macOS and Linux systems) under the group ID `me.tb` and the artifact ID `cashuclient`. You can import it in your project as you would any other Maven dependency, provided you have your local Maven repository (`mavenLocal()`) configured as a dependency source:
```kotlin
// root-level build.gradle.kts
allprojects {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

// app-level build.gradle.kts
implementation("me.tb.cashuclient:0.0.1-SNAPSHOT")
```
