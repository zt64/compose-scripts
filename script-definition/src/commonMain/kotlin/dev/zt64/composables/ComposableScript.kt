package dev.zt64.composables

import org.jetbrains.kotlin.mainKts.*
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.DependsOn
import kotlin.script.experimental.dependencies.Repository
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmScriptDefinitionFromTemplate

@KotlinScript(
    displayName = "Composable Script",
    fileExtension = "composable.kts",
    compilationConfiguration = ComposableScriptConfiguration::class,
    // evaluationConfiguration = MainKtsEvaluationConfiguration::class,
    hostConfiguration = MainKtsHostConfiguration::class
)
abstract class ComposableScript

internal class ComposableScriptConfiguration :
    ScriptCompilationConfiguration(
        body = {
            isStandalone(false)
            ide {
                acceptedLocations(ScriptAcceptedLocation.Everywhere)
            }

            defaultImports(
                DependsOn::class,
                Repository::class,
                Import::class,
                CompilerOptions::class
            )

            defaultImports(
                "androidx.compose.material3.*",
                "androidx.compose.runtime.*",
                "androidx.compose.ui.*",
                "androidx.compose.ui.text.*",
                "androidx.compose.ui.unit.*",
                "androidx.compose.ui.focus.*",
                "androidx.compose.ui.geometry.*",
                "androidx.compose.ui.graphics.*",
                "androidx.compose.ui.input.key.*",
                "androidx.compose.ui.input.pointer.*",
                "androidx.compose.foundation.*",
                "androidx.compose.foundation.layout.*",
                "androidx.compose.foundation.gestures.*",
                "kotlin.math.*",
                "kotlin.random.*",
                "kotlinx.coroutines.*"
            )
            jvm {
                dependenciesFromClassloader(
                    "runtime-desktop",
                    "material3-desktop",
                    "ui-text-desktop",
                    "ui-desktop",
                    "ui-graphics-desktop",
                    "ui-unit-desktop"
                )
                dependenciesFromClassContext(MainKtsScriptDefinition::class, wholeClasspath = true)
            }

            refineConfiguration {
                onAnnotations(
                    DependsOn::class,
                    Repository::class,
                    Import::class,
                    CompilerOptions::class,
                    handler = MainKtsConfigurator()
                )

                onAnnotations(
                    ScriptFileLocation::class,
                    handler = ScriptFileLocationCustomConfigurator()
                )
            }

            compilerOptions(
                "-jvm-target=17"
            )
        }
    )

fun evalComposeScript(scriptSource: SourceCode): ResultWithDiagnostics<EvaluationResult> {
    val scriptDefinition = createJvmScriptDefinitionFromTemplate<ComposableScript>(
        evaluation = {
            // scriptsInstancesSharing(true)
            jvm {
                baseClassLoader(this::class.java.classLoader)
            }
        }
    )

    return BasicJvmScriptingHost().eval(
        script = scriptSource,
        compilationConfiguration = scriptDefinition.compilationConfiguration,
        evaluationConfiguration = scriptDefinition.evaluationConfiguration
    )
}