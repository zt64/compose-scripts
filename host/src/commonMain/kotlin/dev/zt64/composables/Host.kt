package dev.zt64.composables

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.reflect.getDeclaredComposableMethod
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes
import dev.snipme.kodeview.view.material3.CodeEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.host.toScriptSource

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
                var content by remember {
                    mutableStateOf<(@Composable () -> Unit)?>(null)
                }

                Column {
                    var selectedSample by remember { mutableStateOf(Sample.DEPENDENCIES) }

                    var script by remember(selectedSample) {
                        mutableStateOf(selectedSample.code)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var expandedSamples by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = expandedSamples,
                            onExpandedChange = { expandedSamples = it }
                        ) {
                            TextField(
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable),
                                value = selectedSample.displayName,
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
                                            selectedSample = sample
                                            expandedSamples = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }

                        var buttonEnabled by remember { mutableStateOf(true) }
                        val coroutineScope = rememberCoroutineScope { Dispatchers.IO }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        buttonEnabled = false
                                        val resultWithDiag =
                                            evalComposeScript(script.toScriptSource())

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
                                            throw IllegalStateException(
                                                "Script must contain a composable function named 'Content'"
                                            )
                                        }

                                        // necessary to give compose time to render the new content
                                        content = null

                                        delay(20)

                                        content = {
                                            contentMethod(currentComposer, result.scriptInstance)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()

                                        coroutineScope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar(
                                                message = e.toString(),
                                                duration = SnackbarDuration.Long
                                            )
                                        }
                                    } finally {
                                        buttonEnabled = true
                                    }
                                }
                            },
                            enabled = buttonEnabled
                        ) {
                            Crossfade(
                                targetState = buttonEnabled
                            ) { enabled ->
                                Box(contentAlignment = Alignment.Center) {
                                    if (enabled) {
                                        Text("Compile")
                                    } else {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }

                    val highlights by remember(script) {
                        mutableStateOf(
                            Highlights
                                .Builder()
                                .code(script)
                                .language(SyntaxLanguage.KOTLIN)
                                .theme(SyntaxThemes.pastel(darkMode = true))
                                .build()
                        )
                    }

                    CodeEditText(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(380.dp),
                        highlights = highlights,
                        onValueChange = { textValue ->
                            script = textValue
                        },
                        colors = TextFieldDefaults.colors()
                    )
                }

                VerticalDivider()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .focusable()
                ) {
                    content?.invoke()
                }
            }
        }
    }
}