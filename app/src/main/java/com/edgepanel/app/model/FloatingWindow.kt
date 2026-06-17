package com.edgepanel.app.model

import android.graphics.Rect

data class FloatingWindow(
    val id: String,
    val packageName: String,
    val label: String,
    var bounds: Rect,
    var state: WindowState = WindowState.NORMAL
)

enum class WindowState { MINIMIZED, NORMAL, MAXIMIZED, FULLSCREEN }
