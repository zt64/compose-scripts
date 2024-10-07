package dev.zt64.composables

import androidx.compose.ui.window.singleWindowApplication
import java.awt.Dimension

fun main() {
    singleWindowApplication(
        title = "Compose scripting"
    ) {
        window.minimumSize = Dimension(900, 900)

        Host()
    }
}