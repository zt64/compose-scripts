plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(17)

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.compiler.embeddable)
                implementation(libs.kotlin.scripting.mainKts)
                implementation(libs.kotlin.scripting.dependencies)
                implementation(libs.kotlin.scripting.dependencies.maven)
                implementation(libs.kotlin.coroutines.core)
                api(libs.kotlin.scripting.common)
            }
        }
    }
}