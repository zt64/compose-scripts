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
                api(libs.kotlin.scripting.common)
            }
        }
    }
}