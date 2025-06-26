@file:OptIn(ExperimentalMaterial3Api::class)

package com.yasir.iustthread.presentation.addpost.composable

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.yasir.iustthread.navigation.Routes
import com.yasir.iustthread.utils.SharedPref
import com.yasir.iustthread.presentation.addpost.AddThreadViewModel
import com.yasir.iustthread.R

@Composable
fun AddThreads(navHostController: NavHostController) {
    val threadViewModel: AddThreadViewModel = viewModel()
    val isPosted by threadViewModel.isPosted.observeAsState(false)
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var thread by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val loading = remember { mutableStateOf(false) }

    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                launcher.launch("image/*")
            }
        }

    LaunchedEffect(isPosted) {
        if (isPosted) {
            thread = ""
            imageUri = null
            Toast.makeText(context, "Thread Posted!", Toast.LENGTH_SHORT).show()
            navHostController.navigate(Routes.Home.routes) {
                popUpTo(Routes.AddThread.routes) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.threads_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                Color(0xFFE91E63),
                                CircleShape
                            )
                            .padding(6.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Thread",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1F2937)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = { navHostController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF1F2937)
                    )
                }
            },
            actions = {
                if (loading.value) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 16.dp),
                        color = Color(0xFFE91E63),
                        strokeWidth = 2.dp
                    )
                } else {
                    Button(
                        onClick = {
                            if (imageUri == null && thread.isNotEmpty()) {
                                loading.value = true
                                threadViewModel.saveData(
                                    thread,
                                    FirebaseAuth.getInstance().currentUser!!.uid,
                                    "",
                                    loading
                                )
                            } else if (imageUri != null) {
                                loading.value = true
                                threadViewModel.saveImage(
                                    thread,
                                    FirebaseAuth.getInstance().currentUser!!.uid,
                                    imageUri!!,
                                    loading,
                                    context
                                )
                            } else {
                                showDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE91E63)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Post",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // User Profile Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = SharedPref.getImageUrl(context)),
                        contentDescription = "User Profile",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = SharedPref.getUserName(context),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            text = "Create a new thread",
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Thread Content Section
            Text(
                text = "Thread Content *",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = thread,
                onValueChange = { thread = it },
                placeholder = {
                    Text(
                        text = "Start a thread...",
                        color = Color(0xFF9CA3AF)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE91E63),
                    unfocusedBorderColor = Color(0xFFD1D5DB),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Photo Section
            Text(
                text = "Photo",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (imageUri == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .border(
                            2.dp,
                            Color(0xFFD1D5DB),
                            RoundedCornerShape(8.dp)
                        )
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .clickable {
                            val isGranted = ContextCompat.checkSelfPermission(
                                context,
                                permissionToRequest
                            ) == PackageManager.PERMISSION_GRANTED

                            if (isGranted) {
                                launcher.launch("image/*")
                            } else {
                                permissionLauncher.launch(permissionToRequest)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.image),
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add a photo",
                            color = Color(0xFF6B7280),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box {
                        Image(
                            painter = rememberAsyncImagePainter(model = imageUri),
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { imageUri = null },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove Image",
                                tint = Color.White,
                                modifier = Modifier
                                    .background(
                                        Color.Black.copy(alpha = 0.5f),
                                        CircleShape
                                    )
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(
                    icon = painterResource(R.drawable.image),
                    text = "Photo",
                    onClick = {
                        val isGranted = ContextCompat.checkSelfPermission(
                            context,
                            permissionToRequest
                        ) == PackageManager.PERMISSION_GRANTED

                        if (isGranted) {
                            launcher.launch("image/*")
                        } else {
                            permissionLauncher.launch(permissionToRequest)
                        }
                    }
                )
                ActionButton(
                    icon = painterResource(R.drawable.threads_logo),
                    text = "Anyone can reply",
                    onClick = { /* Handle reply settings */ }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tips Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3F2)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tips for great threads",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1F2937)
                        )
                    }

                    val tips = listOf(
                        "Write engaging and clear content",
                        "Add relevant images to your thread",
                        "Keep your message concise and meaningful",
                        "Engage with your community respectfully"
                    )

                    tips.forEach { tip ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                color = Color(0xFF6B7280),
                                fontSize = 14.sp
                            )
                            Text(
                                text = tip,
                                color = Color(0xFF6B7280),
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Alert Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Thread") },
            text = { Text("Write something in thread or upload picture") },
            confirmButton = {
                Button(
                    onClick = { showDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE91E63)
                    )
                ) {
                    Text("OK")
                }
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun ActionButton(
    icon: Painter,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = Color(0xFF6B7280),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}