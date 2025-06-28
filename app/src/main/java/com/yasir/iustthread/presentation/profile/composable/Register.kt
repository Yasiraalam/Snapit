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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.yasir.iustthread.navigation.Routes
import com.yasir.iustthread.presentation.login.AuthViewModel
import com.yasir.iustthread.R
import com.yasir.iustthread.ui.theme.PinkColor

import androidx.compose.foundation.verticalScroll

@Composable
fun Register(navHostController: NavHostController) {
    // --- State Management (from your original code) ---
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") } // Changed to var and made optional
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var imageUri by remember { mutableStateOf<Uri?>(null) }

    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

    // --- Visibility and Error States ---
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var usernameError by remember { mutableStateOf<String?>(null) }

    // --- Logic (from your original code) ---
    val emailRegex = Regex("^([a-zA-Z0-9._%+-]+)?@+(?:gmail|hotmail|outlook)\\.com\$")
    val passwordRegex = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#\$%^&*]).{8,}\$")
    val authViewModel: AuthViewModel = remember { AuthViewModel() } // Use remember for ViewModel
    val firebaseUser by authViewModel.firebaseUser.observeAsState(null)
    val loading by authViewModel.loading.observeAsState(false)
    val error by authViewModel.error.observeAsState(null)
    val context = LocalContext.current

    // --- Image Picker Logic (from your original code) ---
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

    // --- Navigation Logic (from your original code) ---
    LaunchedEffect(firebaseUser) {
        if (firebaseUser != null) {
            navHostController.navigate(Routes.BottomNav.routes) {
                popUpTo(navHostController.graph.startDestinationId)
                launchSingleTop = true
            }
        }
    }

    // Handle ViewModel errors
    LaunchedEffect(error) {
        if (error != null) {
            dialogMessage = error!!
            showDialog = true
        }
    }

    // Function to handle registration click
    fun handleRegister() {
        // Reset errors
        usernameError = if (username.isBlank()) "Username cannot be empty" else null
        emailError = if (!email.matches(emailRegex)) "Invalid email format" else null
        passwordError = if (!password.matches(passwordRegex)) "Password format is too weak" else null
        confirmPasswordError = if (password != confirmPassword) "Passwords do not match" else null

        val hasError = listOf(usernameError, emailError, passwordError, confirmPasswordError).any { it != null }

        if (fullName.isBlank()) {
            dialogMessage = "Please enter your full name."
            showDialog = true
            return
        }

        if (hasError) {
            dialogMessage = "Please fix the errors before proceeding."
            showDialog = true
            return
        }

        authViewModel.register(email, password, fullName, bio, username, imageUri, context)
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header
            Text(
                text = "Create Account", 
                fontSize = 24.sp, 
                fontWeight = FontWeight.Bold, 
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Join our community and start sharing", 
                fontSize = 14.sp, 
                color = Color.Gray, 
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Profile Photo Picker
            ProfileImagePicker(imageUri = imageUri) {
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
            Spacer(modifier = Modifier.height(16.dp))

            // Input Fields with improved styling
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name", fontSize = 14.sp) },
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                placeholder = { Text("Enter your full name", fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PinkColor,
                    unfocusedBorderColor = Color.Black.copy(alpha = 0.5f),
                    focusedLabelColor = PinkColor
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it; usernameError = null },
                label = { Text("Username", fontSize = 14.sp) },
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                placeholder = { Text("Choose a username", fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = usernameError != null,
                supportingText = { 
                    if (usernameError != null) Text(
                        text = usernameError!!, 
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    ) 
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PinkColor,
                    unfocusedBorderColor = Color.Black.copy(alpha = 0.5f),
                    focusedLabelColor = PinkColor,
                    errorBorderColor = MaterialTheme.colorScheme.error
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = null },
                label = { Text("Email", fontSize = 14.sp) },
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                placeholder = { Text("Enter your email", fontSize = 14.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = emailError != null,
                supportingText = { 
                    if (emailError != null) Text(
                        text = emailError!!, 
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    ) 
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PinkColor,
                    unfocusedBorderColor = Color.Black.copy(alpha = 0.5f),
                    focusedLabelColor = PinkColor,
                    errorBorderColor = MaterialTheme.colorScheme.error
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

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
                    unfocusedBorderColor = Color.Black.copy(alpha = 0.5f),
                    focusedLabelColor = PinkColor
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; passwordError = null },
                label = { Text("Password", fontSize = 14.sp) },
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                placeholder = { Text("Create a password", fontSize = 14.sp) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible) painterResource(R.drawable.visible_icon) else painterResource(R.drawable.visibility_off_icon)
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = image, 
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = passwordError != null,
                supportingText = { 
                    if (passwordError != null) Text(
                        text = passwordError!!, 
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    ) 
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PinkColor,
                    unfocusedBorderColor = Color.Black.copy(alpha = 0.5f),
                    focusedLabelColor = PinkColor,
                    errorBorderColor = MaterialTheme.colorScheme.error
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; confirmPasswordError = null },
                label = { Text("Confirm Password", fontSize = 14.sp) },
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                placeholder = { Text("Confirm your password", fontSize = 14.sp) },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (confirmPasswordVisible) painterResource(R.drawable.visible_icon) else painterResource(R.drawable.visibility_off_icon)
                    IconButton(
                        onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = image, 
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = confirmPasswordError != null,
                supportingText = { 
                    if (confirmPasswordError != null) Text(
                        text = confirmPasswordError!!, 
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    ) 
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PinkColor,
                    unfocusedBorderColor = Color.Black.copy(alpha = 0.5f),
                    focusedLabelColor = PinkColor,
                    errorBorderColor = MaterialTheme.colorScheme.error
                )
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Register Button
            Button(
                onClick = { handleRegister() },
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
                        text = "Create Account", 
                        color = Color.White, 
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Sign-in link
            SignInLink(primaryColor = PinkColor) {
                navHostController.navigate(Routes.Login.routes) {
                    popUpTo(navHostController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Alert Dialog for errors/validation
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Registration Incomplete", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                text = { Text(dialogMessage, fontSize = 14.sp) },
                confirmButton = {
                    Button(
                        onClick = { showDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = PinkColor)
                    ) { 
                        Text("OK", color = Color.White) 
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileImagePicker(imageUri: Uri?, onImageClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(Color(0xFFF0F0F0))
            .border(1.dp, Color.LightGray, CircleShape)
            .clickable(onClick = onImageClick),
        contentAlignment = Alignment.Center
    ) {
        if (imageUri == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Photo",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
                Text("Photo", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            Image(
                painter = rememberAsyncImagePainter(model = imageUri),
                contentDescription = "Selected profile picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun SignInLink(primaryColor: Color, onSignInClick: () -> Unit) {
    Row {
        Text("Already have an account? ", color = Color.Gray)
        Text(
            text = "Sign in",
            color = primaryColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onSignInClick)
        )
    }
}