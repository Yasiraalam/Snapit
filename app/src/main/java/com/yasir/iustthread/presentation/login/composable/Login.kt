package com.yasir.iustthread.presentation.login.composable

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.yasir.iustthread.navigation.Routes
import com.yasir.iustthread.presentation.login.AuthViewModel
import com.yasir.iustthread.ui.theme.LightBorderGray
import com.yasir.iustthread.ui.theme.PinkColor
import com.yasir.iustthread.ui.theme.TextGray
import com.yasir.iustthread.R


@Composable
fun Login(navController: NavHostController) {
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val authViewModel: AuthViewModel = AuthViewModel()
    val firebaseUser by authViewModel.firebaseUser.observeAsState(null)
    val error by authViewModel.error.observeAsState(null)
    val loading by authViewModel.loading.observeAsState(false)

    // Regex pattern for email validation
    val emailRegex = Regex("^([a-zA-Z0-9._%+-]+)?@+(?:gmail|hotmail|outlook)\\.com\$")

    LaunchedEffect(firebaseUser) {
        if (firebaseUser != null) {
            navController.navigate(Routes.BottomNav.routes) {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
        }
    }

    // Show toast for any errors
    error?.let {
        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        // Reset loading state on error
        LaunchedEffect(it) {
            showDialog = false
        }
    }

    // --- New UI Structure ---
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            AppLogo()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sign in to your IUST-Thread account",
                style = MaterialTheme.typography.bodyLarge,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(32.dp))

            // The main form card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, LightBorderGray)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    Text(text = "Email address", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            // Your validation logic
                            emailError = !it.matches(emailRegex)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter your email", color = TextGray) },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = "Email Icon") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = emailError // Your error state
                    )
                    // Your error message display
                    if (emailError && email.isNotEmpty()) {
                        Text(
                            text = "Invalid email format (e.g., user@gmail.com)",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Password Field ---
                    Text(text = "Password", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter your password", color = TextGray) },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = "Password Icon") },
                        trailingIcon = {
                            val image = if (passwordVisible) painterResource(R.drawable.visible_icon)else painterResource(R.drawable.visibility_off_icon)
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(painter = image, contentDescription = "Toggle visibility")
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { rememberMe = !rememberMe }
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = PinkColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Remember me", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                        }
                        Text(
                            text = "Forgot password?",
                            color = PinkColor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { /* TODO: Add forgot password logic */ }
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // --- Sign In Button with Loading State ---
                    Box(
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (loading) {
                            CircularProgressIndicator(color = PinkColor)
                        } else {
                            Button(
                                onClick = {
                                    if (email.isEmpty() || password.isEmpty()) {
                                        showDialog = true
                                    } else {
                                        authViewModel.login(email, password, context)
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                colors = ButtonDefaults.buttonColors(containerColor = PinkColor),
                                shape = RoundedCornerShape(12.dp),
                                enabled = email.isNotEmpty() && password.isNotEmpty() && !emailError
                            ) {
                                Text("Sign In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- "Or continue with" divider ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Divider(modifier = Modifier.weight(1f), color = LightBorderGray.copy(alpha = 0.5f))
                        Text(
                            text = "Or continue with",
                            color = TextGray,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Divider(modifier = Modifier.weight(1f), color = LightBorderGray.copy(alpha = 0.5f))
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // --- Social Login Buttons ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SocialLoginButton(
                            text = "Google",
                            icon = { Icon(painterResource(R.drawable.google_icon), contentDescription = "Google Logo") },
                            onClick = { /* TODO: Add Google sign-in logic */ },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))

                    // --- Sign up text with your navigation logic ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Don't have an account? ", color = TextGray, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Sign up",
                            color = PinkColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable {
                                navController.navigate(Routes.Register.routes) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Login Error") },
            text = { Text("Please fill in both email and password.") },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}


@Composable
fun AppLogo() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(72.dp)
            .background(PinkColor, shape = RoundedCornerShape(20.dp))
    ) {
        Icon(
            painter = painterResource(R.drawable.threads_logo),
            contentDescription = "App Logo",
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
fun SocialLoginButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp).width(60.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LightBorderGray.copy(alpha = 0.5f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                icon()
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@Composable
fun GoogleIconPlaceholder() {
    // In a real application, use the official Google logo from your drawables
     //Image(painter = painterResource(id = R.drawable.ic_google_logo))
    Text("G", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF4285F4))
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    MaterialTheme {
        Login(navController = rememberNavController())
    }
}