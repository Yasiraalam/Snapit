package com.yasir.iustthread.domain.model

data class UserModel(
    val email: String ="",
    val password: String ="",
    val name: String ="",
    val bio: String ="",
    val username: String ="",
    val imageUri: String ="",
    val uid: String? =""
)
