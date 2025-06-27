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

    private val _threadsAndUsers = MutableLiveData<List<Pair<ThreadModel, UserModel>>>()
    val threadsAndUsers: LiveData<List<Pair<ThreadModel, UserModel>>> = _threadsAndUsers

    init {
        fetchThreadAndUsers()
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
        database.getReference("Users").child("test_user_id").setValue(sampleUser)
            .addOnSuccessListener {
                Log.d("iust_debug", "Sample user created successfully")
                // Save thread to database
                database.getReference("threads").child("sample_thread_1").setValue(sampleThread)
                    .addOnSuccessListener {
                        Log.d("iust_debug", "Sample thread created successfully")
                        // Refresh the data
                        fetchThreadAndUsers()
                    }
                    .addOnFailureListener { exception ->
                        Log.e("iust_debug", "Failed to create sample thread: $exception")
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("iust_debug", "Failed to create sample user: $exception")
            }
    }
    
    private fun fetchThreadAndUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val snapshot = allThreads.get().await()
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
                        val user = fetchUserFromThread(it)
                        result.add(0, it to user)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    _threadsAndUsers.value = result
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _threadsAndUsers.value = emptyList()
                }
            }
        }
    }

    private suspend fun fetchUserFromThread(thread: ThreadModel): UserModel {
        return try {
            if (thread.userId.isEmpty()) {
                return UserModel()
            }
            
            val userSnapshot = database.getReference("Users").child(thread.userId).get().await()
            userSnapshot.getValue(UserModel::class.java) ?: UserModel()
        } catch (e: Exception) {
            UserModel()
        }
    }
    
    fun toggleThreadLike(threadId: String, userId: String, isLiked: Boolean, callback: (Int) -> Unit) {
        if (threadId.isEmpty() || userId.isEmpty()) {
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val threadRef = allThreads.child(threadId)
                val dataSnapshot = threadRef.get().await()
                val currentThread = dataSnapshot.getValue(ThreadModel::class.java)

                currentThread?.let {
                    val newLikesCount = if (isLiked) it.likes - 1 else it.likes + 1
                    val newLikedBy = if (isLiked) {
                        it.likedBy.toMutableList().apply { remove(userId) }
                    } else {
                        it.likedBy.toMutableList().apply { add(userId) }
                    }
                    val updatedThread = it.copy(
                        likes = newLikesCount,
                        likedBy = newLikedBy
                    )
                    threadRef.setValue(updatedThread).await()
                    
                    // Refresh the data to update UI
                    fetchThreadAndUsers()
                    
                    withContext(Dispatchers.Main) {
                        callback(newLikesCount)
                    }
                }
            } catch (e: Exception) {
                Log.e("iust_debug", "Failed to toggle like: ${e.message}")
            }
        }
    }
    
    // Function to manually refresh data
    fun refreshData() {
        fetchThreadAndUsers()
    }
}