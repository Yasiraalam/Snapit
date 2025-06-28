package com.yasir.iustthread.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.yasir.iustthread.R
import com.yasir.iustthread.domain.model.ThreadModel
import com.google.firebase.auth.FirebaseAuth
import com.yasir.iustthread.domain.model.UserModel
import com.yasir.iustthread.presentation.home.HomeViewModel
import com.yasir.iustthread.ui.theme.PinkColor

@Composable
fun ThreadItem(
    thread: ThreadModel,
    users: UserModel,
    navHostController: NavHostController,
    userId: String,
    threadId: String,
    homeViewModel: HomeViewModel,
    threadAndUsers: List<Pair<ThreadModel, UserModel>>
) {
    ThreadContent(
        thread = thread,
        users = users,
        threadId = threadId,
        homeViewModel = homeViewModel
    )
}

@Composable
fun ThreadContent(
    thread: ThreadModel,
    users: UserModel,
    threadId: String,
    homeViewModel: HomeViewModel
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var isLiked by remember { mutableStateOf(thread.likedBy.contains(currentUserId)) }
    var likes by remember { mutableIntStateOf(thread.likes) }
    val cardShape = RoundedCornerShape(16.dp)
    
    // Update local state when thread data changes
    LaunchedEffect(thread) {
        isLiked = thread.likedBy.contains(currentUserId)
        likes = thread.likes
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp),
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Row: Avatar, Username, Time, Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (users.imageUri.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = users.imageUri),
                        contentDescription = "User Image",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE1306C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = users.username.take(2).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = users.username,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatTimeAgo(thread.timeStamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = { /* TODO: Menu */ }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Menu",
                        tint = Color.Black
                    )
                }
            }
            // Post Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 0.dp)
            ) {
                if (thread.image.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = thread.image),
                        contentDescription = "Post Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF6F6F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Beautiful Sunset",
                            color = Color(0xFF888888),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button - Now functional
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        homeViewModel.toggleThreadLike(
                            threadId = thread.threadId,
                            userId = currentUserId,
                            isLiked = isLiked
                        ) { newLikeCount ->
                            // Update local state immediately for better UX
                            isLiked = !isLiked
                            likes = newLikeCount
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) PinkColor else Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Comment Button
                Icon(
                    painter = painterResource(R.drawable.chat),
                    contentDescription = "Comment",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Share Button
                Icon(
                    painter = painterResource(R.drawable.send),
                    contentDescription = "send",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Bookmark Button
                Icon(
                    painter = painterResource(R.drawable.bookmark),
                    contentDescription = "Save",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            // Like and comment counts - Now dynamic
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)) {
                Text(
                    text = likes.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = thread.comments,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black
                )
            }
            
            // Title and description
            if (thread.thread.isNotEmpty()) {
                Text(
                    text = "Golden Hour Magic",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
                Text(
                    text = thread.thread,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)
                )
            }
            // Liked by ... and ... others
            Text(
                text = "Liked by alex_chen and 123 others",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
    Divider(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 0.dp),
        color = Color(0xFFE0E0E0),
        thickness = 1.dp
    )
}

// Helper to format time ago (simple version)
fun formatTimeAgo(timeStamp: String): String {
    return "2h" // TODO: Implement real time formatting
}

@Composable
fun ShimmerItem() {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .shimmerEffect()
        )
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember {
        mutableStateOf(IntSize.Zero)
    }
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scaleX by infiniteTransition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )
    background(
        brush = Brush.linearGradient(
            colors= listOf(
                Color(0xFFC9C1C1),
                Color(0xFF646262),
                Color(0xFFC9C1C1)
            ),
            start = Offset(scaleX,0f),
            end = Offset(scaleX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size =it.size
    }
}


