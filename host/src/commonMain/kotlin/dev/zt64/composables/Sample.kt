package dev.zt64.composables

private fun getResourceAsText(name: String): String? {
    return Sample::class.java.getResourceAsStream(name)?.reader()?.readText()
}

enum class Sample(val displayName: String, val fileName: String) {
    COUNTER(
        displayName = "Counter",
        fileName = "Counter"
    ),
    DRAGGING(
        displayName = "Dragging",
        fileName = "Dragging"
    ),
    GAME_2048(
        displayName = "2048",
        fileName = "2048"
    ),
    SNAKE(
        displayName = "Snake",
        fileName = "Snake"
    ),
    GOL(
        displayName = "Game of Life",
        fileName = "GameOfLife"
    ),
    DEPENDENCIES(
        displayName = "Dependencies",
        fileName = "Dependencies"
    ),
    BINARY_CLOCK(
        displayName = "Binary Clock",
        fileName = "BinaryClock"
    ),
    FRACTAL(
        displayName = "Fractal",
        fileName = "Fractal"
    );

    val code: String
        get() = getResourceAsText("/scripts/$fileName.composable.kts")!!
}