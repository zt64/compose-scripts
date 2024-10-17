package dev.zt64.composables

private fun getResourceAsText(name: String): String? {
    return Sample::class.java.getResourceAsStream(name)?.reader()?.readText()
}

enum class Sample(val displayName: String, val code: String) {
    COUNTER(
        displayName = "Counter",
        code = getResourceAsText("/scripts/Counter.composable.kts")!!
    ),
    DRAGGING(
        displayName = "Dragging",
        code = getResourceAsText("/scripts/Dragging.composable.kts")!!
    ),
    GAME_2048(
        displayName = "2048",
        code = getResourceAsText("/scripts/2048.composable.kts")!!
    ),
    SNAKE(
        displayName = "Snake",
        code = getResourceAsText("/scripts/Snake.composable.kts")!!
    ),
    DEPENDENCIES(
        displayName = "Dependencies",
        code = getResourceAsText("/scripts/Dependencies.composable.kts")!!
    )
}