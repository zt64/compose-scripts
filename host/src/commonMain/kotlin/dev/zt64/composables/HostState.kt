package dev.zt64.composables

import androidx.compose.runtime.Composable
import kotlin.time.Duration

sealed interface HostState {
    data object Idle : HostState

    data class Compiling(val script: String) : HostState

    data class Compiled(val content: @Composable () -> Unit, val duration: Duration) : HostState

    data class Error(val message: String) : HostState
}