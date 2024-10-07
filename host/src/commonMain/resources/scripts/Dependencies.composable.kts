@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("io.github.qdsfdhvh:image-loader-jvm:1.8.3")

import com.seiko.imageloader.*

@Composable
fun Content() {
    val painter = rememberImagePainter("https://avatars.githubusercontent.com/u/31907977?v=4")

    Image(
        painter = painter,
        contentDescription = "image",
    )
}