package dev.zt64.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Host() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        val viewModel = remember { ViewModel() }

        Scaffold(
            snackbarHost = { SnackbarHost(viewModel.snackbarHostState) }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val uiState by viewModel.hostState.collectAsState()
                val selectedSample by viewModel.sample.collectAsState()

                ControlsPane(
                    state = uiState,
                    sample = selectedSample,
                    onSelectSample = viewModel::selectSample,
                    onScriptChange = viewModel::updateScript,
                    onClickCompile = viewModel::compile
                )

                OutputPane(
                    modifier = Modifier.weight(1f),
                    state = uiState
                )
            }
        }
    }
}