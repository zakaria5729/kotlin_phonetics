package io.github.zakaria5729.phonetics.sample

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Phonetics") {
        App()
    }
}
