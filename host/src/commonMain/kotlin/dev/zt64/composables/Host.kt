package dev.zt64.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.reflect.getDeclaredComposableMethod
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.host.toScriptSource
import kotlin.time.measureTimedValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Host() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                var state by remember { mutableStateOf<UiState>(UiState.Idle) }
                var selectedSample by remember { mutableStateOf(Sample.FRACTAL) }
                val compileScope = rememberCoroutineScope { Dispatchers.IO }

                ControlsPane(
                    state = state,
                    sample = selectedSample,
                    onSelectSample = { sample ->
                        selectedSample = sample
                        state = UiState.Idle
                    },
                    onClickCompile = { script ->
                        compileScope.launch {
                            state = UiState.Compiling(script)

                            try {
                                val (resultWithDiag, duration) = measureTimedValue {
                                    evalComposeScript(script.toScriptSource())
                                }

                                val result = when (resultWithDiag) {
                                    is ResultWithDiagnostics.Failure -> {
                                        throw Exception(
                                            resultWithDiag.reports
                                                .filter { it.severity >= ScriptDiagnostic.Severity.WARNING }
                                                .joinToString("\n") { it.render() }
                                        )
                                    }
                                    is ResultWithDiagnostics.Success<*> -> {
                                        resultWithDiag.valueOrThrow().returnValue
                                    }
                                }

                                val contentMethod = try {
                                    result.scriptClass!!
                                        .java
                                        .getDeclaredComposableMethod("Content")
                                } catch (e: NoSuchMethodException) {
                                    error("Script must contain a composable function named 'Content'")
                                }

                                state = UiState.Compiled(
                                    content = { contentMethod(currentComposer, result.scriptInstance) },
                                    duration = duration
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()

                                compileScope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar("An error has occurred, see logs for details")
                                }

                                state = UiState.Error(e.message ?: "An error occurred")
                            }
                        }
                    }
                )

                OutputPane(
                    modifier = Modifier.weight(1f),
                    state = state
                )
            }
        }
    }
}