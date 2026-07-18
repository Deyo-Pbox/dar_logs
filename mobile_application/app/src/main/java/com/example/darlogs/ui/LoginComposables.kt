package com.example.darlogs.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darlogs.R
import com.example.darlogs.ui.theme.*
import java.util.*

@Composable
fun LoginScreen(
    lastUsername: String,
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: (String, String) -> Unit,
    onBiometricLogin: () -> Unit,
    isBiometricAvailable: Boolean
) {
    var username by remember { mutableStateOf(lastUsername) }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val hasRememberedUsername = lastUsername.isNotEmpty()

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
                .imePadding()
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

                    if (hasRememberedUsername) {
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

                        Spacer(modifier = Modifier.height(24.dp))
                    } else {
                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                usernameError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Username", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
                            placeholder = { Text("Enter your username", color = Color(0xFFA5B1C2)) },
                            isError = usernameError != null,
                            supportingText = usernameError?.let { { Text(it, color = Color(0xFFC93030), fontSize = 12.sp) } },
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
                            textStyle = TextStyle(color = Color(0xFF0A1F44), fontSize = 16.sp, fontWeight = FontWeight.Medium),
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
                        placeholder = { Text("Enter your password", color = Color(0xFFA5B1C2)) },
                        isError = passwordError != null,
                        supportingText = passwordError?.let { { Text(it, color = Color(0xFFC93030), fontSize = 12.sp) } },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                    tint = if (isPasswordVisible) Color(0xFF144326) else Color(0xFFA5B1C2)
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
                        textStyle = TextStyle(color = Color(0xFF0A1F44), fontSize = 16.sp, fontWeight = FontWeight.Medium),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val u = username.trim()
                                val p = password

                                usernameError = if (u.isEmpty()) "Username is required" else null
                                passwordError = if (p.isEmpty()) "Password is required" else null

                                if (usernameError == null && passwordError == null) {
                                    onLogin(u, p)
                                }
                            },
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
                            AnimatedContent(
                                targetState = isLoading,
                                transitionSpec = {
                                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                                },
                                label = "buttonContent"
                            ) { loading ->
                                if (loading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Log In",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (isBiometricAvailable && hasRememberedUsername) {
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
                        AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically { it }) {
                            Surface(
                                color = Color(0xFFFFF0F0),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text(
                                    text = it,
                                    color = Color(0xFFC93030),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }

                    if (!hasRememberedUsername) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFE0E0E0)))
                            Text(
                                text = "   Authorized users only   ",
                                color = Color(0xFF7A869A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFE0E0E0)))
                        }
                    } else {
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
}
