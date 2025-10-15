package dev.zt64.composables

import androidx.compose.runtime.Composable
import kotlin.time.Duration

sealed interface UiState {
    data object Idle : UiState

    data class Compiling(val script: String) : UiState

    data class Compiled(val content: @Composable () -> Unit, val duration: Duration) : UiState

    data class Error(val message: String) : UiState
}