package com.yasir.iustthread.presentation.profile.composable

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.yasir.iustthread.navigation.Routes
import com.yasir.iustthread.presentation.login.AuthViewModel
import com.yasir.iustthread.presentation.profile.UserViewModel
import com.yasir.iustthread.ui.theme.PinkColor
import com.yasir.iustthread.utils.SharedPref
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfile(navHostController: NavHostController) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = remember { AuthViewModel() }
    val userViewModel: UserViewModel = remember { UserViewModel() }
    
    // Get current user data from SharedPref
    val currentName = SharedPref.getName(context)
    val currentBio = SharedPref.getBio(context)
    val currentImageUri = SharedPref.getImageUrl(context)
    
    // State management
    var name by remember { mutableStateOf(currentName) }
    var bio by remember { mutableStateOf(currentBio) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Error states
    var nameError by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    
    // Loading state
    val loading by userViewModel.isLoading.observeAsState(false)
    
    // Error message state
    val errorMessage by userViewModel.errorMessage.observeAsState(null)
    
    // Success state
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    // Handle error messages
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            dialogMessage = errorMessage ?: "An error occurred while updating profile"
            showDialog = true
            userViewModel.clearErrorMessage()
        }
    }
    
    // Navigate back after successful update
    LaunchedEffect(loading) {
        if (!loading && userViewModel.user.value != null && errorMessage == null) {
            // Profile update completed successfully
            delay(500) // Small delay to ensure UI is updated
            showSuccessDialog = true
        }
    }
    
    // Handle success dialog navigation
    LaunchedEffect(showSuccessDialog) {
        if (showSuccessDialog) {
            delay(2000) // Show success message for 2 seconds
            showSuccessDialog = false
            // Navigate to the Home screen within the BottomNav
            navHostController.navigate(Routes.Home.routes) {
                // Pop up to the start destination (Home) and make it the only destination
                popUpTo(navHostController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
            }
        }
    }
    
    // Image picker logic
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            launcher.launch("image/*")
        }
    }
    
    // Function to handle save
    fun handleSave() {
        // Reset errors
        nameError = if (name.isBlank()) "Name cannot be empty" else null
        
        if (nameError != null) {
            dialogMessage = "Please enter your name."
            showDialog = true
            return
        }
        
        // Use current values if fields are empty
        val finalName = if (name.isBlank()) currentName else name
        val finalBio = if (bio.isBlank()) currentBio else bio
        
        // Update profile
        userViewModel.updateProfile(finalName, finalBio, imageUri, context)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        // Navigate to the Home screen within the BottomNav
                        navHostController.navigate(Routes.Home.routes) {
                            // Pop up to the start destination (Home) and make it the only destination
                            popUpTo(navHostController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Profile Image Picker
            ProfileImagePicker(
                currentImageUri = currentImageUri,
                newImageUri = imageUri
            ) {
                val isGranted = ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED
                if (isGranted) {
                    launcher.launch("image/*")
                } else {
                    permissionLauncher.launch(permissionToRequest)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Profile Picture (Optional)", 
                fontSize = 12.sp, 
                color = Color.Gray, 
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = null },
                label = { Text("Name", fontSize = 14.sp) },
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                placeholder = { Text("Enter your name", fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = nameError != null,
                supportingText = { 
                    if (nameError != null) Text(
                        text = nameError!!, 
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    ) 
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PinkColor,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                    focusedLabelColor = PinkColor,
                    errorBorderColor = MaterialTheme.colorScheme.error
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Bio Field
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio (Optional)", fontSize = 14.sp) },
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                placeholder = { Text("Tell us about yourself...", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp, max = 120.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PinkColor,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                    focusedLabelColor = PinkColor
                )
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            // Save Button
            Button(
                onClick = { handleSave() },
                colors = ButtonDefaults.buttonColors(containerColor = PinkColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = !loading
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "Save Changes", 
                        color = Color.White, 
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Alert Dialog for errors
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White,
                title = { 
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Update Failed", 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = { 
                    Text(
                        dialogMessage, 
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    ) 
                },
                confirmButton = {
                    Button(
                        onClick = { showDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) { 
                        Text(
                            "OK", 
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ) 
                    }
                }
            )
        }
        
        // Success Dialog
        if (showSuccessDialog && !loading) {
            AlertDialog(
                onDismissRequest = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        color = PinkColor,
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White,
                title = { 
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = PinkColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Success!", 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = { 
                    Text(
                        "Profile updated successfully", 
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    ) 
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            showSuccessDialog = false
                            navHostController.navigateUp()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PinkColor),
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) { 
                        Text(
                            "Continue", 
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ) 
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileImagePicker(
    currentImageUri: String,
    newImageUri: Uri?,
    onImageClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(Color(0xFFF0F0F0))
            .border(1.dp, Color.LightGray, CircleShape)
            .clickable(onClick = onImageClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            newImageUri != null -> {
                // Show newly selected image
                Image(
                    painter = rememberAsyncImagePainter(model = newImageUri),
                    contentDescription = "Selected profile picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            currentImageUri.isNotEmpty() -> {
                // Show current profile image
                Image(
                    painter = rememberAsyncImagePainter(model = currentImageUri),
                    contentDescription = "Current profile picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                // Show add icon
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Photo",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Photo", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
} 