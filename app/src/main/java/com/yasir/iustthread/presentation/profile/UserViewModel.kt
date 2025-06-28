package com.yasir.iustthread.presentation.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.yasir.iustthread.domain.model.ThreadModel
import com.yasir.iustthread.domain.model.UserModel
import com.yasir.iustthread.utils.SharedPref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class UserViewModel : ViewModel() {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    
    private val threadRef = database.getReference("threads")
    private val usersRef = database.getReference("Users")

    // LiveData for UI state
    private val _threads = MutableLiveData<List<ThreadModel>>()
    val threads: LiveData<List<ThreadModel>> = _threads
    
    private val _followersList = MutableLiveData<List<UserModel>>()
    val followersList: LiveData<List<UserModel>> = _followersList
    
    private val _followingList = MutableLiveData<List<UserModel>>()
    val followingList: LiveData<List<UserModel>> = _followingList

    private val _user = MutableLiveData<UserModel?>()
    val user: LiveData<UserModel?> = _user

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Function to refresh all user data
    fun refreshUserData(userId: String): Unit {
        if (userId.isBlank()) {
            Log.e("iust_debug", "Cannot refresh user data: userId is empty")
            _errorMessage.value = "Invalid user ID"
            _isLoading.value = false
            return
        }
        
        Log.d("iust_debug", "Refreshing user data for: $userId")
        _isLoading.value = true
        _errorMessage.value = null
        
        viewModelScope.launch {
            try {
                // Execute all operations concurrently
                val userJob = launch { fetchUser(userId) }
                val threadsJob = launch { fetchThreads(userId) }
                val followersJob = launch { getFollowers(userId) }
                val followingJob = launch { getFollowing(userId) }
                
                // Wait for all operations to complete
                userJob.join()
                threadsJob.join()
                followersJob.join()
                followingJob.join()
                
                delay(500) // Small delay to ensure UI updates properly
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e("iust_debug", "Error refreshing user data: ${e.message}")
                _errorMessage.value = "Failed to refresh data"
                _isLoading.value = false
            }
        }
    }

    // Function to clear all data
    fun clearData(): Unit {
        _threads.value = emptyList()
        _followersList.value = emptyList()
        _followingList.value = emptyList()
        _user.value = null
        _errorMessage.value = null
        Log.d("iust_debug", "Cleared all user data")
    }

    private suspend fun fetchUser(uid: String): Unit {
        if (uid.isBlank()) {
            Log.e("iust_debug", "User ID is empty")
            return
        }
        
        try {
            val snapshot = usersRef.child(uid).get().await()
            val user = snapshot.getValue(UserModel::class.java)
            
            withContext(Dispatchers.Main) {
                if (user != null) {
                    Log.d("iust_debug", "Successfully fetched user: ${user.username}")
                    _user.value = user
                } else {
                    Log.e("iust_debug", "User not found for uid: $uid")
                    _user.value = null
                    _errorMessage.value = "User not found"
                }
            }
        } catch (e: Exception) {
            Log.e("iust_debug", "Failed to fetch user: ${e.message}")
            withContext(Dispatchers.Main) {
                _user.value = null
                _errorMessage.value = "Failed to fetch user data"
            }
        }
    }
    
    fun fetchThreads(userId: String): Unit {
        if (userId.isBlank()) {
            Log.e("iust_debug", "User ID is empty for fetching threads")
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("iust_debug", "Starting to fetch threads for userId: $userId")
                val threadsList = mutableListOf<ThreadModel>()
                
                val snapshot = threadRef.get().await()
                Log.d("iust_debug", "Database snapshot exists: ${snapshot.exists()}")
                Log.d("iust_debug", "Total number of threads in database: ${snapshot.childrenCount}")
                
                for (threadSnapshot in snapshot.children) {
                    val thread = threadSnapshot.getValue(ThreadModel::class.java)
                    if (thread != null && thread.userId == userId) {
                        Log.d("iust_debug", "Adding thread to user's list: ${thread.threadId}")
                        threadsList.add(thread)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    _threads.value = threadsList.sortedByDescending { it.timeStamp }
                    Log.d("iust_debug", "Fetched ${threadsList.size} threads for user: $userId")
                }
            } catch (e: Exception) {
                Log.e("iust_debug", "Error fetching threads: ${e.message}")
                withContext(Dispatchers.Main) {
                    _threads.value = emptyList()
                    _errorMessage.value = "Failed to fetch threads"
                }
            }
        }
    }
    
    fun followUser(targetUserId: String, currentUserId: String): Unit {
        if (targetUserId.isBlank() || currentUserId.isBlank()) {
            Log.e("iust_debug", "Invalid user IDs for following")
            _errorMessage.value = "Invalid user IDs"
            return
        }
        
        if (targetUserId == currentUserId) {
            Log.e("iust_debug", "Cannot follow yourself")
            _errorMessage.value = "Cannot follow yourself"
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val followingRef = firestore.collection("following").document(currentUserId)
                val followersRef = firestore.collection("followers").document(targetUserId)
                
                // Use batch write for atomicity
                val batch = firestore.batch()
                batch.update(followingRef, "followingIds", FieldValue.arrayUnion(targetUserId))
                batch.update(followersRef, "followerIds", FieldValue.arrayUnion(currentUserId))
                batch.commit().await()
                
                Log.d("iust_debug", "Successfully followed user: $targetUserId")
                
                // Refresh following list
                getFollowing(currentUserId)
                
            } catch (e: Exception) {
                Log.e("iust_debug", "Failed to follow user: ${e.message}")
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to follow user"
                }
            }
        }
    }
    
    private suspend fun getFollowers(userId: String): Unit {
        if (userId.isBlank()) {
            Log.e("iust_debug", "User ID is empty for fetching followers")
            return
        }
        
        try {
            val followersRef = firestore.collection("followers").document(userId)
            val followersSnapshot = followersRef.get().await()
            
            val followerIds = followersSnapshot.get("followerIds") as? List<String> ?: emptyList()
            val followersList = mutableListOf<UserModel>()
            
            // Fetch user data for each follower
            for (followerId in followerIds) {
                val userSnapshot = usersRef.child(followerId).get().await()
                val user = userSnapshot.getValue(UserModel::class.java)
                user?.let { followersList.add(it) }
            }
            
            withContext(Dispatchers.Main) {
                _followersList.value = followersList
            }
        } catch (e: Exception) {
            Log.e("iust_debug", "Failed to get followers: ${e.message}")
            withContext(Dispatchers.Main) {
                _followersList.value = emptyList()
                _errorMessage.value = "Failed to fetch followers"
            }
        }
    }
    
    private suspend fun getFollowing(userId: String): Unit {
        if (userId.isBlank()) {
            Log.e("iust_debug", "User ID is empty for fetching following")
            return
        }
        
        try {
            val followingRef = firestore.collection("following").document(userId)
            val followingSnapshot = followingRef.get().await()
            
            val followingIds = followingSnapshot.get("followingIds") as? List<String> ?: emptyList()
            val followingList = mutableListOf<UserModel>()
            
            // Fetch user data for each following
            for (followingId in followingIds) {
                val userSnapshot = usersRef.child(followingId).get().await()
                val user = userSnapshot.getValue(UserModel::class.java)
                user?.let { followingList.add(it) }
            }
            
            withContext(Dispatchers.Main) {
                _followingList.value = followingList
            }
        } catch (e: Exception) {
            Log.e("iust_debug", "Failed to get following: ${e.message}")
            withContext(Dispatchers.Main) {
                _followingList.value = emptyList()
                _errorMessage.value = "Failed to fetch following"
            }
        }
    }

    fun unfollowUser(targetUserId: String, currentUserId: String): Unit {
        if (targetUserId.isBlank() || currentUserId.isBlank()) {
            Log.e("iust_debug", "Invalid user IDs for unfollowing")
            _errorMessage.value = "Invalid user IDs"
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val followingRef = firestore.collection("following").document(currentUserId)
                val followersRef = firestore.collection("followers").document(targetUserId)
                
                // Use batch write for atomicity
                val batch = firestore.batch()
                batch.update(followingRef, "followingIds", FieldValue.arrayRemove(targetUserId))
                batch.update(followersRef, "followerIds", FieldValue.arrayRemove(currentUserId))
                batch.commit().await()
                
                Log.d("iust_debug", "Successfully unfollowed user: $targetUserId")
                
                // Refresh following list
                getFollowing(currentUserId)
                
            } catch (e: Exception) {
                Log.e("iust_debug", "Failed to unfollow user: ${e.message}")
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to unfollow user"
                }
            }
        }
    }

    fun deleteThread(thread: ThreadModel): Unit {
        if (thread.threadId.isBlank()) {
            Log.e("iust_debug", "Cannot delete thread: threadId is empty")
            _errorMessage.value = "Invalid thread ID"
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Remove from database
                threadRef.child(thread.threadId).removeValue().await()
                Log.d("iust_debug", "Successfully deleted thread: ${thread.threadId}")
                
                // Update local list
                withContext(Dispatchers.Main) {
                    val currentThreads = _threads.value?.toMutableList() ?: mutableListOf()
                    currentThreads.removeAll { it.threadId == thread.threadId }
                    _threads.value = currentThreads
                }
            } catch (e: Exception) {
                Log.e("iust_debug", "Failed to delete thread: ${e.message}")
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to delete thread"
                }
            }
        }
    }

    fun updateProfile(name: String, bio: String, imageUri: Uri?, context: Context): Unit {
        val currentUserId = SharedPref.getUserId(context)
        if (currentUserId.isBlank()) {
            Log.e("iust_debug", "Cannot update profile: currentUserId is empty")
            _errorMessage.value = "User not authenticated"
            return
        }
        
        if (name.isBlank()) {
            _errorMessage.value = "Name cannot be empty"
            return
        }
        
        _isLoading.value = true
        _errorMessage.value = null
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var finalImageUri = SharedPref.getImageUrl(context)
                
                // Upload new image if provided
                if (imageUri != null) {
                    finalImageUri = uploadProfileImage(imageUri)
                }
                
                // Update user data in database
                val updatedUser = createUpdatedUser(name, bio, finalImageUri, context, currentUserId)
                usersRef.child(currentUserId).setValue(updatedUser).await()
                
                // Update SharedPref
                updateSharedPreferences(name, bio, finalImageUri, context, currentUserId)
                
                // Update local user data
                withContext(Dispatchers.Main) {
                    _user.value = updatedUser
                    _isLoading.value = false
                }
                
                Log.d("iust_debug", "Profile updated successfully")
                
            } catch (e: Exception) {
                Log.e("iust_debug", "Failed to update profile: ${e.message}")
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    _errorMessage.value = "Failed to update profile"
                }
            }
        }
    }
    
    private suspend fun uploadProfileImage(imageUri: Uri): String {
        val storageRef = storage.reference
        val imageRef = storageRef.child("Users/${UUID.randomUUID()}.jpg")
        imageRef.putFile(imageUri).await()
        return imageRef.downloadUrl.await().toString()
    }
    
    private fun createUpdatedUser(
        name: String,
        bio: String,
        imageUri: String,
        context: Context,
        currentUserId: String
    ): UserModel {
        val currentEmail = SharedPref.getEmail(context)
        val currentUsername = SharedPref.getUserName(context)
        
        return UserModel(
            email = currentEmail,
            password = "", // We don't store password in SharedPref for security
            name = name,
            bio = bio,
            username = currentUsername,
            imageUri = imageUri,
            uid = currentUserId
        )
    }
    
    private fun updateSharedPreferences(
        name: String,
        bio: String,
        imageUri: String,
        context: Context,
        currentUserId: String
    ): Unit {
        val currentEmail = SharedPref.getEmail(context)
        val currentUsername = SharedPref.getUserName(context)
        
        SharedPref.storeData(
            name,
            currentEmail,
            bio,
            currentUsername,
            imageUri,
            currentUserId,
            context
        )
    }
    
    fun clearErrorMessage(): Unit {
        _errorMessage.value = null
    }
}