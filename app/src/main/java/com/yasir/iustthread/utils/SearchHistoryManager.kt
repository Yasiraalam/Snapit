package com.yasir.iustthread.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONException

object SearchHistoryManager {
    private const val PREF_NAME = "SearchHistory"
    private const val KEY_SEARCH_HISTORY = "search_history"
    private const val MAX_HISTORY_SIZE = 10

    fun addSearchQuery(context: Context, query: String) {
        if (query.trim().isEmpty()) return
        
        val history = getSearchHistory(context).toMutableList()
        
        // Remove if already exists (to move to top)
        history.remove(query.trim())
        
        // Add to the beginning
        history.add(0, query.trim())
        
        // Keep only the latest MAX_HISTORY_SIZE items
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(history.size - 1)
        }
        
        saveSearchHistory(context, history)
    }

    fun getSearchHistory(context: Context): List<String> {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val jsonString = sharedPreferences.getString(KEY_SEARCH_HISTORY, "[]")
        
        return try {
            val jsonArray = JSONArray(jsonString)
            val result = mutableListOf<String>()
            
            for (i in 0 until jsonArray.length()) {
                result.add(jsonArray.getString(i))
            }
            
            result
        } catch (e: JSONException) {
            emptyList()
        }
    }

    fun removeSearchQuery(context: Context, query: String) {
        val history = getSearchHistory(context).toMutableList()
        history.remove(query)
        saveSearchHistory(context, history)
    }

    fun clearSearchHistory(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        sharedPreferences.edit {
            remove(KEY_SEARCH_HISTORY)
        }
    }

    private fun saveSearchHistory(context: Context, history: List<String>) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val jsonArray = JSONArray()
        
        history.forEach { query ->
            jsonArray.put(query)
        }
        
        sharedPreferences.edit {
            putString(KEY_SEARCH_HISTORY, jsonArray.toString())
        }
    }
} 