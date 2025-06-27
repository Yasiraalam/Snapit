package com.yasir.iustthread.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.content.edit

object SharedPref {
    @SuppressLint("CommitPrefEdits")
    fun storeData(
        name: String,
        email: String,
        bio: String,
        username: String,
        imageUrl: String,
        userId: String,
        context: Context
    ) {
        val sharedPreferences = context.getSharedPreferences("Users", MODE_PRIVATE)
        sharedPreferences.edit {
            putString("name", name)
            putString("email", email)
            putString("bio", bio)
            putString("userName", username)
            putString("imageUrl", imageUrl)
            putString("userId", userId)
        }
    }

    fun getUserName(context:Context):String{
        val sharedPreferences = context.getSharedPreferences("Users",MODE_PRIVATE)
        return sharedPreferences.getString("userName","")!!
    }

    fun getName(context:Context):String{
        val sharedPreferences = context.getSharedPreferences("Users",MODE_PRIVATE)
        return sharedPreferences.getString("name","")!!
    }

    fun getEmail(context:Context):String{
        val sharedPreferences = context.getSharedPreferences("Users",MODE_PRIVATE)
        return sharedPreferences.getString("email","")!!
    }

    fun getBio(context:Context):String{
        val sharedPreferences = context.getSharedPreferences("Users",MODE_PRIVATE)
        return sharedPreferences.getString("bio","Bio not available")!!
    }

    fun getImageUrl(context:Context):String{
        val sharedPreferences = context.getSharedPreferences("Users",MODE_PRIVATE)
        return sharedPreferences.getString("imageUrl","")!!
    }

    fun getUserId(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("Users", MODE_PRIVATE)
        return sharedPreferences.getString("userId", "") ?: ""
    }

    fun clearData(context: Context) {
        val sharedPreferences = context.getSharedPreferences("Users", MODE_PRIVATE)
        sharedPreferences.edit {
            clear()
        }
    }
}