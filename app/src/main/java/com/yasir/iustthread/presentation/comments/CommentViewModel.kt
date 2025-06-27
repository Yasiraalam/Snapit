package com.yasir.iustthread.presentation.comments

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
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
    
    private val _commentsAndUsers = MutableLiveData<List<Pair<CommentModel, UserModel>>>()
    val commentsAndUsers: LiveData<List<Pair<CommentModel, UserModel>>> = _commentsAndUsers
    
    private val _isCommentAdded = MutableLiveData<Boolean>()
    val isCommentAdded: LiveData<Boolean> = _isCommentAdded
    
    fun fetchComments(threadId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val snapshot = commentsRef.orderByChild("threadId").equalTo(threadId).get().await()
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
                        val user = fetchUserForComment(it)
                        result.add(it to user)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    _commentsAndUsers.value = result
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _commentsAndUsers.value = emptyList()
                }
            }
        }
    }
    
    private suspend fun fetchUserForComment(comment: CommentModel): UserModel {
        return try {
            if (comment.userId.isEmpty()) {
                return UserModel()
            }
            
            val userSnapshot = usersRef.child(comment.userId).get().await()
            userSnapshot.getValue(UserModel::class.java) ?: UserModel()
        } catch (e: Exception) {
            UserModel()
        }
    }
    
    fun addComment(threadId: String, commentText: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = SharedPref.getUserId(context)
                
                if (currentUserId.isEmpty()) {
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
                    likes = 0
                )
                
                commentsRef.child(commentId).setValue(comment).await()
                updateThreadCommentCount(threadId)
                
                withContext(Dispatchers.Main) {
                    _isCommentAdded.value = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isCommentAdded.value = false
                }
            }
        }
    }
    
    private suspend fun updateThreadCommentCount(threadId: String) {
        try {
            val threadRef = database.getReference("threads").child(threadId)
            val snapshot = threadRef.get().await()
            val thread = snapshot.getValue(com.yasir.iustthread.domain.model.ThreadModel::class.java)
            thread?.let {
                val newCommentCount = (it.comments.toIntOrNull() ?: 0) + 1
                threadRef.child("comments").setValue(newCommentCount.toString()).await()
            }
        } catch (e: Exception) {
            // Handle error silently
        }
    }
    
    fun toggleCommentLike(commentId: String, userId: String, isLiked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val commentRef = commentsRef.child(commentId)
                val snapshot = commentRef.get().await()
                val comment = snapshot.getValue(CommentModel::class.java)
                
                comment?.let {
                    val newLikesCount = if (isLiked) it.likes - 1 else it.likes + 1
                    val newLikedBy = if (isLiked) {
                        it.likedBy.toMutableList().apply { remove(userId) }
                    } else {
                        it.likedBy.toMutableList().apply { add(userId) }
                    }
                    
                    val updatedComment = it.copy(
                        likes = newLikesCount,
                        likedBy = newLikedBy
                    )
                    
                    commentRef.setValue(updatedComment).await()
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
} 