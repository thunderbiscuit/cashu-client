import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT

// Library version is defined in gradle.properties
val libraryVersion: String by project

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
    id("org.gradle.java-library")
    id("org.gradle.maven-publish")
    id("org.jetbrains.dokka") version "1.9.10"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    // Bitcoin
    implementation("fr.acinq.bitcoin:bitcoin-kmp-jvm:0.14.0")
    implementation("fr.acinq.secp256k1:secp256k1-kmp-jni-jvm-darwin:0.11.0")
    implementation("fr.acinq.lightning:lightning-kmp:1.5.15")

    // Exposed
    implementation("org.jetbrains.exposed:exposed-core:0.40.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.40.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.40.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.40.1")

    // SQLite
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")

    // Ktor
    implementation("io.ktor:ktor-client-core-jvm:2.3.1")
    implementation("io.ktor:ktor-client-okhttp:2.3.1")
    implementation("io.ktor:ktor-client-logging:2.3.1")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.1")

    // Logging
    implementation("co.touchlab:kermit:2.0.4")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use Kotlin Test test framework
            useKotlinTest("1.9.10")
        }
    }
}

tasks.withType<Test> {
    testLogging {
        events(PASSED, SKIPPED, FAILED, STANDARD_OUT, STANDARD_ERROR)
        exceptionFormat = FULL
        showExceptions = true
        showStackTraces = true
        showCauses = true
    }
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    explicitApi()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "me.tb"
            artifactId = "cashu-client"
            version = libraryVersion

            from(components["java"])
        }
    }
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    dokkaSourceSets {
        named("main") {
            moduleName.set("cashu-client")
            moduleVersion.set(libraryVersion)
            // includes.from("Module.md")
            // samples.from("src/test/kotlin/me/tb/Samples.kt")
        }
    }
}
