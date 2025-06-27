package com.yasir.iustthread.presentation.home.composable

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.yasir.iustthread.R
import com.yasir.iustthread.navigation.Routes
import com.yasir.iustthread.ui.theme.PinkColor
import kotlinx.coroutines.delay

@Composable
fun Splash(navController: NavHostController) {
    
    // Animation states
    var logoVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }
    var developerVisible by remember { mutableStateOf(false) }
    
    // Animated values
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(1000, easing = EaseOutBack),
        label = "logo_scale"
    )
    
    val logoRotation by animateFloatAsState(
        targetValue = if (logoVisible) 360f else 0f,
        animationSpec = tween(1500, easing = EaseInOutCubic),
        label = "logo_rotation"
    )
    
    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(800),
        label = "logo_alpha"
    )
    
    val textAlpha by animateFloatAsState(
        targetValue = if (textVisible) 1f else 0f,
        animationSpec = tween(1000),
        label = "text_alpha"
    )
    
    val developerAlpha by animateFloatAsState(
        targetValue = if (developerVisible) 0.6f else 0f,
        animationSpec = tween(800),
        label = "developer_alpha"
    )
    
    // Infinite pulse animation for logo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Gradient background
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A1A2E),
            Color(0xFF16213E),
            Color(0xFF0F3460)
        )
    )
    
    // Start animations
    LaunchedEffect(true) {
        delay(300)
        logoVisible = true
        delay(800)
        textVisible = true
        delay(500)
        developerVisible = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        
        // Main content - centered
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // Logo with animations
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(logoScale * pulseScale)
                    .rotate(logoRotation)
                    .alpha(logoAlpha)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                PinkColor.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.threads_logo),
                    contentDescription = "Snappit Logo",
                    modifier = Modifier.size(80.dp),
                    tint = PinkColor
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App name with animation
            Text(
                text = "Snappit",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.alpha(textAlpha)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tagline
            Text(
                text = "Share Your Moments",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.alpha(textAlpha)
            )
        }
        
        // Developer credit - bottom right corner (subtle)
        Text(
            text = "by Yasir Alam",
            fontSize = 12.sp,
            fontWeight = FontWeight.Light,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .alpha(developerAlpha)
        )
        
        // Floating particles effect
        repeat(6) { index ->
            FloatingParticle(
                delay = index * 200L,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    
    // Navigation logic
    LaunchedEffect(true) {
        delay(3500) // Increased delay to show animations
        if (FirebaseAuth.getInstance().currentUser != null) {
            navController.navigate(Routes.BottomNav.routes) {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
        } else {
            navController.navigate(Routes.Login.routes) {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
        }
    }
}

@Composable
fun FloatingParticle(delay: Long, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "particle")
    
    val yPosition by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000 + (delay / 10).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "y_position"
    )
    
    val xPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 4000 + (delay / 10).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "x_position"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    LaunchedEffect(true) {
        delay(delay)
    }
    
    Box(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .offset(
                    x = (xPosition * 400).dp,
                    y = (yPosition * 800).dp
                )
                .size(4.dp)
                .clip(CircleShape)
                .background(PinkColor.copy(alpha = alpha))
        )
    }
}