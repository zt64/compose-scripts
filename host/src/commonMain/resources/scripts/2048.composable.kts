import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val tileColors = mapOf(
    0 to Color(0xFFCCC0B3), // Empty Tile
    2 to Color(0xFFEEE4DA), // Tile 2
    4 to Color(0xFFEDE0C8), // Tile 4
    8 to Color(0xFFF2B179), // Tile 8
    16 to Color(0xFFF59563), // Tile 16
    32 to Color(0xFFF67C5F), // Tile 32
    64 to Color(0xFFF65E3B), // Tile 64
    128 to Color(0xFFEDCF72), // Tile 128
    256 to Color(0xFFEDCC61), // Tile 256
    512 to Color(0xFFEDC850), // Tile 512
    1024 to Color(0xFFEDC53F), // Tile 1024
    2048 to Color(0xFFEDC22E) // Tile 2048
)

val BOARD_SIZE = 4

@Composable
fun Content() {
    var board by remember {
        mutableStateOf(
            MutableList(BOARD_SIZE) { MutableList(BOARD_SIZE) { 0 } },
            neverEqualPolicy()
        )
    }

    LaunchedEffect(Unit) {
        board.randomAddTwoInPlace()
        board.randomAddTwoInPlace()
    }

    val textMeasurer = rememberTextMeasurer()
    val focusRequester = remember { FocusRequester() }

    Canvas(
        modifier = Modifier
            .onKeyEvent {
                if (it.type != KeyEventType.KeyDown) return@onKeyEvent false

                board = when (it.key) {
                    Key.DirectionUp -> board.transpose().moveLeftInPlace().transpose()
                    Key.DirectionDown -> board.transpose().moveRightInPlace().transpose()
                    Key.DirectionLeft -> board.moveLeftInPlace()
                    Key.DirectionRight -> board.moveRightInPlace()
                    else -> return@onKeyEvent false
                }

                board.randomAddTwoInPlace()

                true
            }.drawBehind {
                drawRect(
                    color = Color.DarkGray
                )
            }.size(300.dp)
            .focusRequester(focusRequester)
            .focusable()
            .clickable {
                focusRequester.requestFocus()
            }
    ) {
        val tileSize = size.height / BOARD_SIZE

        board.forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) {
                drawLine(
                    Color.Black,
                    Offset(0f, rowIndex * tileSize),
                    Offset(size.width, rowIndex * tileSize)
                )
            }

            row.forEachIndexed { tileIndex, value ->
                if (tileIndex > 0) {
                    drawLine(
                        Color.Black,
                        Offset(tileIndex * tileSize, 0f),
                        Offset(tileIndex * tileSize, size.height)
                    )
                }

                if (value > 0) {
                    drawRect(
                        color = tileColors[value]!!,
                        topLeft = Offset(tileIndex * tileSize, rowIndex * tileSize),
                        size = Size(tileSize, tileSize)
                    )

                    // Measure the text size
                    val textWidth = textMeasurer
                        .measure(
                            text = value.toString(),
                            style = TextStyle.Default.copy(fontSize = 12.sp)
                        ).size.width
                    val textHeight = textMeasurer
                        .measure(
                            text = value.toString(),
                            style = TextStyle.Default.copy(fontSize = 12.sp)
                        ).size.height

                    // Calculate the top-left position to center the text
                    val textX = tileIndex * tileSize + (tileSize - textWidth) / 2
                    val textY = rowIndex * tileSize + (tileSize - textHeight) / 2

                    drawText(
                        textMeasurer = textMeasurer,
                        text = value.toString(),
                        topLeft = Offset(textX, textY),
                        style = TextStyle.Default.copy(fontSize = 12.sp)
                    )
                }
            }
        }
    }
}

fun MutableList<MutableList<Int>>.moveLeftInPlace(): MutableList<MutableList<Int>> {
    this.forEach { row ->
        // Filter out non-zero values and prepare to merge
        val filtered = row.filter { it != 0 }.toMutableList()

        // Merge adjacent tiles if they are the same
        var i = 0
        while (i < filtered.size - 1) {
            if (filtered[i] == filtered[i + 1]) {
                filtered[i] *= 2 // Merge the two tiles
                filtered.removeAt(i + 1) // Remove the merged tile
            }
            i++
        }

        // Shift tiles to the left and fill remaining with zeros
        for (j in filtered.indices) {
            row[j] = filtered[j]
        }
        for (j in filtered.size until row.size) {
            row[j] = 0
        }
    }

    return this
}

fun MutableList<MutableList<Int>>.moveRightInPlace(): MutableList<MutableList<Int>> {
    this.forEach { row ->
        // Filter out non-zero values and prepare to merge
        val filtered = row.filter { it != 0 }.toMutableList()

        // Merge adjacent tiles if they are the same, moving from right to left
        var i = filtered.size - 1
        while (i > 0) {
            if (filtered[i] == filtered[i - 1]) {
                filtered[i] *= 2 // Merge the two tiles
                filtered.removeAt(i - 1) // Remove the merged tile
            }
            i--
        }

        // Shift tiles to the right and fill remaining with zeros
        for (j in 0 until (row.size - filtered.size)) {
            row[j] = 0
        }
        for (j in (row.size - filtered.size) until row.size) {
            row[j] = filtered[j - (row.size - filtered.size)]
        }
    }

    return this
}

fun MutableList<MutableList<Int>>.randomAddTwoInPlace() {
    val emptyTiles = this.flatMapIndexed { rowIndex, row ->
        row.mapIndexedNotNull { tileIndex, value ->
            if (value == 0) Pair(rowIndex, tileIndex) else null
        }
    }

    if (emptyTiles.isNotEmpty()) {
        val (rowIndex, tileIndex) = emptyTiles.random()
        this[rowIndex][tileIndex] = 2
    }
}

fun MutableList<MutableList<Int>>.transpose(): MutableList<MutableList<Int>> {
    val transposed = MutableList(size) { MutableList(size) { 0 } }
    forEachIndexed { rowIndex, row ->
        row.forEachIndexed { colIndex, value ->
            transposed[colIndex][rowIndex] = value
        }
    }
    return transposed
}