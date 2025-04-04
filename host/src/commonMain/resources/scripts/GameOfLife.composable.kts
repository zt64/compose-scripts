import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor

@Composable
fun Content() {
    Column {
        val gridSize = remember { 20 }
        val scope = rememberCoroutineScope()
        var running by remember { mutableStateOf(false) }
        var speed by remember { mutableStateOf(1f) }
        var cells by remember {
            mutableStateOf(
                List(gridSize) {
                    List(gridSize) { false }
                }
            )
        }

        Row(
            modifier = Modifier.padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconToggleButton(
                checked = running,
                onCheckedChange = { running = it }
            ) {
                if (running) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }

            Slider(
                value = speed,
                valueRange = 0.1f..2f,
                onValueChange = { speed = it }
            )
        }

        LaunchedEffect(running) {
            if (running) {
                scope.launch(Dispatchers.IO) {
                    val neighbors = listOf(
                        Pair(-1, -1),
                        Pair(-1, 0),
                        Pair(-1, 1),
                        Pair(0, -1),
                        Pair(0, 1),
                        Pair(1, -1),
                        Pair(1, 0),
                        Pair(1, 1)
                    )

                    while (running) {
                        cells = cells.mapIndexed { rowIndex, row ->
                            row.mapIndexed { columnIndex, cell ->
                                val modifiedNeighbors = neighbors.count { (dx, dy) ->
                                    val x = columnIndex + dx
                                    val y = rowIndex + dy

                                    if (x in 0 until gridSize && y in 0 until gridSize) {
                                        cells[y][x]
                                    } else {
                                        false
                                    }
                                }

                                if (cell) {
                                    modifiedNeighbors in 2..3
                                } else {
                                    modifiedNeighbors == 3
                                }
                            }
                        }
                        delay(150 * (1 / speed).toLong())
                    }
                }
            }
        }

        Canvas(
            modifier = Modifier
                .background(Color.DarkGray)
                .requiredSize(500.dp)
                .pointerInput(Unit) {
                    detectTapGestures { (x, y) ->
                        val cellX = floor(x / (size.width / gridSize)).toInt()
                        val cellY = floor(y / (size.height / gridSize)).toInt()

                        // toggle cell state
                        cells = cells.mapIndexed { rowIndex, row ->
                            row.mapIndexed { columnIndex, cell ->
                                if (rowIndex == cellY && columnIndex == cellX) !cell else cell
                            }
                        }
                    }
                }.pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val cellX = floor(change.position.x / (size.width / gridSize)).toInt()
                        val cellY = floor(change.position.y / (size.height / gridSize)).toInt()

                        // toggle cell state for only the cell being dragged
                        cells = cells.mapIndexed { rowIndex, row ->
                            row.mapIndexed { columnIndex, cell ->
                                if (rowIndex == cellY && columnIndex == cellX) true else cell
                            }
                        }
                    }
                }
        ) {
            val tileSize = size.minDimension / gridSize

            cells.forEachIndexed { rowIndex, row ->
                row.forEachIndexed { columnIndex, cell ->
                    if (cell) {
                        drawRect(
                            color = Color.Red,
                            topLeft = Offset(columnIndex * tileSize, rowIndex * tileSize),
                            size = Size(tileSize, tileSize)
                        )
                    }
                }
            }

            repeat(gridSize) { index ->
                drawLine(
                    color = Color.Black.copy(alpha = 0.8f),
                    start = Offset(index * tileSize, 0f),
                    end = Offset(index * tileSize, size.height)
                )

                drawLine(
                    color = Color.Black.copy(alpha = 0.8f),
                    start = Offset(0f, index * tileSize),
                    end = Offset(size.width, index * tileSize)
                )
            }
        }
    }
}