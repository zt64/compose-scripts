@file:Repository("https://repo.maven.apache.org/maven2/")
@file:Repository("https://maven.google.com/")
@file:DependsOn("io.github.qdsfdhvh:image-loader-desktop:1.9.0")

import androidx.compose.runtime.Composable
import com.seiko.imageloader.*

@Composable
fun Content() {
    val painter = rememberImagePainter("https://avatars.githubusercontent.com/u/31907977?v=4")

    Image(
        painter = painter,
        contentDescription = "image"
    )
}