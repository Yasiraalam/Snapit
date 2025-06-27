package com.yasir.iustthread.presentation.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.yasir.iustthread.domain.model.ThreadModel
import com.yasir.iustthread.domain.model.UserModel
import com.yasir.iustthread.utils.SearchHistoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SearchViewModel : ViewModel() {

    private val database = FirebaseDatabase.getInstance()
    val users = database.getReference("Users")

    private val _users = MutableLiveData<List<UserModel>>()
    val usersList: LiveData<List<UserModel>> = _users

    private val _searchHistory = MutableLiveData<List<String>>()
    val searchHistory: LiveData<List<String>> = _searchHistory

    init {
        fetchUsers()
    }

    fun loadSearchHistory(context: Context) {
        val history = SearchHistoryManager.getSearchHistory(context)
        _searchHistory.value = history
    }

    fun addSearchQuery(context: Context, query: String) {
        SearchHistoryManager.addSearchQuery(context, query)
        loadSearchHistory(context)
    }

    fun removeSearchQuery(context: Context, query: String) {
        SearchHistoryManager.removeSearchQuery(context, query)
        loadSearchHistory(context)
    }

    fun clearSearchHistory(context: Context) {
        SearchHistoryManager.clearSearchHistory(context)
        loadSearchHistory(context)
    }

    private fun fetchUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val snapshot = users.get().await()
                val result = mutableListOf<UserModel>()
                for (threadSnapshot in snapshot.children) {
                    val thread = threadSnapshot.getValue(UserModel::class.java)
                    thread?.let { result.add(it) }
                }
                withContext(Dispatchers.Main) {
                    _users.value = result
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _users.value = emptyList()
                }
            }
        }
    }
    
    fun fetchUserFromThread(
        thread: ThreadModel,
        onResult: (UserModel) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userSnapshot = database.getReference("Users").child(thread.userId).get().await()
                val user = userSnapshot.getValue(UserModel::class.java)
                user?.let { 
                    withContext(Dispatchers.Main) {
                        onResult(it)
                    }
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
}