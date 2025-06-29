package com.yasir.iustthread.presentation.home.composable

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.yasir.iustthread.R
import com.yasir.iustthread.navigation.Routes
import com.yasir.iustthread.presentation.login.AuthViewModel
import com.yasir.iustthread.presentation.home.HomeViewModel
import com.yasir.iustthread.presentation.profile.UserViewModel
import com.yasir.iustthread.ui.theme.PinkColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUsers(
    navHostController: NavHostController,
    uid: String
) {
    val authViewModel: AuthViewModel = viewModel()
    val firebaseUser by authViewModel.firebaseUser.observeAsState(null)

    val userViewModel: UserViewModel = viewModel()
    val threads by userViewModel.threads.observeAsState(emptyList())
    val user by userViewModel.user.observeAsState(null)
    val followersList by userViewModel.followersList.observeAsState(emptyList())
    val followingList by userViewModel.followingList.observeAsState(emptyList())
    val isLoading by userViewModel.isLoading.observeAsState(false)

    var currentUserId = ""
    if (FirebaseAuth.getInstance().currentUser != null) {
        currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
    }

    var isInitialLoading by remember { mutableStateOf(true) }

    // --- Local state for follow button and counts ---
    var isFollowing by remember(followersList, currentUserId) {
        mutableStateOf(followersList.any { it.uid == currentUserId })
    }
    var followerCount by remember(followersList) { mutableStateOf(followersList.size) }
    var followingCount by remember(followingList) { mutableStateOf(followingList.size) }
    var isFollowLoading by remember { mutableStateOf(false) }
    var shouldRefreshFollowers by remember { mutableStateOf(false) }

    // Fetch data when uid is available
    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            try {
                isInitialLoading = true
                userViewModel.refreshUserData(uid)
                kotlinx.coroutines.delay(500)
                isInitialLoading = false
            } catch (e: Exception) {
                isInitialLoading = false
            }
        } else {
            isInitialLoading = false
        }
    }

    LaunchedEffect(followersList, followingList) {
        isFollowing = followersList.any { it.uid == currentUserId }
        followerCount = followersList.size
        followingCount = followingList.size
    }

    // Fix: Use a flag and a top-level LaunchedEffect for refreshing followers/following
    LaunchedEffect(shouldRefreshFollowers) {
        if (shouldRefreshFollowers) {
            kotlinx.coroutines.delay(600)
            userViewModel.refreshUserData(uid)
            isFollowLoading = false
            shouldRefreshFollowers = false
        }
    }

    LaunchedEffect(firebaseUser) {
        if (firebaseUser == null) {
            navHostController.navigate(Routes.Login.routes) {
                popUpTo(navHostController.graph.startDestinationId)
                launchSingleTop = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = user?.username ?: "User",
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

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isInitialLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFE91E63)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Profile Header Section
                    item {
                        if (user != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Profile Image with initials
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .background(
                                                Color(0xFF4A90E2),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (user!!.imageUri.isNotEmpty()) {
                                            AsyncImage(
                                                model = user!!.imageUri,
                                                contentDescription = "Profile image",
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = user!!.name.take(2).uppercase().ifEmpty { "U" },
                                                color = Color.White,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Stats Section
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                                    ) {
                                        StatItem(
                                            count = threads.size.toString(),
                                            label = "posts"
                                        )
                                        StatItem(
                                            count = if (followerCount >= 1000) "${(followerCount / 1000f).format(1)}k" else followerCount.toString(),
                                            label = "followers"
                                        )
                                        StatItem(
                                            count = followingCount.toString(),
                                            label = "following"
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // User Info
                                Text(
                                    text = user!!.name.ifEmpty { "User" },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )

                                if (user!!.bio.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = user!!.bio,
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        lineHeight = 18.sp
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "No bio available",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Follow Button
                                if (currentUserId.isNotEmpty() && currentUserId != uid) {
                                    Button(
                                        onClick = {
                                            if (!isFollowLoading) {
                                                isFollowLoading = true
                                                if (isFollowing) {
                                                    userViewModel.unfollowUser(uid, currentUserId)
                                                } else {
                                                    userViewModel.followUser(uid, currentUserId)
                                                }
                                                shouldRefreshFollowers = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFollowing) Color.Gray else Color(0xFFE91E63)
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !isFollowLoading
                                    ) {
                                        if (isFollowLoading) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                text = if (isFollowing) "Unfollow" else "Follow",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Posts Section Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Grid",
                                        tint = PinkColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "POSTS",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = PinkColor,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        } else {
                            // Loading state for user data
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFFE91E63)
                                )
                            }
                        }
                    }

                    // Posts Grid
                    item {
                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFFE91E63),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        } else if (threads.isNotEmpty()) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(500.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                userScrollEnabled = false
                            ) {
                                items(threads) { thread ->
                                    PostGridItem(
                                        thread = thread,
                                        onClick = {
                                            navHostController.navigate("comments/${thread.threadId}")
                                        }
                                    )
                                }
                            }
                        } else {
                            // Empty state
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Create,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No posts yet",
                                        fontSize = 16.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "This user hasn't shared any posts yet.",
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(
    count: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun PostGridItem(
    thread: com.yasir.iustthread.domain.model.ThreadModel,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() }
            .background(Color(0xFFF5F5F5))
    ) {
        if (thread.image.isNotEmpty()) {
            AsyncImage(
                model = thread.image,
                contentDescription = "Post image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Show placeholder for posts without images
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No image of this post",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Like count overlay (bottom right) - only show if there are likes
        if (thread.likes > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = thread.likes.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Comment count overlay (bottom left) - only show if there are comments
        if (thread.comments.isNotEmpty() && thread.comments != "0") {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.chat),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = thread.comments,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Extension function to format numbers
fun Float.format(digits: Int) = "%.${digits}f".format(this)