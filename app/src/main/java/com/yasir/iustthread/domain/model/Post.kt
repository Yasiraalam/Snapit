package com.yasir.iustthread.domain.model

data class Post(
    val id: String,
    val userAvatar: String,
    val userName: String,
    val userUsername: String,
    val timeAgo: String,
    val title: String,
    val content: String,
    val imageUrl: String? = null,
    val likes: Int,
    val comments: Int,
    val shares: Int = 0,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val likedBy: String = ""
)