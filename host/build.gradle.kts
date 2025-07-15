plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.jb)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.application)
}

kotlin {
    jvmToolchain(17)

    jvm()
    androidTarget()

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.material3)
                implementation(compose.components.resources)

                implementation(libs.kodeview)

                implementation(projects.scriptDefinition)
                implementation(libs.kotlin.scripting.jvm.host)
            }
        }

        jvmMain {
            dependencies {
                implementation(compose.desktop.currentOs) {
                    exclude(group = "org.jetbrains.compose", module = "material")
                }
                implementation(libs.kotlin.coroutines.swing)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.android.core)
                implementation(libs.android.compat)
                implementation(libs.android.activity)
            }
        }
    }
}

android {
    namespace = "dev.zt64.composables"
    compileSdk = 35

    defaultConfig {
        targetSdk = 35
        minSdk = 26
    }

    packaging {
        resources {
            // Reflection symbol list (https://stackoverflow.com/a/41073782/13964629)
            excludes += "/**/*.kotlin_builtins"
            excludes.addAll(
                listOf(
                    "licenses/**"
                )
            )
        }
    }

    lint {
        abortOnError = false
    }
}

compose.desktop {
    application {
        mainClass = "dev.zt64.composables.MainKt"
    }
}