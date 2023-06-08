import org.gradle.api.tasks.testing.logging.TestExceptionFormat.*
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"
    id("java-library")
    id("maven-publish")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Bitcoin
    implementation("fr.acinq.bitcoin:bitcoin-kmp-jvm:0.12.0")
    implementation("fr.acinq.secp256k1:secp256k1-kmp-jni-jvm-darwin:0.10.0")

    // Exposed
    implementation("org.jetbrains.exposed:exposed-core:0.40.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.40.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.40.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.40.1")

    // SQLite
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")

    // TODO: Why isn't this needed?
    // testImplementation(kotlin("test"))
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use Kotlin Test test framework
            useKotlinTest("1.8.10")
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
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

kotlin {
    explicitApi()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "me.tb"
            artifactId = "kashuclient"
            version = "0.1.0-SNAPSHOT"

            from(components["java"])
        }
    }
}
