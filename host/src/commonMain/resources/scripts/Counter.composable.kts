import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*

@Composable
fun Content() {
    var i by remember { mutableIntStateOf(0) }

    Row {
        Text(i.toString())

        Button(onClick = { i++ }) {
            Text("Increment")
        }

        Button(onClick = { i-- }) {
            Text("Decrement")
        }
    }
}