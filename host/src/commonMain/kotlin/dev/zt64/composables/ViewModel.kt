package dev.zt64.composables

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.reflect.getDeclaredComposableMethod
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.host.toScriptSource
import kotlin.time.measureTimedValue

@OptIn(FlowPreview::class)
class ViewModel {
    private val scope = CoroutineScope(Dispatchers.IO)

    private var _hostState = MutableStateFlow<HostState>(HostState.Idle)
    val hostState = _hostState.asStateFlow()

    private var _sample = MutableStateFlow(Sample.FRACTAL)
    val sample = _sample.asStateFlow()

    private var _currentScript = MutableStateFlow("")
    val currentScript = _currentScript.asStateFlow()

    private var lastCompiledScript = ""

    val snackbarHostState = SnackbarHostState()

    init {
        scope.launch {
            currentScript
                .debounce(2000)
                .filter { it != lastCompiledScript && it.isNotEmpty() }
                .collectLatest {
                    compile()
                }
        }
    }

    fun updateScript(script: String) {
        scope.launch {
            _currentScript.emit(script)
        }
    }

    fun compile() {
        scope.launch {
            _hostState.emit(HostState.Compiling(_currentScript.value))

            try {
                val (resultWithDiag, duration) = measureTimedValue {
                    evalComposeScript(_currentScript.value.toScriptSource())
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

                lastCompiledScript = _currentScript.value

                _hostState.emit(
                    HostState.Compiled(
                        content = { contentMethod(currentComposer, result.scriptInstance) },
                        duration = duration
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()

                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar("An error has occurred, see logs for details")
                }

                _hostState.emit(HostState.Error(e.message ?: "An error occurred"))
            }
        }
    }

    fun selectSample(sample: Sample) {
        scope.launch {
            _sample.emit(sample)
            _currentScript.emit(sample.code)
            _hostState.emit(HostState.Idle)
        }
    }
}