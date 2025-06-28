package com.yasir.iustthread.domain.model

data class CommentModel(
    val commentId: String = "",
    val threadId: String = "",
    val userId: String = "",
    val comment: String = "",
    val timeStamp: String = "",
    val likedBy: List<String> = emptyList(),
    val likes: Int = 0,
    val parentCommentId: String? = null // null for top-level comments, commentId for replies
) {
    constructor() : this(
        "",
        "",
        "",
        "",
        "",
        emptyList(),
        0,
        null
    )
} 