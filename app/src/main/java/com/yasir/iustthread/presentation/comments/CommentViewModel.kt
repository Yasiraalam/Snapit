package com.yasir.iustthread.presentation.comments

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.yasir.iustthread.domain.model.CommentModel
import com.yasir.iustthread.domain.model.UserModel
import com.yasir.iustthread.utils.SharedPref
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
        Log.d("CommentViewModel", "Fetching comments for thread: $threadId")
        
        commentsRef.orderByChild("threadId").equalTo(threadId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("CommentViewModel", "Comments snapshot size: ${snapshot.childrenCount}")
                    val result = mutableListOf<Pair<CommentModel, UserModel>>()
                    
                    if (snapshot.childrenCount == 0L) {
                        Log.d("CommentViewModel", "No comments found for thread: $threadId")
                        _commentsAndUsers.postValue(result)
                        return
                    }
                    
                    var processedCount = 0
                    val totalComments = snapshot.childrenCount.toInt()
                    
                    for (commentSnapshot in snapshot.children) {
                        val comment = commentSnapshot.getValue(CommentModel::class.java)
                        Log.d("CommentViewModel", "Comment: $comment")
                        
                        comment?.let {
                            fetchUserForComment(it) { user ->
                                result.add(it to user)
                                processedCount++
                                
                                if (processedCount == totalComments) {
                                    Log.d("CommentViewModel", "All comments fetched: ${result.size}")
                                    _commentsAndUsers.postValue(result)
                                }
                            }
                        } ?: run {
                            processedCount++
                            if (processedCount == totalComments) {
                                _commentsAndUsers.postValue(result)
                            }
                        }
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e("CommentViewModel", "Failed to fetch comments: ${error.message}")
                    _commentsAndUsers.postValue(emptyList())
                }
            })
    }
    
    private fun fetchUserForComment(
        comment: CommentModel,
        onResult: (UserModel) -> Unit
    ) {
        if (comment.userId.isEmpty()) {
            Log.e("CommentViewModel", "Comment userId is empty")
            onResult(UserModel())
            return
        }
        
        usersRef.child(comment.userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(UserModel::class.java)
                    if (user != null) {
                        Log.d("CommentViewModel", "Fetched user for comment: ${user.username}")
                        onResult(user)
                    } else {
                        Log.e("CommentViewModel", "User not found for comment: ${comment.userId}")
                        onResult(UserModel())
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e("CommentViewModel", "Failed to fetch user for comment: ${error.message}")
                    onResult(UserModel())
                }
            })
    }
    
    fun addComment(threadId: String, commentText: String, context: Context) {
        val currentUserId = SharedPref.getUserId(context)
        
        if (currentUserId.isEmpty()) {
            Log.e("CommentViewModel", "Current user ID is empty")
            return
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
        
        Log.d("CommentViewModel", "Adding comment: $comment")
        
        commentsRef.child(commentId).setValue(comment)
            .addOnSuccessListener {
                Log.d("CommentViewModel", "Comment added successfully")
                _isCommentAdded.postValue(true)
                // Update thread comment count
                updateThreadCommentCount(threadId)
            }
            .addOnFailureListener { exception ->
                Log.e("CommentViewModel", "Failed to add comment: $exception")
                _isCommentAdded.postValue(false)
            }
    }
    
    private fun updateThreadCommentCount(threadId: String) {
        val threadRef = database.getReference("threads").child(threadId)
        threadRef.get().addOnSuccessListener { snapshot ->
            val thread = snapshot.getValue(com.yasir.iustthread.domain.model.ThreadModel::class.java)
            thread?.let {
                val newCommentCount = (it.comments.toIntOrNull() ?: 0) + 1
                threadRef.child("comments").setValue(newCommentCount.toString())
                    .addOnSuccessListener {
                        Log.d("CommentViewModel", "Thread comment count updated to: $newCommentCount")
                    }
                    .addOnFailureListener { exception ->
                        Log.e("CommentViewModel", "Failed to update thread comment count: $exception")
                    }
            }
        }
    }
    
    fun toggleCommentLike(commentId: String, userId: String, isLiked: Boolean) {
        val commentRef = commentsRef.child(commentId)
        commentRef.get().addOnSuccessListener { snapshot ->
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
                
                commentRef.setValue(updatedComment)
                    .addOnSuccessListener {
                        Log.d("CommentViewModel", "Comment like updated: $newLikesCount")
                    }
                    .addOnFailureListener { exception ->
                        Log.e("CommentViewModel", "Failed to update comment like: $exception")
                    }
            }
        }
    }
} 