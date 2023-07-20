# Readme
⚠️ Do not use in production ⚠️  
<br>

This is a library I'm writing to learn more about the Cashu protocol. Nothing like attempting to code out a spec to learn!

## Outline
This library is an implementation of the client side of the [Cashu specification] (i.e. it does not include mint software). It is written in Kotlin and is intended to be used in Kotlin Multiplatform projects. The API is still very much in development, and I am open to suggestions; see the [contribute](#contribute) section for more info on where I think I could use the most help. See the [issues] for discussion items and design decisions; issues and PRs are most welcome.

The main goals of this library are:
- [ ] 1. Cashu protocol compliant client side
    - [x] NUT-00
    - [x] NUT-01
    - [x] NUT-02
    - [x] NUT-03
    - [x] NUT-04
    - [ ] NUT-05
    - [ ] NUT-06
    - [ ] NUT-07
    - [ ] NUT-08
    - [ ] NUT-09
- [ ] 2. Well tested
- [ ] 3. Well documented
- [ ] 4. Usable in KMP projects (JVM and iOS platforms)

The library is not currently available on Maven Central and is certainly not production ready (hence the package name `me.tb`). If it ever grows into more than a personal side-project I would release under a different package name. 

To build locally and deploy to your local Maven repository, see the [build instructions](#build-instructions).

## Build Instructions
To build the library locally and deploy to your local Maven repository, run the following command:
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

## Contribute
If you are interested in contributing, there are a few areas where I think the codebase could use help with some input in terms of design and implementation:
1. The database, (particularly, the use of the [Exposed] library from JetBrains which I have not worked with before)
2. Mocking the mint for testing using Ktor
3. Dealing with errors when communicating with the mint

[Cashu specification]: https://github.com/cashubtc/nuts
[issues]: https://github.com/thunderbiscuit/cashu-client/issues
[Exposed]: https://github.com/JetBrains/Exposed
