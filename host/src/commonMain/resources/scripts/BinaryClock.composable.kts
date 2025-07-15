import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun Content() {
    var totalSeconds by remember { mutableStateOf(System.currentTimeMillis()) }
    val scope = rememberCoroutineScope()
    val measurer = rememberTextMeasurer()

    LaunchedEffect(Unit) {
        scope.launch {
            while (isActive) {
                totalSeconds = System.currentTimeMillis() / 1000
                delay(200)
            }
        }
    }

    Canvas(
        modifier = Modifier.size(320.dp)
    ) {
        val hours = ((totalSeconds / 3600) % 24).toString().padStart(2, '0')
        val minutes = ((totalSeconds / 60) % 60).toString().padStart(2, '0')
        val seconds = (totalSeconds % 60).toString().padStart(2, '0')

        val sectionPadding = 16f
        val numColumns = 6
        val availableWidth = size.width - (2 * sectionPadding)
        val columnWidth = availableWidth / numColumns

        listOf(hours, minutes, seconds).forEachIndexed { sectionIdx, timeStr ->
            val d1 = timeStr[0].digitToInt()
            val d2 = timeStr[1].digitToInt()

            listOf(d1, d2).forEachIndexed { digitIdx, digit ->
                val col = sectionIdx * 2 + digitIdx
                val x = (col * columnWidth) + (sectionIdx * sectionPadding) + (columnWidth / 2)

                repeat(4) { row ->
                    val y = size.height / 5 * (row + 1)

                    drawCircle(
                        color = if ((digit shr (3 - row)) and 1 == 1) Color.Red else Color.White,
                        radius = 20f,
                        center = Offset(x, y)
                    )
                }
            }

            val label = "$d1$d2"
            val textLayoutResult = measurer.measure(label)
            val sectionCenter = (sectionIdx * (2 * columnWidth + sectionPadding)) + columnWidth
            drawText(
                textMeasurer = measurer,
                text = label,
                topLeft = Offset(
                    x = sectionCenter - textLayoutResult.size.width / 2,
                    y = size.height - textLayoutResult.size.height - 10
                ),
                style = TextStyle.Default.copy(
                    color = Color.White,
                    fontSize = 20.sp
                )
            )
        }
    }
}