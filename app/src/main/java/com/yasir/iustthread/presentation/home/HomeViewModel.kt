package com.yasir.iustthread.presentation.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.yasir.iustthread.domain.model.ThreadModel
import com.yasir.iustthread.domain.model.UserModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class HomeViewModel : ViewModel() {

    private val database = FirebaseDatabase.getInstance()
    private val allThreads = database.getReference("threads")
    private val allUsers = database.getReference("Users")

    private val _threadsAndUsers = MutableLiveData<List<Pair<ThreadModel, UserModel>>>()
    val threadsAndUsers: LiveData<List<Pair<ThreadModel, UserModel>>> = _threadsAndUsers

    // Cache for optimistic updates
    private val threadCache = mutableMapOf<String, ThreadModel>()
    private val userCache = mutableMapOf<String, UserModel>()

    // Firebase listeners
    private var threadsListener: ValueEventListener? = null
    private var usersListener: ValueEventListener? = null

    init {
        setupRealTimeListeners()
    }

    private fun setupRealTimeListeners() {
        // Listen for real-time thread updates
        threadsListener = allThreads.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val result = mutableListOf<Pair<ThreadModel, UserModel>>()
                        
                        if (snapshot.childrenCount == 0L) {
                            withContext(Dispatchers.Main) {
                                _threadsAndUsers.value = result
                            }
                            return@launch
                        }

                        for (threadSnapshot in snapshot.children) {
                            val thread = threadSnapshot.getValue(ThreadModel::class.java)
                            thread?.let {
                                // Cache the thread
                                threadCache[it.threadId] = it
                                
                                // Get user from cache or fetch
                                val user = userCache[it.userId] ?: fetchUserFromThread(it)
                                if (user.uid?.isNotEmpty() == true) {
                                    userCache[it.userId] = user
                                }
                                result.add(0, it to user)
                            }
                        }
                        
                        withContext(Dispatchers.Main) {
                            _threadsAndUsers.value = result
                        }
                    } catch (e: Exception) {
                        Log.e("iust_debug", "Error in threads listener: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("iust_debug", "Threads listener cancelled: ${error.message}")
            }
        })

        // Listen for real-time user updates
        usersListener = allUsers.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(UserModel::class.java)
                            user?.let {
                                userCache[it.uid.toString()] = it
                            }
                        }
                        
                        // Refresh threads to update user info
                        refreshThreadsWithCachedUsers()
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

    private suspend fun refreshThreadsWithCachedUsers() {
        val currentThreads = _threadsAndUsers.value ?: return
        val updatedThreads = currentThreads.map { (thread, _) ->
            val user = userCache[thread.userId] ?: UserModel()
            thread to user
        }
        
        withContext(Dispatchers.Main) {
            _threadsAndUsers.value = updatedThreads
        }
    }
    
    // Debug function to test database connection
    fun testDatabaseConnection() {
        Log.d("iust_debug", "Testing database connection...")
        database.getReference(".info/connected").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                Log.d("iust_debug", "Database connected: $connected")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("iust_debug", "Database connection test failed: ${error.message}")
            }
        })
    }
    
    // Function to create sample data for testing
    fun createSampleData() {
        Log.d("iust_debug", "Creating sample data...")
        
        // Create a sample user
        val sampleUser = UserModel(
            email = "test@example.com",
            name = "Test User",
            username = "testuser",
            bio = "This is a test user",
            imageUri = "",
            uid = "test_user_id"
        )
        
        // Create a sample thread
        val sampleThread = ThreadModel(
            threadId = "sample_thread_1",
            thread = "This is a sample thread for testing the home screen data display.",
            image = "",
            userId = "test_user_id",
            timeStamp = System.currentTimeMillis().toString(),
            likedBy = emptyList(),
            likes = 0,
            comments = "0"
        )
        
        // Save user to database
        allUsers.child("test_user_id").setValue(sampleUser)
            .addOnSuccessListener {
                Log.d("iust_debug", "Sample user created successfully")
                // Save thread to database
                allThreads.child("sample_thread_1").setValue(sampleThread)
                    .addOnSuccessListener {
                        Log.d("iust_debug", "Sample thread created successfully")
                    }
                    .addOnFailureListener { exception ->
                        Log.e("iust_debug", "Failed to create sample thread: $exception")
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("iust_debug", "Failed to create sample user: $exception")
            }
    }

    private suspend fun fetchUserFromThread(thread: ThreadModel): UserModel {
        return try {
            if (thread.userId.isEmpty()) {
                return UserModel()
            }
            
            // Check cache first
            userCache[thread.userId]?.let { return it }
            
            val userSnapshot = allUsers.child(thread.userId).get().await()
            userSnapshot.getValue(UserModel::class.java) ?: UserModel()
        } catch (e: Exception) {
            UserModel()
        }
    }
    
    fun toggleThreadLike(threadId: String, userId: String, isLiked: Boolean, callback: (Int) -> Unit) {
        if (threadId.isEmpty() || userId.isEmpty()) {
            return
        }
        
        // Get current thread from cache or current data
        val currentThread = threadCache[threadId] ?: _threadsAndUsers.value?.find { it.first.threadId == threadId }?.first
        
        if (currentThread == null) {
            Log.e("iust_debug", "Thread not found for like toggle")
            return
        }
        
        // Calculate new values
        val newLikesCount = if (isLiked) currentThread.likes - 1 else currentThread.likes + 1
        val newLikedBy = if (isLiked) {
            currentThread.likedBy.toMutableList().apply { remove(userId) }
        } else {
            currentThread.likedBy.toMutableList().apply { add(userId) }
        }
        
        // Create updated thread
        val updatedThread = currentThread.copy(
            likes = newLikesCount,
            likedBy = newLikedBy
        )
        
        // Update cache immediately for optimistic UI
        threadCache[threadId] = updatedThread
        
        // Update UI immediately (optimistic update)
        val currentList = _threadsAndUsers.value?.toMutableList() ?: mutableListOf()
        val index = currentList.indexOfFirst { it.first.threadId == threadId }
        if (index != -1) {
            val user = currentList[index].second
            currentList[index] = updatedThread to user
            _threadsAndUsers.value = currentList
        }
        
        // Call callback immediately
        callback(newLikesCount)
        
        // Update backend in background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                allThreads.child(threadId).setValue(updatedThread).await()
                Log.d("iust_debug", "Like toggle successful for thread: $threadId")
            } catch (e: Exception) {
                Log.e("iust_debug", "Failed to toggle like: ${e.message}")
                
                // Revert optimistic update on error
                withContext(Dispatchers.Main) {
                    threadCache[threadId] = currentThread
                    val revertedList = _threadsAndUsers.value?.toMutableList() ?: mutableListOf()
                    val revertIndex = revertedList.indexOfFirst { it.first.threadId == threadId }
                    if (revertIndex != -1) {
                        val user = revertedList[revertIndex].second
                        revertedList[revertIndex] = currentThread to user
                        _threadsAndUsers.value = revertedList
                    }
                    callback(currentThread.likes)
                }
            }
        }
    }
    
    // Function to manually refresh data
    fun refreshData() {
        // The real-time listeners will automatically handle updates
        Log.d("iust_debug", "Data refresh requested - listeners are already active")
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up listeners
        threadsListener?.let { allThreads.removeEventListener(it) }
        usersListener?.let { allUsers.removeEventListener(it) }
        threadCache.clear()
        userCache.clear()
    }
}