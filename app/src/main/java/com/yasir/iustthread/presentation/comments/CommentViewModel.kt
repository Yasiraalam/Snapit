package com.yasir.iustthread.presentation.comments

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.yasir.iustthread.domain.model.CommentModel
import com.yasir.iustthread.domain.model.UserModel
import com.yasir.iustthread.utils.SharedPref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class CommentViewModel : ViewModel() {
    
    private val database = FirebaseDatabase.getInstance()
    private val commentsRef = database.getReference("comments")
    private val usersRef = database.getReference("Users")
    private val threadsRef = database.getReference("threads")
    
    private val _commentsAndUsers = MutableLiveData<List<Pair<CommentModel, UserModel>>>()
    val commentsAndUsers: LiveData<List<Pair<CommentModel, UserModel>>> = _commentsAndUsers
    
    private val _isCommentAdded = MutableLiveData<Boolean>()
    val isCommentAdded: LiveData<Boolean> = _isCommentAdded
    
    // Cache for optimistic updates
    private val commentCache = mutableMapOf<String, CommentModel>()
    private val userCache = mutableMapOf<String, UserModel>()
    
    // Firebase listeners
    private var commentsListener: ValueEventListener? = null
    private var usersListener: ValueEventListener? = null
    private var currentThreadId: String? = null
    
    fun fetchComments(threadId: String) {
        currentThreadId = threadId
        setupRealTimeListeners(threadId)
    }
    
    private fun setupRealTimeListeners(threadId: String) {
        // Remove existing listeners
        commentsListener?.let { commentsRef.removeEventListener(it) }
        usersListener?.let { usersRef.removeEventListener(it) }
        
        // Listen for real-time comment updates for this thread
        commentsListener = commentsRef.orderByChild("threadId").equalTo(threadId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val result = mutableListOf<Pair<CommentModel, UserModel>>()
                            
                            if (snapshot.childrenCount == 0L) {
                                withContext(Dispatchers.Main) {
                                    _commentsAndUsers.value = result
                                }
                                return@launch
                            }
                            
                            for (commentSnapshot in snapshot.children) {
                                val comment = commentSnapshot.getValue(CommentModel::class.java)
                                comment?.let {
                                    // Cache the comment
                                    commentCache[it.commentId] = it
                                    
                                    // Get user from cache or fetch
                                    val user = userCache[it.userId] ?: fetchUserForComment(it)
                                    if (user.uid?.isNotEmpty() == true) {
                                        userCache[it.userId] = user
                                    }
                                    result.add(it to user)
                                }
                            }
                            
                            // Sort by timestamp (newest first)
                            result.sortByDescending { it.first.timeStamp }
                            
                            withContext(Dispatchers.Main) {
                                _commentsAndUsers.value = result
                            }
                        } catch (e: Exception) {
                            Log.e("iust_debug", "Error in comments listener: ${e.message}")
                        }
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e("iust_debug", "Comments listener cancelled: ${error.message}")
                }
            })
        
        // Listen for real-time user updates
        usersListener = usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(UserModel::class.java)
                            user?.let {
                                userCache[it.uid.toString()] = it
                            }
                        }
                        
                        // Refresh comments to update user info
                        refreshCommentsWithCachedUsers()
                    } catch (e: Exception) {
                        Log.e("iust_debug", "Error in users listener: ${e.message}")
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("iust_debug", "Users listener cancelled: ${error.message}")
            }
        })
    }
    
    private suspend fun refreshCommentsWithCachedUsers() {
        val currentComments = _commentsAndUsers.value ?: return
        val updatedComments = currentComments.map { (comment, _) ->
            val user = userCache[comment.userId] ?: UserModel()
            comment to user
        }
        
        withContext(Dispatchers.Main) {
            _commentsAndUsers.value = updatedComments
        }
    }
    
    private suspend fun fetchUserForComment(comment: CommentModel): UserModel {
        return try {
            if (comment.userId.isEmpty()) {
                return UserModel()
            }
            
            // Check cache first
            userCache[comment.userId]?.let { return it }
            
            val userSnapshot = usersRef.child(comment.userId).get().await()
            userSnapshot.getValue(UserModel::class.java) ?: UserModel()
        } catch (e: Exception) {
            Log.e("iust_debug", "Error fetching user for comment: ${e.message}")
            UserModel()
        }
    }
    
    fun addComment(threadId: String, commentText: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = SharedPref.getUserId(context)
                
                if (currentUserId.isEmpty()) {
                    Log.e("iust_debug", "Current user ID is empty")
                    withContext(Dispatchers.Main) {
                        _isCommentAdded.value = false
                    }
                    return@launch
                }
                
                val commentId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis().toString()
                
                val comment = CommentModel(
                    commentId = commentId,
                    threadId = threadId,
                    userId = currentUserId,
                    comment = commentText,
                    timeStamp = timestamp,
                    likedBy = emptyList(),
                    likes = 0,
                    parentCommentId = null // Top-level comment
                )
                
                // Add to cache immediately for optimistic update
                commentCache[commentId] = comment
                
                // Update UI immediately
                val currentList = _commentsAndUsers.value?.toMutableList() ?: mutableListOf()
                val user = userCache[currentUserId] ?: UserModel()
                currentList.add(0, comment to user)
                
                withContext(Dispatchers.Main) {
                    _commentsAndUsers.value = currentList
                }
                
                // Save to database
                commentsRef.child(commentId).setValue(comment).await()
                updateThreadCommentCount(threadId)
                
                withContext(Dispatchers.Main) {
                    _isCommentAdded.value = true
                }
                
                Log.d("iust_debug", "Comment added successfully: $commentId")
            } catch (e: Exception) {
                Log.e("iust_debug", "Failed to add comment: ${e.message}")
                
                // Remove from cache if failed
                val commentId = UUID.randomUUID().toString()
                commentCache.remove(commentId)
                
                // Revert UI update
                val currentList = _commentsAndUsers.value?.toMutableList() ?: mutableListOf()
                currentList.removeAt(0) // Remove the first comment we added
                
                withContext(Dispatchers.Main) {
                    _commentsAndUsers.value = currentList
                    _isCommentAdded.value = false
                }
            }
        }
    }
    
    fun addReply(threadId: String, parentCommentId: String, replyText: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = SharedPref.getUserId(context)
                
                if (currentUserId.isEmpty()) {
                    Log.e("iust_debug", "Current user ID is empty")
                    withContext(Dispatchers.Main) {
                        _isCommentAdded.value = false
                    }
                    return@launch
                }
                
                val replyId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis().toString()
                
                val reply = CommentModel(
                    commentId = replyId,
                    threadId = threadId,
                    userId = currentUserId,
                    comment = replyText,
                    timeStamp = timestamp,
                    likedBy = emptyList(),
                    likes = 0,
                    parentCommentId = parentCommentId // This makes it a reply
                )
                
                // Add to cache immediately for optimistic update
                commentCache[replyId] = reply
                
                // Update UI immediately
                val currentList = _commentsAndUsers.value?.toMutableList() ?: mutableListOf()
                val user = userCache[currentUserId] ?: UserModel()
                currentList.add(reply to user)
                
                withContext(Dispatchers.Main) {
                    _commentsAndUsers.value = currentList
                }
                
                // Save to database
                commentsRef.child(replyId).setValue(reply).await()
                
                withContext(Dispatchers.Main) {
                    _isCommentAdded.value = true
                }
                
                Log.d("iust_debug", "Reply added successfully: $replyId")
            } catch (e: Exception) {
                Log.e("iust_debug", "Failed to add reply: ${e.message}")
                
                // Remove from cache if failed
                val replyId = UUID.randomUUID().toString()
                commentCache.remove(replyId)
                
                // Revert UI update
                val currentList = _commentsAndUsers.value?.toMutableList() ?: mutableListOf()
                currentList.removeAt(currentList.size - 1) // Remove the last reply we added
                
                withContext(Dispatchers.Main) {
                    _commentsAndUsers.value = currentList
                    _isCommentAdded.value = false
                }
            }
        }
    }
    
    private suspend fun updateThreadCommentCount(threadId: String) {
        try {
            val threadSnapshot = threadsRef.child(threadId).get().await()
            val thread = threadSnapshot.getValue(com.yasir.iustthread.domain.model.ThreadModel::class.java)
            thread?.let {
                val newCommentCount = (it.comments.toIntOrNull() ?: 0) + 1
                threadsRef.child(threadId).child("comments").setValue(newCommentCount.toString()).await()
                Log.d("iust_debug", "Updated thread comment count: $newCommentCount")
            }
        } catch (e: Exception) {
            Log.e("iust_debug", "Failed to update thread comment count: ${e.message}")
        }
    }
    
    fun toggleCommentLike(commentId: String, userId: String, isLiked: Boolean) {
        if (commentId.isEmpty() || userId.isEmpty()) {
            return
        }
        
        // Get current comment from cache or current data
        val currentComment = commentCache[commentId] ?: _commentsAndUsers.value?.find { it.first.commentId == commentId }?.first
        
        if (currentComment == null) {
            Log.e("iust_debug", "Comment not found for like toggle")
            return
        }
        
        // Calculate new values
        val newLikesCount = if (isLiked) currentComment.likes - 1 else currentComment.likes + 1
        val newLikedBy = if (isLiked) {
            currentComment.likedBy.toMutableList().apply { remove(userId) }
        } else {
            currentComment.likedBy.toMutableList().apply { add(userId) }
        }
        
        // Create updated comment
        val updatedComment = currentComment.copy(
            likes = newLikesCount,
            likedBy = newLikedBy
        )
        
        // Update cache immediately for optimistic UI
        commentCache[commentId] = updatedComment
        
        // Update UI immediately (optimistic update)
        val currentList = _commentsAndUsers.value?.toMutableList() ?: mutableListOf()
        val index = currentList.indexOfFirst { it.first.commentId == commentId }
        if (index != -1) {
            val user = currentList[index].second
            currentList[index] = updatedComment to user
            _commentsAndUsers.value = currentList
        }
        
        // Update backend in background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                commentsRef.child(commentId).setValue(updatedComment).await()
                Log.d("iust_debug", "Comment like toggle successful: $commentId")
            } catch (e: Exception) {
                Log.e("iust_debug", "Failed to toggle comment like: ${e.message}")
                
                // Revert optimistic update on error
                withContext(Dispatchers.Main) {
                    commentCache[commentId] = currentComment
                    val revertedList = _commentsAndUsers.value?.toMutableList() ?: mutableListOf()
                    val revertIndex = revertedList.indexOfFirst { it.first.commentId == commentId }
                    if (revertIndex != -1) {
                        val user = revertedList[revertIndex].second
                        revertedList[revertIndex] = currentComment to user
                        _commentsAndUsers.value = revertedList
                    }
                }
            }
        }
    }
    
    // Function to reset the comment added flag
    fun resetCommentAddedFlag() {
        _isCommentAdded.value = false
    }
    
    // Function to delete a comment (only for user's own comments)
    fun deleteComment(commentId: String, context: Context) {
        val currentUserId = SharedPref.getUserId(context)
        
        if (currentUserId.isEmpty()) {
            Log.e("iust_debug", "Current user ID is empty")
            return
        }
        
        // Get the comment to check if it belongs to current user
        val commentToDelete = commentCache[commentId] ?: _commentsAndUsers.value?.find { it.first.commentId == commentId }?.first
        
        if (commentToDelete == null) {
            Log.e("iust_debug", "Comment not found for deletion")
            return
        }
        
        // Check if the comment belongs to current user
        if (commentToDelete.userId != currentUserId) {
            Log.e("iust_debug", "User not authorized to delete this comment")
            return
        }
        
        // Remove from cache immediately for optimistic update
        commentCache.remove(commentId)
        
        // Update UI immediately (optimistic update)
        val currentList = _commentsAndUsers.value?.toMutableList() ?: mutableListOf()
        currentList.removeAll { it.first.commentId == commentId }
        
        _commentsAndUsers.value = currentList
        
        // Delete from database in background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                commentsRef.child(commentId).removeValue().await()
                
                // Update thread comment count if it's a top-level comment
                if (commentToDelete.parentCommentId == null) {
                    updateThreadCommentCountOnDelete(commentToDelete.threadId)
                }
                
                Log.d("iust_debug", "Comment deleted successfully: $commentId")
            } catch (e: Exception) {
                Log.e("iust_debug", "Failed to delete comment: ${e.message}")
                
                // Revert optimistic update on error
                withContext(Dispatchers.Main) {
                    commentCache[commentId] = commentToDelete
                    val revertedList = _commentsAndUsers.value?.toMutableList() ?: mutableListOf()
                    val user = userCache[commentToDelete.userId] ?: UserModel()
                    revertedList.add(commentToDelete to user)
                    _commentsAndUsers.value = revertedList
                }
            }
        }
    }
    
    private suspend fun updateThreadCommentCountOnDelete(threadId: String) {
        try {
            val threadSnapshot = threadsRef.child(threadId).get().await()
            val thread = threadSnapshot.getValue(com.yasir.iustthread.domain.model.ThreadModel::class.java)
            thread?.let {
                val newCommentCount = maxOf(0, (it.comments.toIntOrNull() ?: 0) - 1)
                threadsRef.child(threadId).child("comments").setValue(newCommentCount.toString()).await()
                Log.d("iust_debug", "Updated thread comment count after deletion: $newCommentCount")
            }
        } catch (e: Exception) {
            Log.e("iust_debug", "Failed to update thread comment count after deletion: ${e.message}")
        }
    }
    
    // Debug function to create sample comments for testing
    fun createSampleComments(threadId: String, context: Context) {
        Log.d("iust_debug", "Creating sample comments for thread: $threadId")
        
        val sampleComments = listOf(
            "This is a great post! 👍",
            "Thanks for sharing this!",
            "I totally agree with you",
            "Amazing content! 🔥",
            "Keep up the good work!"
        )
        
        sampleComments.forEachIndexed { index, commentText ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val currentUserId = SharedPref.getUserId(context)
                    if (currentUserId.isEmpty()) {
                        Log.e("iust_debug", "Current user ID is empty for sample comment")
                        return@launch
                    }
                    
                    val commentId = UUID.randomUUID().toString()
                    val timestamp = (System.currentTimeMillis() - (index * 60000)).toString() // Stagger timestamps
                    
                    val comment = CommentModel(
                        commentId = commentId,
                        threadId = threadId,
                        userId = currentUserId,
                        comment = commentText,
                        timeStamp = timestamp,
                        likedBy = emptyList(),
                        likes = 0
                    )
                    
                    commentsRef.child(commentId).setValue(comment).await()
                    Log.d("iust_debug", "Sample comment created: $commentId")
                } catch (e: Exception) {
                    Log.e("iust_debug", "Failed to create sample comment: ${e.message}")
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up listeners
        commentsListener?.let { commentsRef.removeEventListener(it) }
        usersListener?.let { usersRef.removeEventListener(it) }
        commentCache.clear()
        userCache.clear()
    }
} 