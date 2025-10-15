package dev.zt64.composables

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OutputPane(
    state: HostState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            HostState.Idle -> {
                Text(
                    text = "No script loaded",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            is HostState.Compiling -> {
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
            is HostState.Compiled -> {
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
            is HostState.Error -> {
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