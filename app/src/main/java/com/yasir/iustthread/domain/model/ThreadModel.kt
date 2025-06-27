package com.yasir.iustthread.domain.model

data class ThreadModel(
    val threadId: String = "",
    val thread: String = "",
    val image: String = "",
    val userId: String = "",
    val timeStamp: String = "",
    val likedBy: List<String> = emptyList(),
    val likes: Int = 0,
    val comments: String = ""
) {
    constructor() : this(
        threadId = "",
        thread = "",
        image = "",
        userId = "",
        timeStamp = "",
        likedBy = emptyList(),
        likes = 0,
        comments = ""
    )
}
