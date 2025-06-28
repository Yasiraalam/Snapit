package com.yasir.iustthread.presentation.comments.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.yasir.iustthread.R
import com.yasir.iustthread.domain.model.CommentModel
import com.yasir.iustthread.domain.model.UserModel
import com.yasir.iustthread.presentation.comments.CommentViewModel
import com.yasir.iustthread.utils.SharedPref
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    threadId: String,
    onDismiss: () -> Unit,
    commentViewModel: CommentViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUserId by remember { mutableStateOf(SharedPref.getUserId(context)) }
    
    val commentsAndUsers by commentViewModel.commentsAndUsers.observeAsState(initial = emptyList())
    val isCommentAdded by commentViewModel.isCommentAdded.observeAsState(initial = false)
    
    var commentText by remember { mutableStateOf("") }
    var isAddingComment by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var replyingToComment by remember { mutableStateOf<CommentModel?>(null) }
    var replyingToUser by remember { mutableStateOf<UserModel?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var commentToDelete by remember { mutableStateOf<CommentModel?>(null) }
    
    // Fetch comments when sheet opens
    LaunchedEffect(threadId) {
        isLoading = true
        commentViewModel.fetchComments(threadId)
        delay(500)
        isLoading = false
    }
    
    // Reset comment text when comment is added successfully
    LaunchedEffect(isCommentAdded) {
        if (isCommentAdded) {
            commentText = ""
            isAddingComment = false
            replyingToComment = null
            replyingToUser = null
            commentViewModel.resetCommentAddedFlag()
        }
    }
    
    // Function to handle comment submission
    val handleCommentSubmit = {
        if (commentText.isNotBlank()) {
            isAddingComment = true
            if (replyingToComment != null) {
                commentViewModel.addReply(
                    threadId = threadId,
                    parentCommentId = replyingToComment!!.commentId,
                    replyText = commentText,
                    context = context
                )
            } else {
                commentViewModel.addComment(threadId, commentText, context)
            }
        }
    }
    
    // Function to handle reply click
    val handleReplyClick = { commentToReply: CommentModel, userToReply: UserModel ->
        replyingToComment = commentToReply
        replyingToUser = userToReply
        // Don't call focusManager.requestFocus() here as it can cause issues
    }
    
    // Function to handle delete click
    val handleDeleteClick = { comment: CommentModel ->
        commentToDelete = comment
        showDeleteDialog = true
    }
    
    // Function to confirm delete
    val confirmDelete = {
        commentToDelete?.let { comment ->
            commentViewModel.deleteComment(comment.commentId, context)
        }
        showDeleteDialog = false
        commentToDelete = null
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comments (${commentsAndUsers.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF6B7280)
                        )
                    }
                }
                
                Divider(color = Color(0xFFE5E7EB))
                
                // Comments List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isLoading) {
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
                                    text = "Loading comments...",
                                    fontSize = 16.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                    } else if (commentsAndUsers.isEmpty()) {
                        // Empty state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
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
                            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
                        ) {
                            items(commentsAndUsers.filter { it.first.parentCommentId == null }) { (comment, user) ->
                                CommentItemWithReplies(
                                    comment = comment,
                                    user = user,
                                    currentUserId = currentUserId,
                                    allComments = commentsAndUsers,
                                    commentViewModel = commentViewModel,
                                    onLikeClick = { isLiked ->
                                        commentViewModel.toggleCommentLike(
                                            commentId = comment.commentId,
                                            userId = currentUserId,
                                            isLiked = isLiked
                                        )
                                    },
                                    onReplyClick = handleReplyClick,
                                    onDeleteClick = handleDeleteClick
                                )
                            }
                        }
                    }
                }
                
                // Reply indicator
                if (replyingToComment != null && replyingToUser != null) {
                    ReplyIndicator(
                        replyingToUser = replyingToUser!!,
                        onCancelReply = {
                            replyingToComment = null
                            replyingToUser = null
                            commentText = ""
                        }
                    )
                }
                
                // Comment Input
                CommentInputBar(
                    commentText = commentText,
                    onCommentTextChange = { commentText = it },
                    onSendClick = handleCommentSubmit,
                    isAddingComment = isAddingComment,
                    placeholder = if (replyingToComment != null) {
                        "Reply to ${replyingToUser?.name ?: "comment"}..."
                    } else {
                        "Add a comment..."
                    }
                )
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Delete Comment",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this comment? This action cannot be undone.",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = confirmDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFDC2626)
                    )
                ) {
                    Text(
                        text = "Delete",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        )
    }
}

