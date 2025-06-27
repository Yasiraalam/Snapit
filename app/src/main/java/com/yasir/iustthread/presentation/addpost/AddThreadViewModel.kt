package com.yasir.iustthread.presentation.addpost

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.yasir.iustthread.domain.model.ThreadModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class AddThreadViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance()
    val userRef = database.getReference("threads")

    private val _isPosted = MutableLiveData<Boolean>()
    val isPosted: LiveData<Boolean> = _isPosted

    private val storageref = Firebase.storage.reference

    fun saveImage(
        thread: String,
        userId: String,
        imageUri: Uri,
        loading: MutableState<Boolean>,
        context: Context
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    loading.value = true
                }

                val imageRef = storageref.child("threads/${UUID.randomUUID()}.jpg")
                
                val inputStream = context.contentResolver.openInputStream(imageUri)
                if (inputStream != null) {
                    try {
                        val uploadTask = imageRef.putStream(inputStream).await()
                        val downloadUri = imageRef.downloadUrl.await()
                        saveData(thread, userId, downloadUri.toString(), loading)
                    } catch (e: Exception) {
                        // Fallback to text-only post if image upload fails
                        saveData(thread, userId, "", loading)
                    } finally {
                        inputStream.close()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loading.value = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.value = false
                }
            }
        }
    }

    fun saveTextOnly(
        thread: String,
        userId: String,
        loading: MutableState<Boolean>
    ) {
        saveData(thread, userId, "", loading)
    }

    fun saveData(
        thread: String,
        userId: String,
        image: String,
        loading: MutableState<Boolean>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val threadId = userRef.push().key ?: UUID.randomUUID().toString()
                val threadData = ThreadModel(
                    threadId = threadId,
                    thread = thread,
                    image = image,
                    userId = userId,
                    timeStamp = System.currentTimeMillis().toString(),
                    likedBy = emptyList(),
                    likes = 0,
                    comments = "0"
                )
                
                userRef.child(threadId).setValue(threadData).await()
                
                withContext(Dispatchers.Main) {
                    loading.value = false
                    _isPosted.value = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.value = false
                }
            }
        }
    }
}