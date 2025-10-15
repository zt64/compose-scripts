package dev.zt64.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import org.jetbrains.compose.resources.Font

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlsPane(
    state: HostState,
    sample: Sample,
    onSelectSample: (Sample) -> Unit,
    onScriptChange: (String) -> Unit,
    onClickCompile: () -> Unit
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

            LaunchedEffect(script) {
                onScriptChange(script)
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
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
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
                    onClick = { onClickCompile() },
                    enabled = state !is HostState.Compiling
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