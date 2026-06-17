package com.edgepanel.app.model

data class PeopleContact(
    val id: String,
    val name: String,
    val phone: String?,
    val photoUri: String?,
    val initials: String = name.take(2).uppercase()
)
