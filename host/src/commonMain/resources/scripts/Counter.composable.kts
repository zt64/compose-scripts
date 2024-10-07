@file:Repository("https://repo.maven.apache.org/maven2/")

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