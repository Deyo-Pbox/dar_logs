package com.example.darlogs.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darlogs.R
import com.example.darlogs.ui.theme.*
import java.util.*

import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient

@Composable
fun LoginScreen(
    lastUsername: String,
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: (String, String) -> Unit,
    onBiometricLogin: () -> Unit,
    isBiometricAvailable: Boolean
) {
    // Hidden WebView to bypass InfinityFree AES
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = WebViewClient()
                // Use the same User-Agent as ApiClient
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                loadUrl("https://darlogs.freedev.app/")
            }
        },
        modifier = Modifier.size(0.dp) // Invisible
    )

    var username by remember { mutableStateOf(lastUsername) }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        Image(
            painter = painterResource(id = R.drawable.login_bg_sunset),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .animateContentSize(),
                shape = RoundedCornerShape(40.dp),
                color = Color.White.copy(alpha = 0.82f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier.size(90.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.dar_logo_white_round),
                            contentDescription = "DAR Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        color = Color(0xFF144326),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "SECURE ACCESS",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            letterSpacing = 0.8.sp
                        )
                    }

                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { visible = true }
                    
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(animationSpec = tween(1000)) { -20 }
                    ) {
                        AnimatedContent(
                            targetState = greeting,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(700)) + slideInVertically(animationSpec = tween(700)) { height -> -height / 2 })
                                    .togetherWith(fadeOut(animationSpec = tween(700)) + slideOutVertically(animationSpec = tween(700)) { height -> height / 2 })
                            },
                            label = "GreetingTransition"
                        ) { targetGreeting ->
                            Text(
                                text = "Hi, $targetGreeting",
                                color = Color(0xFF144326).copy(alpha = 0.8f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Welcome Back",
                        color = Color(0xFF0A1F44),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Text(
                        text = "Sign in to your account.",
                        color = Color(0xFF5E6C84),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 28.dp)
                    )

                    if (lastUsername.isNotEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.5.dp, Color(0xFF144326), CircleShape)
                        ) {
                            Text(
                                text = lastUsername.take(1).uppercase(),
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF144326)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = lastUsername,
                                color = Color(0xFF0A1F44),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(3.5.dp)
                                    .background(Color(0xFFFFC107))
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(28.dp))
                    } else {
                        TextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF144326).copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                            label = { 
                                Text(
                                    text = "Username", 
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ) 
                            },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedLabelColor = Color(0xFF144326),
                                unfocusedLabelColor = Color(0xFFA5B1C2),
                                cursorColor = Color(0xFF144326)
                            ),
                            textStyle = TextStyle(color = Color(0xFF0A1F44), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF144326).copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                        label = { 
                            Text(
                                text = "Password", 
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ) 
                        },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color(0xFF144326)
                                )
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = Color(0xFF144326),
                            unfocusedLabelColor = Color(0xFFA5B1C2),
                            cursorColor = Color(0xFF144326)
                        ),
                        textStyle = TextStyle(color = Color(0xFF0A1F44), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onLogin(username, password) },
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF144326),
                                contentColor = Color.White
                            ),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = "Log In", 
                                    fontSize = 18.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        if (isBiometricAvailable) {
                            Spacer(modifier = Modifier.width(14.dp))
                            Surface(
                                onClick = onBiometricLogin,
                                shape = CircleShape,
                                color = Color.White,
                                border = BorderStroke(1.5.dp, Color(0xFF144326)),
                                modifier = Modifier.size(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Biometric Login",
                                        tint = Color(0xFF2CC56A),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }

                    errorMessage?.let {
                        Text(
                            text = it,
                            color = Color(0xFFC93030),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "Authorized users only.",
                        color = Color(0xFF7A869A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
