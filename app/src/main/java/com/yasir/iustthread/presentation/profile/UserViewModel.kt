package com.yasir.iustthread.presentation.profile

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yasir.iustthread.domain.model.ThreadModel
import com.yasir.iustthread.domain.model.UserModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserViewModel : ViewModel() {

    private val database = FirebaseDatabase.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    val threadRef = database.getReference("threads")
    val usersRef = database.getReference("Users")

    private val _threads = MutableLiveData<List<ThreadModel>>()
    val threads: LiveData<List<ThreadModel>> = _threads
    
    //follower ids
    private val _followersList = MutableLiveData<List<UserModel>>()
    val followersList: LiveData<List<UserModel>> = _followersList
    
    //following ids
    private val _followingList = MutableLiveData<List<UserModel>>()
    val followingList: LiveData<List<UserModel>> = _followingList

    private val _users = MutableLiveData<UserModel?>()
    val users: LiveData<UserModel?> = _users

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Function to refresh all user data
    fun refreshUserData(userId: String) {
        if (userId.isNotEmpty()) {
            Log.d("iust_debug", "Refreshing user data for: $userId")
            _isLoading.value = true
            fetchUser(userId)
            fetchThreads(userId)
            getFollowers(userId)
            getFollowing(userId)
            // Set loading to false after all operations complete
            viewModelScope.launch {
                kotlinx.coroutines.delay(1000) // Give time for all operations
                _isLoading.value = false
            }
        } else {
            Log.e("iust_debug", "Cannot refresh user data: userId is empty")
            _isLoading.value = false
        }
    }

    // Function to clear all data
    fun clearData() {
        _threads.value = emptyList()
        _followersList.value = emptyList()
        _followingList.value = emptyList()
        _users.value = null
        Log.d("iust_debug", "Cleared all user data")
    }

    fun fetchUser(uid: String) {
        if (uid.isEmpty()) {
            Log.e("iust_debug", "User ID is empty")
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val snapshot = usersRef.child(uid).get().await()
                val user = snapshot.getValue(UserModel::class.java)
                withContext(Dispatchers.Main) {
                    if (user != null) {
                        Log.d("iust_debug", "Successfully fetched user: ${user.username}")
                        _users.value = user
                    } else {
                        Log.e("iust_debug", "User not found for uid: $uid")
                        _users.value = null
                    }
                }
            } catch (e: Exception) {
                Log.e("iust_debug", "Failed to fetch user: ${e.message}")
                withContext(Dispatchers.Main) {
                    _users.value = null
                }
            }
        }
    }
    
    fun fetchThreads(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("iust_debug", "Starting to fetch threads for userId: $userId")
                val threadsList = mutableListOf<ThreadModel>()
                
                // Try fetching all threads first to debug
                val snapshot = threadRef.get().await()
                Log.d("iust_debug", "Database snapshot exists: ${snapshot.exists()}")
                Log.d("iust_debug", "Total number of threads in database: ${snapshot.childrenCount}")
                
                for (threadSnapshot in snapshot.children) {
                    Log.d("iust_debug", "Processing thread: ${threadSnapshot.key}")
                    val thread = threadSnapshot.getValue(ThreadModel::class.java)
                    if (thread != null) {
                        Log.d("iust_debug", "Thread found: ${thread.threadId}, userId: ${thread.userId}, content: ${thread.thread.take(50)}")
                        if (thread.userId == userId) {
                            Log.d("iust_debug", "Adding thread to user's list: ${thread.threadId}")
                            threadsList.add(thread)
                        }
                    } else {
                        Log.e("iust_debug", "Failed to parse thread: ${threadSnapshot.key}")
                    }
                }
                
                withContext(Dispatchers.Main) {
                    _threads.value = threadsList
                    Log.d("iust_debug", "Fetched ${threadsList.size} threads for user: $userId")
                    Log.d("iust_debug", "Threads list: ${threadsList.map { it.threadId }}")
                }
            } catch (e: Exception) {
                Log.e("iust_debug", "Error fetching threads: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _threads.value = emptyList()
                }
            }
        }
    }
    
    private val firestoreDb = Firebase.firestore
    
    fun followUsers(userid: String, currentUserId: String) {
        if (userid.isEmpty() || currentUserId.isEmpty()) {
            Log.e("iust_debug", "Invalid user IDs for following")
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ref = firestoreDb.collection("following").document(currentUserId)
                val followerRef = firestoreDb.collection("followers").document(userid)
                
                ref.update("followingIds", FieldValue.arrayUnion(userid)).await()
                Log.d("iust_debug", "Successfully added to following")
                
                followerRef.update("followerIds", FieldValue.arrayUnion(currentUserId)).await()
                Log.d("iust_debug", "Successfully added to followers")
            } catch (e: Exception) {
                Log.e("iust_debug", "Failed to follow user: ${e.message}")
            }
        }
    }
    
    fun getFollowers(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val followersRef = firestore.collection("followers").document(userId)
                val followersSnapshot = followersRef.get().await()
                
                val followerIds = followersSnapshot.get("followerIds") as? List<String> ?: emptyList()
                val followersList = mutableListOf<UserModel>()
                
                for (followerId in followerIds) {
                    val userRef = database.getReference("Users").child(followerId)
                    val userSnapshot = userRef.get().await()
                    val user = userSnapshot.getValue(UserModel::class.java)
                    user?.let { followersList.add(it) }
                }
                
                withContext(Dispatchers.Main) {
                    _followersList.value = followersList
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _followersList.value = emptyList()
                }
            }
        }
    }
    
    fun getFollowing(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val followingRef = firestore.collection("following").document(userId)
                val followingSnapshot = followingRef.get().await()
                
                val followingIds = followingSnapshot.get("followingIds") as? List<String> ?: emptyList()
                val followingList = mutableListOf<UserModel>()
                
                for (followingId in followingIds) {
                    val userRef = database.getReference("Users").child(followingId)
                    val userSnapshot = userRef.get().await()
                    val user = userSnapshot.getValue(UserModel::class.java)
                    user?.let { followingList.add(it) }
                }
                
                withContext(Dispatchers.Main) {
                    _followingList.value = followingList
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _followingList.value = emptyList()
                }
            }
        }
    }

    fun unfollowUsers(userid: String, currentUserId: String) {
        if (userid.isEmpty() || currentUserId.isEmpty()) {
            Log.e("iust_debug", "Invalid user IDs for unfollowing")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ref = firestoreDb.collection("following").document(currentUserId)
                val followerRef = firestoreDb.collection("followers").document(userid)
                ref.update("followingIds", FieldValue.arrayRemove(userid)).await()
                Log.d("iust_debug", "Successfully removed from following")
                followerRef.update("followerIds", FieldValue.arrayRemove(currentUserId)).await()
                Log.d("iust_debug", "Successfully removed from followers")
            } catch (e: Exception) {
                Log.e("iust_debug", "Failed to unfollow user: ${e.message}")
            }
        }
    }

    // Function to test database connection and create sample data
    fun testDatabaseConnection(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("iust_debug", "Testing database connection for user: $userId")
                
                // Test if we can read from the database
                val snapshot = threadRef.get().await()
                Log.d("iust_debug", "Database connection successful. Total threads: ${snapshot.childrenCount}")
                
                // Create a test thread if none exist
                if (snapshot.childrenCount == 0L) {
                    Log.d("iust_debug", "No threads found, creating test thread")
                    val testThread = ThreadModel(
                        threadId = "test_thread_${System.currentTimeMillis()}",
                        thread = "This is a test post to verify the profile screen is working correctly.",
                        image = "",
                        userId = userId,
                        timeStamp = System.currentTimeMillis().toString(),
                        likedBy = emptyList(),
                        likes = 0,
                        comments = "0"
                    )
                    
                    threadRef.child(testThread.threadId).setValue(testThread).await()
                    Log.d("iust_debug", "Test thread created successfully")
                    
                    // Fetch threads again
                    fetchThreads(userId)
                } else {
                    Log.d("iust_debug", "Found ${snapshot.childrenCount} existing threads")
                    // Show all threads for debugging
                    for (threadSnapshot in snapshot.children) {
                        val thread = threadSnapshot.getValue(ThreadModel::class.java)
                        Log.d("iust_debug", "Thread: ${thread?.threadId}, UserId: ${thread?.userId}, Content: ${thread?.thread?.take(50)}")
                    }
                }
            } catch (e: Exception) {
                Log.e("iust_debug", "Database connection test failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}