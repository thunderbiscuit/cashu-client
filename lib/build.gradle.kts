import org.gradle.api.tasks.testing.logging.TestExceptionFormat.*
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.10"
    id("java-library")
    id("maven-publish")
}

repositories {
    mavenCentral()
}

dependencies {}

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
