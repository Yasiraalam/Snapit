@file:OptIn(ExperimentalMaterial3Api::class)

package com.yasir.iustthread.presentation.home.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.yasir.iustthread.R
import com.yasir.iustthread.domain.model.Post
import com.yasir.iustthread.presentation.home.HomeViewModel
import com.yasir.iustthread.utils.SharedPref
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    navController: NavHostController,
    homeViewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUserId by remember { mutableStateOf(SharedPref.getUserId(context)) }
    
    val threadsAndUsers by homeViewModel.threadsAndUsers.observeAsState(initial = emptyList())
    
    // Convert ThreadModel and UserModel pairs to Post objects
    val posts = remember(threadsAndUsers, currentUserId) {
        threadsAndUsers.map { (thread, user) ->
            Post(
                id = thread.threadId,
                userAvatar = user.imageUri.ifEmpty { "" },
                userName = user.name.ifEmpty { "Loading..." },
                userUsername = user.username.ifEmpty { "unknown" },
                timeAgo = formatTimeAgo(thread.timeStamp),
                title = "Thread",
                content = thread.thread,
                imageUrl = thread.image.ifEmpty { null },
                likes = thread.likes,
                comments = thread.comments.toIntOrNull() ?: 0,
                isLiked = thread.likedBy.contains(currentUserId),
                likedBy = if (thread.likes > 0) "Liked by ${thread.likedBy.size} people" else ""
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        if (posts.isEmpty()) {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFE91E63),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading posts...",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(posts) { post ->
                    PostCard(
                        post = post,
                        onLikeClick = { isLiked ->
                            homeViewModel.toggleThreadLike(
                                threadId = post.id,
                                userId = currentUserId,
                                isLiked = isLiked
                            ) { newLikeCount ->
                                // The ViewModel will automatically refresh the data
                                // This callback can be used for additional UI updates if needed
                            }
                        },
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun PostCard(
    post: Post,
    onLikeClick: (Boolean) -> Unit,
    navController: NavHostController
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // User Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (post.userAvatar.isNotEmpty()) {
                        AsyncImage(
                            model = post.userAvatar,
                            contentDescription = "User Avatar",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFE91E63), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = post.userName.take(2).uppercase(),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = post.userName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            text = "@${post.userUsername} • ${post.timeAgo}",
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                IconButton(onClick = { /* Handle menu */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = Color(0xFF6B7280)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Post Image
            if (post.imageUrl != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = "Post Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.image)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Post Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onLikeClick(post.isLiked) }
                    ) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.isLiked) Color(0xFFE91E63) else Color(0xFF6B7280),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = post.likes.toString(),
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Comment Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { 
                            navController.navigate("comments/${post.id}")
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.chat),
                            contentDescription = "Comment",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = post.comments.toString(),
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Share Button
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { /* Handle share */ }
                    )
                }

//                // Bookmark Button
//                Icon(
//                    painter = if (post.isBookmarked) painterResource(id = R.drawable.filled_bookmark) else painterResource(id = R.drawable.bookmark),
//                    contentDescription = "Bookmark",
//                    tint = if (post.isBookmarked) Color(0xFFE91E63) else Color(0xFF6B7280),
//                    modifier = Modifier
//                        .size(24.dp)
//                        .clickable { /* Handle bookmark */ }
//                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Post Content
            Text(
                text = post.content,
                fontSize = 14.sp,
                color = Color(0xFF4B5563),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Liked by text
            if (post.likedBy.isNotEmpty()) {
                Text(
                    text = post.likedBy,
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

private fun formatTimeAgo(timestamp: String): String {
    return try {
        val timestampLong = timestamp.toLong()
        val currentTime = System.currentTimeMillis()
        val diffInMillis = currentTime - timestampLong
        val diffInMinutes = diffInMillis / (1000 * 60)
        val diffInHours = diffInMinutes / 60
        val diffInDays = diffInHours / 24

        when {
            diffInMinutes < 1 -> "Just now"
            diffInMinutes < 60 -> "${diffInMinutes}m"
            diffInHours < 24 -> "${diffInHours}h"
            diffInDays < 7 -> "${diffInDays}d"
            else -> {
                val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                dateFormat.format(Date(timestampLong))
            }
        }
    } catch (e: Exception) {
        "Unknown"
    }
}
