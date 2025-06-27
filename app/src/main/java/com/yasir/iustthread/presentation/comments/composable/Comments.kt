package com.yasir.iustthread.presentation.comments.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.yasir.iustthread.R
import com.yasir.iustthread.domain.model.CommentModel
import com.yasir.iustthread.domain.model.UserModel
import com.yasir.iustthread.presentation.comments.CommentViewModel
import com.yasir.iustthread.utils.SharedPref
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
    navController: NavHostController,
    threadId: String,
    commentViewModel: CommentViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUserId by remember { mutableStateOf(SharedPref.getUserId(context)) }
    
    val commentsAndUsers by commentViewModel.commentsAndUsers.observeAsState(initial = emptyList())
    val isCommentAdded by commentViewModel.isCommentAdded.observeAsState(initial = false)
    val isLoading by commentViewModel.isLoading.observeAsState(initial = false)
    
    var commentText by remember { mutableStateOf("") }
    var isAddingComment by remember { mutableStateOf(false) }
    
    // Listen for comments in real-time when screen loads
    LaunchedEffect(threadId) {
        commentViewModel.listenForComments(threadId)
    }
    
    // Reset comment text when comment is added successfully
    LaunchedEffect(isCommentAdded) {
        if (isCommentAdded) {
            commentText = ""
            isAddingComment = false
            // Reset the isCommentAdded state
            commentViewModel.resetCommentAddedState()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Comments",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1F2937)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            CommentInputBar(
                commentText = commentText,
                onCommentTextChange = { commentText = it },
                onSendClick = {
                    if (commentText.isNotBlank()) {
                        isAddingComment = true
                        commentViewModel.addComment(threadId, commentText, context)
                    }
                },
                isAddingComment = isAddingComment,
                isLoading = isLoading
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(paddingValues)
        ) {
            if (commentsAndUsers.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.chat),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF9CA3AF)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No comments yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6B7280)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Be the first to comment!",
                            fontSize = 14.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(commentsAndUsers) { (comment, user) ->
                        CommentItem(
                            comment = comment,
                            user = user,
                            currentUserId = currentUserId,
                            onLikeClick = { isLiked ->
                                commentViewModel.toggleCommentLike(
                                    commentId = comment.commentId,
                                    userId = currentUserId,
                                    isLiked = isLiked
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: CommentModel,
    user: UserModel,
    currentUserId: String,
    onLikeClick: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // User Avatar
            if (user.imageUri.isNotEmpty()) {
                AsyncImage(
                    model = user.imageUri,
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFE91E63), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(2).uppercase(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Comment Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = user.name.ifEmpty { "Unknown User" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatTimeAgo(comment.timeStamp),
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = comment.comment,
                    fontSize = 14.sp,
                    color = Color(0xFF4B5563),
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Comment Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { 
                            onLikeClick(comment.likedBy.contains(currentUserId))
                        }
                    ) {
                        Icon(
                            imageVector = if (comment.likedBy.contains(currentUserId)) 
                                Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (comment.likedBy.contains(currentUserId)) 
                                Color(0xFFE91E63) else Color(0xFF6B7280),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = comment.likes.toString(),
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = "Reply",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.clickable { /* Handle reply */ }
                    )
                }
            }
        }
    }
}

@Composable
fun CommentInputBar(
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isAddingComment: Boolean,
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar
            val context = LocalContext.current
            val currentUserId = SharedPref.getUserId(context)
            val userImageUri = SharedPref.getImageUrl(context)
            
            if (userImageUri.isNotEmpty()) {
                AsyncImage(
                    model = userImageUri,
                    contentDescription = "Your Avatar",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFE91E63), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = SharedPref.getName(context).take(2).uppercase(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Comment Input
            OutlinedTextField(
                value = commentText,
                onValueChange = onCommentTextChange,
                placeholder = {
                    Text(
                        text = "Add a comment...",
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF)
                    )
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE91E63),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send
                ),
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Send Button
            IconButton(
                onClick = onSendClick,
                enabled = commentText.isNotBlank() && !isAddingComment && !isLoading
            ) {
                if (isAddingComment) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFFE91E63),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (commentText.isNotBlank() && !isLoading) Color(0xFFE91E63) else Color(0xFF9CA3AF),
                        modifier = Modifier.size(24.dp)
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