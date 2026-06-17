package com.edgepanel.app.model

data class HandleConfig(
    val isLeft: Boolean = true,
    val color: Int = 0xFF6200EE.toInt(),
    val transparency: Float = 0.85f,
    val width: Int = 56,
    val height: Int = 180,
    val cornerRadius: Float = 24f,
    val yOffset: Int = 0
)
