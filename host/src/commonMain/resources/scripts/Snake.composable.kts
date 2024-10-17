import androidx.compose.ui.unit.IntOffset

enum class Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT,
}

@Composable
fun Content() {
    val gridSize = remember { 50 }
    var direction by remember { mutableStateOf(Direction.DOWN) }
    val tail = mutableStateListOf(
        IntOffset((0..gridSize).random(), (0..gridSize).random())
    )
    var pointCell by remember { mutableStateOf(IntOffset(gridSize / 2, gridSize / 2)) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            while (isActive) {
                val head = tail.first()

                val newHead = when (direction) {
                    Direction.UP -> head.copy(y = head.y - 1)
                    Direction.DOWN -> head.copy(y = head.y + 1)
                    Direction.LEFT -> head.copy(x = head.x - 1)
                    Direction.RIGHT -> head.copy(x = head.x + 1)
                }

                if (newHead.x !in 0..gridSize || newHead.y !in 0..gridSize) {
                    tail.clear()
                    tail += IntOffset((0..gridSize).random(), (0..gridSize).random())
                } else {
                    tail.add(0, newHead)
                    tail.removeLast()

                    if (head == pointCell) {
                        tail += tail.last()

                        pointCell = IntOffset((0..gridSize).random(), (0..gridSize).random())
                    }
                }

                delay(150)
            }
        }
    }

    val focusRequester = remember { FocusRequester() }

    Canvas(
        modifier = Modifier
            .onKeyEvent {
                if (it.type != KeyEventType.KeyDown) return@onKeyEvent false

                direction =
                    if (it.key == Key.DirectionUp && direction != Direction.DOWN) Direction.UP
                    else if (it.key == Key.DirectionDown && direction != Direction.UP) Direction.DOWN
                    else if (it.key == Key.DirectionLeft && direction != Direction.RIGHT) Direction.LEFT
                    else if (it.key == Key.DirectionRight && direction != Direction.LEFT) Direction.RIGHT
                    else return@onKeyEvent false

                true
            }
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .clickable(onClick = focusRequester::requestFocus)
    ) {
        // Size of individual tile
        val tileSize = size.minDimension / gridSize

        // Number of tiles horizontally
        val tilesWidth = gridSize * tileSize
        repeat(ceil(tilesWidth).toInt()) { columnIndex ->
            drawLine(
                color = Color.Black.copy(alpha = 0.8f),
                start = Offset(columnIndex * tileSize, 0f),
                end = Offset(columnIndex * tileSize, size.width)
            )
        }

        repeat(gridSize) { rowIndex ->
            drawLine(
                color = Color.Black.copy(alpha = 0.8f),
                start = Offset(0f, rowIndex * tileSize),
                end = Offset(size.width, rowIndex * tileSize)
            )
        }

        drawRect(
            color = Color.Red,
            topLeft = (pointCell * tileSize).toOffset(),
            size = Size(tileSize, tileSize)
        )

        tail.forEach { (x, y) ->
            drawRect(
                color = Color.Green,
                topLeft = Offset(x * tileSize, y * tileSize),
                size = Size(tileSize, tileSize)
            )
        }
    }
}