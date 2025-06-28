@file:OptIn(ExperimentalMaterial3Api::class)

package com.yasir.iustthread.presentation.home.composable

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.yasir.iustthread.presentation.comments.composable.CommentsBottomSheet
import com.yasir.iustthread.presentation.home.HomeViewModel
import com.yasir.iustthread.presentation.home.LoadingState
import com.yasir.iustthread.utils.SharedPref
import com.yasir.iustthread.utils.spotlightShimmerEffect
import com.yasir.iustthread.utils.NavigationUtils
import com.yasir.iustthread.utils.rememberBottomPadding
import kotlinx.coroutines.delay
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
    val bottomPadding = rememberBottomPadding()
    
    val threadsAndUsers by homeViewModel.threadsAndUsers.observeAsState(initial = emptyList())
    val loadingState by homeViewModel.loadingState.observeAsState(initial = LoadingState.LOADING)
    
    // State for comments bottom sheet
    var showCommentsSheet by remember { mutableStateOf(false) }
    var selectedThreadId by remember { mutableStateOf("") }
    
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

    // Calculate dynamic bottom padding based on navigation type
    val dynamicBottomPadding = if (NavigationUtils.isGestureNavigation(context)) {
        80.dp // Standard padding for gesture navigation
    } else {
        (80 + bottomPadding).dp // Extra padding for three-button navigation
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        when (loadingState) {
            LoadingState.LOADING -> {
                // Skeleton loading state
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = dynamicBottomPadding)
                ) {
                    items(5) { // Show 5 skeleton posts
                        SkeletonPostCard()
                    }
                }
            }
            LoadingState.EMPTY -> {
                // No posts state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        // Empty state icon
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(
                                    Color(0xFFE5E7EB),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "No Posts Yet",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Be the first to share something amazing!",
                            fontSize = 16.sp,
                            color = Color(0xFF6B7280),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Create post button
                        androidx.compose.material3.Button(
                            onClick = {
                                navController.navigate("add_thread")
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE91E63)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Create Your First Post",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            LoadingState.SUCCESS -> {
                // Posts list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = dynamicBottomPadding)
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
                            onCommentClick = {
                                selectedThreadId = post.id
                                showCommentsSheet = true
                            }
                        )
                    }
                }
            }
            LoadingState.ERROR -> {
                // Error state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Something went wrong",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1F2937)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please check your connection and try again",
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
        
        // Comments Bottom Sheet
        if (showCommentsSheet) {
            CommentsBottomSheet(
                threadId = selectedThreadId,
                onDismiss = {
                    showCommentsSheet = false
                    selectedThreadId = ""
                }
            )
        }
    }
}

@Composable
fun PostCard(
    post: Post,
    onLikeClick: (Boolean) -> Unit,
    onCommentClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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

            // Post Image with Double Tap to Like
            if (post.imageUrl != null) {
                DoubleTapLikeImage(
                    imageUrl = post.imageUrl,
                    isLiked = post.isLiked,
                    onLikeClick = onLikeClick
                )
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
                        modifier = Modifier.clickable { onCommentClick() }
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

@Composable
fun DoubleTapLikeImage(
    imageUrl: String,
    isLiked: Boolean,
    onLikeClick: (Boolean) -> Unit
) {
    var showHeartAnimation by remember { mutableStateOf(false) }
    var lastTapTime by remember { mutableStateOf(0L) }
    
    val heartScale by animateFloatAsState(
        targetValue = if (showHeartAnimation) 1.5f else 0f,
        animationSpec = tween(durationMillis = 800, easing = androidx.compose.animation.core.EaseOutBack),
        label = "heart_scale"
    )
    
    val heartAlpha by animateFloatAsState(
        targetValue = if (showHeartAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "heart_alpha"
    )

    // Auto-hide heart animation
    LaunchedEffect(showHeartAnimation) {
        if (showHeartAnimation) {
            delay(800)
            showHeartAnimation = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val currentTime = System.currentTimeMillis()
                        val timeDiff = currentTime - lastTapTime
                        
                        if (timeDiff < 300) { // Double tap detected (within 300ms)
                            // Trigger like if not already liked
                            if (!isLiked) {
                                onLikeClick(false) // false means we want to like it
                            }
                            
                            // Show heart animation
                            showHeartAnimation = true
                        }
                        
                        lastTapTime = currentTime
                    }
                )
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Post Image
            AsyncImage(
                model = imageUrl,
                contentDescription = "Post Image",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.image)
            )
            
            // Animated Heart Overlay
            if (showHeartAnimation) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Heart",
                        tint = Color.White,
                        modifier = Modifier
                            .size(100.dp)
                            .scale(heartScale)
                            .background(
                                Color(0xFFE91E63).copy(alpha = heartAlpha * 0.8f),
                                CircleShape
                            )
                            .padding(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SkeletonPostCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .spotlightShimmerEffect(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // User Header Skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar skeleton
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE5E7EB))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        // Username skeleton
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE5E7EB))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Handle and time skeleton
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE5E7EB))
                        )
                    }
                }

                // Menu button skeleton
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5E7EB))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Post content skeleton
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE5E7EB))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE5E7EB))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE5E7EB))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Post image skeleton (optional)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE5E7EB))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Post actions skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like button skeleton
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE5E7EB))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE5E7EB))
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Comment button skeleton
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE5E7EB))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE5E7EB))
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Share button skeleton
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE5E7EB))
                    )
                }
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
