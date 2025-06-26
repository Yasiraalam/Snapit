package com.yasir.iustthread.presentation.addpost

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.yasir.iustthread.domain.model.ThreadModel
import java.util.UUID

class AddThreadViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance()
    val userRef = database.getReference("threads")

    private val _isPosted = MutableLiveData<Boolean>()
    val isPosted: LiveData<Boolean> = _isPosted

    private val storageref = Firebase.storage.reference
    private val imageRef = storageref.child("threads/${UUID.randomUUID()}.jpg")


    fun saveImage(
        thread: String,
        userId: String,
        imageUri: Uri,
        loading: MutableState<Boolean>,
        context: Context
    ) {
        Log.d("AddThreadViewModel", "Starting image upload: thread=$thread, userId=$userId")
        loading.value = true

        // Create a **new** imageRef every time
        val imageRef = storageref.child("threads/${UUID.randomUUID()}.jpg")
        Log.d("AddThreadViewModel", "Image reference created: ${imageRef.path}")

        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            if (inputStream != null) {
                Log.d("AddThreadViewModel", "InputStream opened successfully, starting upload...")
                
                // Add timeout for upload
                val uploadTask = imageRef.putStream(inputStream)
                
                // Set a timeout - if upload takes more than 30 seconds, fall back to text-only
                val timeoutHandler = Handler(Looper.getMainLooper())
                val timeoutRunnable = Runnable {
                    Log.w("AddThreadViewModel", "Image upload timeout, falling back to text-only")
                    uploadTask.cancel()
                    saveData(thread, userId, "", loading)
                }
                timeoutHandler.postDelayed(timeoutRunnable, 30000) // 30 seconds timeout
                
                uploadTask
                    .addOnSuccessListener { taskSnapshot ->
                        timeoutHandler.removeCallbacks(timeoutRunnable)
                        Log.d("AddThreadViewModel", "Image upload successful, getting download URL...")
                        imageRef.downloadUrl
                            .addOnSuccessListener { uri ->
                                Log.d("AddThreadViewModel", "Download URL obtained: $uri")
                                saveData(thread, userId, uri.toString(), loading)
                            }
                            .addOnFailureListener { exception ->
                                Log.e("AddThreadViewModel", "Failed to get download URL: ${exception.message}")
                                // Fallback to text-only post if image URL fails
                                Log.d("AddThreadViewModel", "Falling back to text-only post")
                                saveData(thread, userId, "", loading)
                            }
                    }
                    .addOnFailureListener { exception ->
                        timeoutHandler.removeCallbacks(timeoutRunnable)
                        Log.e("AddThreadViewModel", "Image upload failed: ${exception.message}")
                        // Fallback to text-only post if upload fails
                        Log.d("AddThreadViewModel", "Falling back to text-only post due to upload failure")
                        saveData(thread, userId, "", loading)
                    }
                    .addOnProgressListener { taskSnapshot ->
                        val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                        Log.d("AddThreadViewModel", "Upload progress: ${progress.toInt()}%")
                    }
            } else {
                loading.value = false
                Log.e("AddThreadViewModel", "InputStream is null — invalid or inaccessible Uri")
            }
        } catch (e: Exception) {
            loading.value = false
            Log.e("AddThreadViewModel", "Exception during image upload: ${e.message}")
        }
    }

    // Simple function to save text-only posts (for testing)
    fun saveTextOnly(
        thread: String,
        userId: String,
        loading: MutableState<Boolean>
    ) {
        Log.d("AddThreadViewModel", "Saving text-only post: thread=$thread, userId=$userId")
        saveData(thread, userId, "", loading)
    }

    fun saveData(
        thread: String,
        userId: String,
        image: String,
        loading: MutableState<Boolean>
    ) {
        Log.d("AddThreadViewModel", "Saving thread data: thread=$thread, userId=$userId, image=$image")
        
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
        
        Log.d("AddThreadViewModel", "Thread data created: $threadData")
        
        userRef.child(threadId).setValue(threadData)
            .addOnSuccessListener {
                loading.value = false
                _isPosted.postValue(true)
                Log.d("AddThreadViewModel", "Thread saved successfully with ID: $threadId")
            }
            .addOnFailureListener { exception ->
                loading.value = false
                Log.e("AddThreadViewModel", "Failed to save thread: ${exception.message}")
                Log.e("AddThreadViewModel", "Exception details: ${exception.cause}")
            }
    }

}