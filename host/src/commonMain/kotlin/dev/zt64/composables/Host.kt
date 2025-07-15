package dev.zt64.composables

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.reflect.getDeclaredComposableMethod
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose_scripts.host.generated.resources.MapleMono_Regular
import compose_scripts.host.generated.resources.Res
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes
import dev.snipme.kodeview.view.material3.CodeEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.Font
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.host.toScriptSource
import kotlin.time.Duration
import kotlin.time.measureTimedValue

private sealed interface UiState {
    data object Idle : UiState

    data class Compiling(val script: String) : UiState

    data class Compiled(val content: @Composable () -> Unit, val duration: Duration) : UiState

    data class Error(val message: String) : UiState
}

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
                var selectedSample by remember { mutableStateOf(Sample.DEPENDENCIES) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlsPane(
    state: UiState,
    sample: Sample,
    onSelectSample: (Sample) -> Unit,
    onClickCompile: (String) -> Unit
) {
    Surface(
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.width(IntrinsicSize.Max)
        ) {
            var script by remember(sample) { mutableStateOf(sample.code) }
            val highlights = remember(script) {
                Highlights
                    .Builder()
                    .code(script)
                    .language(SyntaxLanguage.KOTLIN)
                    .theme(SyntaxThemes.pastel(darkMode = true))
                    .build()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var expandedSamples by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expandedSamples,
                    onExpandedChange = { expandedSamples = it }
                ) {
                    TextField(
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .widthIn(min = 60.dp),
                        value = sample.displayName,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("Code Sample") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSamples) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedSamples,
                        onDismissRequest = { expandedSamples = false }
                    ) {
                        Sample.entries.forEach { sample ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = sample.displayName,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                onClick = {
                                    onSelectSample(sample)
                                    expandedSamples = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = { onClickCompile(script) },
                    enabled = state !is UiState.Compiling
                ) {
                    Text("Compile")
                }
            }

            CodeEditText(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(460.dp),
                highlights = highlights,
                onValueChange = { script = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily(Font(Res.font.MapleMono_Regular)),
                    fontSize = 14.sp
                )
            )
        }
    }
}

@Composable
private fun OutputPane(
    state: UiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = state) {
            UiState.Idle -> {
                Text(
                    text = "No script loaded",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            is UiState.Compiling -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Compiling",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            is UiState.Compiled -> {
                state.content()

                Text(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    text = buildString {
                        append("Compiled in ")
                        if (state.duration.inWholeSeconds > 1) {
                            append("${state.duration.inWholeSeconds.toString(10)}s")
                        }
                        append("${state.duration.inWholeMilliseconds % 1000}ms")
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            is UiState.Error -> {
                Surface(
                    modifier = Modifier
                        .widthIn(min = 200.dp, max = 700.dp)
                        .padding(16.dp),
                    shadowElevation = 4.dp,
                    tonalElevation = 7.dp,
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Failed to compile script",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(16.dp)
                        )

                        SelectionContainer(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}