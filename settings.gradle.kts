@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
        google()
    }
}

rootProject.name = "compose-scripts"

include("host", "script-definition")