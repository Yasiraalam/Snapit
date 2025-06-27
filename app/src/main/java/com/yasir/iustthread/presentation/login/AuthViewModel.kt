package com.yasir.iustthread.presentation.login

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.yasir.iustthread.domain.model.UserModel
import com.yasir.iustthread.utils.SharedPref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class AuthViewModel : ViewModel() {
    val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    val userRef = database.getReference("Users")
    private val _firebaseUser = MutableLiveData<FirebaseUser?>()
    val firebaseUser: MutableLiveData<FirebaseUser?> = _firebaseUser
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    private val storageRef = FirebaseStorage.getInstance().reference

    init {
        _firebaseUser.value = auth.currentUser
    }

    fun login(email: String, password: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    _loading.value = true
                }
                
                val result = auth.signInWithEmailAndPassword(email, password).await()
                withContext(Dispatchers.Main) {
                    _firebaseUser.value = result.user
                    _loading.value = false
                }
                getData(result.user!!.uid, context)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _loading.value = false
                    val errorMessage = when (e) {
                        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Invalid email or password"
                        is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "User not found"
                        is com.google.firebase.auth.FirebaseAuthEmailException -> "Invalid email format"
                        else -> e.message ?: "Login failed"
                    }
                    _error.value = errorMessage
                }
            }
        }
    }

    private suspend fun getData(uid: String, context: Context) {
        try {
            val snapshot = userRef.child(uid).get().await()
            val userData = snapshot.getValue(UserModel::class.java)
            if (userData != null) {
                SharedPref.storeData(
                    userData.name,
                    userData.email,
                    userData.bio,
                    userData.username,
                    userData.imageUri,
                    uid,
                    context
                )
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Something went wrong! Login again or check your connection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun register(
        email: String,
        password: String,
        name: String,
        bio: String,
        username: String,
        imageUri: Uri,
        context: Context
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    _loading.value = true
                }
                
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                withContext(Dispatchers.Main) {
                    _loading.value = false
                    _firebaseUser.value = result.user
                }
                saveImage(email, password, name, bio, username, imageUri, result.user?.uid, context)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _loading.value = false
                    val errorMessage = when (e) {
                        is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> "Password should be at least 6 characters"
                        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Invalid email format"
                        is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "Email already exists"
                        else -> e.message ?: "Registration failed"
                    }
                    _error.value = errorMessage
                }
            }
        }
    }

    private suspend fun saveImage(
        email: String,
        password: String,
        name: String,
        bio: String,
        username: String,
        imageUri: Uri,
        uid: String?,
        context: Context
    ) {
        if (uid == null) {
            withContext(Dispatchers.Main) {
                _error.value = "User ID is null"
            }
            return
        }
        
        try {
            val imageRef = storageRef.child("Users/${UUID.randomUUID()}.jpg")
            val uploadTask = imageRef.putFile(imageUri).await()
            val downloadUri = imageRef.downloadUrl.await()
            saveData(email, password, name, bio, username, downloadUri.toString(), uid, context)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _error.value = "Failed to upload image: ${e.message}"
            }
        }
    }

    private suspend fun saveData(
        email: String,
        password: String,
        name: String,
        bio: String,
        username: String,
        imageUri: String,
        uid: String?,
        context: Context
    ) {
        if (uid == null) {
            withContext(Dispatchers.Main) {
                _error.value = "User ID is null"
            }
            return
        }

        try {
            val firestoreDb = Firebase.firestore
            val followerRef = firestoreDb.collection("followers").document(uid)
            val followingRef = firestoreDb.collection("following").document(uid)

            // Create follower and following documents
            followerRef.set(mapOf("followerId" to listOf<String>())).await()
            followingRef.set(mapOf("followingId" to listOf<String>())).await()

            val userData = UserModel(email, password, name, bio, username, imageUri, uid)
            userRef.child(uid).setValue(userData).await()
            
            SharedPref.storeData(
                name,
                email,
                bio,
                username,
                imageUri,
                uid,
                context
            )
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _error.value = "Failed to save user data: ${e.message}"
            }
        }
    }
    
    fun logout() {
        auth.signOut()
        _firebaseUser.postValue(null)
    }
}