package com.edgepanel.app.model

import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    var isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