@Composable
fun CommentItemWithReplies(
    comment: CommentModel,
    user: UserModel,
    currentUserId: String,
    allComments: List<Pair<CommentModel, UserModel>>,
    commentViewModel: CommentViewModel,
    onLikeClick: (Boolean) -> Unit,
    onReplyClick: (CommentModel, UserModel) -> Unit,
    onDeleteClick: (CommentModel) -> Unit
) {
    val replies = allComments.filter { it.first.parentCommentId == comment.commentId }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Main comment
        CommentItem(
            comment = comment,
            user = user,
            currentUserId = currentUserId,
            onLikeClick = onLikeClick,
            onReplyClick = onReplyClick,
            onDeleteClick = onDeleteClick
        )
        
        // Replies
        if (replies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.padding(start = 32.dp)
            ) {
                replies.forEach { (reply, replyUser) ->
                    CommentItem(
                        comment = reply,
                        user = replyUser,
                        currentUserId = currentUserId,
                        onLikeClick = { isLiked ->
                            commentViewModel.toggleCommentLike(
                                commentId = reply.commentId,
                                userId = currentUserId,
                                isLiked = isLiked
                            )
                        },
                        onReplyClick = onReplyClick,
                        onDeleteClick = onDeleteClick,
                        isReply = true
                    )
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
    onLikeClick: (Boolean) -> Unit,
    onReplyClick: (CommentModel, UserModel) -> Unit,
    onDeleteClick: (CommentModel) -> Unit,
    isReply: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isReply) Color(0xFFF9FAFB) else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isReply) 0.dp else 1.dp)
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
                        .size(if (isReply) 24.dp else 32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(if (isReply) 24.dp else 32.dp)
                        .background(Color(0xFFE91E63), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(2).uppercase(),
                        color = Color.White,
                        fontSize = if (isReply) 10.sp else 12.sp,
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
                        fontSize = if (isReply) 12.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatTimeAgo(comment.timeStamp),
                        fontSize = if (isReply) 10.sp else 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    
                    // Three-dot menu for user's own comments
                    if (comment.userId == currentUserId) {
                        Spacer(modifier = Modifier.weight(1f))
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(if (isReply) 20.dp else 24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(if (isReply) 14.dp else 16.dp)
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Delete",
                                            fontSize = 14.sp,
                                            color = Color(0xFFDC2626)
                                        )
                                    },
                                    onClick = {
                                        onDeleteClick(comment)
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = comment.comment,
                    fontSize = if (isReply) 12.sp else 14.sp,
                    color = Color(0xFF4B5563),
                    lineHeight = if (isReply) 16.sp else 18.sp
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
                            modifier = Modifier.size(if (isReply) 14.dp else 16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = comment.likes.toString(),
                            fontSize = if (isReply) 10.sp else 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = "Reply",
                        fontSize = if (isReply) 10.sp else 12.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.clickable { 
                            onReplyClick(comment, user)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ReplyIndicator(
    replyingToUser: UserModel,
    onCancelReply: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF3F4F6)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.reply),
                contentDescription = null,
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Replying to ${replyingToUser.name}",
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onCancelReply,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel reply",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(16.dp)
                )
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
    placeholder: String
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
                        text = if (isAddingComment) "Adding comment..." else placeholder,
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF)
                    )
                },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE91E63),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send
                ),
                maxLines = 3,
                enabled = !isAddingComment
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Send Button
            IconButton(
                onClick = onSendClick,
                enabled = commentText.isNotBlank() && !isAddingComment
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
                        tint = if (commentText.isNotBlank()) Color(0xFFE91E63) else Color(0xFF9CA3AF),
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