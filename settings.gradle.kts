rootProject.name = "cashu-client"
include("lib")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()

        // Snapshot repository
        // maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")

        // Local Maven (~/.m2/repository/)
        // mavenLocal()
    }
}
