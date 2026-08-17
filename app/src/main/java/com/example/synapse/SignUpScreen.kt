package com.example.synapse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.random.Random

// Password Strength representation
private enum class PasswordStrength(val label: String, val color: Color, val progress: Float) {
    EMPTY("", Color.Transparent, 0f),
    WEAK("Weak", Color(0xFFEF4444), 0.33f),      // HighRisk red
    MEDIUM("Medium", Color(0xFFF59E0B), 0.66f),  // Amber yellow
    STRONG("Strong", Color(0xFF10B981), 1f)     // CalmRisk green
}

private data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(onSignUpSuccess: (email: String, username: String) -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    // ── Input Fields States ──
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf<String?>(null) }

    // ── Animation & Transition States ──
    var isBursting by remember { mutableStateOf(false) }
    val cardAlpha = remember { Animatable(1f) }
    val cardScale = remember { Animatable(1f) }

    // ── Background Color State based on Password Strength ──
    val strength = remember(password) {
        if (password.isEmpty()) {
            PasswordStrength.EMPTY
        } else {
            var score = 0
            if (password.length >= 8) score++
            if (password.any { it.isDigit() }) score++
            if (password.any { it.isLetter() && it.isUpperCase() }) score++
            if (password.any { !it.isLetterOrDigit() }) score++

            when {
                score <= 1 -> PasswordStrength.WEAK
                score <= 3 -> PasswordStrength.MEDIUM
                else -> PasswordStrength.STRONG
            }
        }
    }

    // Determine the color of the neural network connections based on password strength
    val connectionColor by animateColorAsState(
        targetValue = when (strength) {
            PasswordStrength.EMPTY -> Color(0xFF818CF8) // Neutral neon violet
            PasswordStrength.WEAK -> PasswordStrength.WEAK.color
            PasswordStrength.MEDIUM -> PasswordStrength.MEDIUM.color
            PasswordStrength.STRONG -> PasswordStrength.STRONG.color
        },
        animationSpec = tween(500),
        label = "ConnectionColorTransition"
    )

    // ── Setup Particles ──
    val particles = remember {
        List(25) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                vx = (Random.nextFloat() - 0.5f) * 0.003f,
                vy = (Random.nextFloat() - 0.5f) * 0.003f,
                radius = Random.nextFloat() * 6f + 3f
            )
        }
    }

    // Canvas frame ticker
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { time ->
                tick = time
                
                // Physics update loop
                particles.forEach { p ->
                    if (isBursting) {
                        // Radial explosion outward from the center of the screen (0.5f, 0.5f)
                        val dx = p.x - 0.5f
                        val dy = p.y - 0.5f
                        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
                        
                        // Rapid acceleration outward
                        p.x += (dx / dist) * 0.06f
                        p.y += (dy / dist) * 0.06f
                    } else {
                        // Normal slow drift
                        p.x += p.vx
                        p.y += p.vy
                        if (p.x < 0 || p.x > 1) p.vx *= -1
                        if (p.y < 0 || p.y > 1) p.vy *= -1
                    }
                }
            }
        }
    }

    // ── Form Validation & Burst trigger ──
    val onSignUpClick = {
        when {
            email.isBlank() || !email.contains("@") -> {
                showError = "Please enter a valid email address."
            }
            username.trim().length < 3 -> {
                showError = "Username must be at least 3 characters."
            }
            password.length < 6 -> {
                showError = "Password must be at least 6 characters."
            }
            else -> {
                showError = null
                coroutineScope.launch {
                    isBursting = true
                    // Animate the card shrinking and fading out simultaneously
                    launch { cardAlpha.animateTo(0f, animationSpec = tween(500)) }
                    launch { cardScale.animateTo(0.6f, animationSpec = tween(600, easing = FastOutSlowInEasing)) }
                    
                    // Wait for the burst explosion animation to finish before proceeding
                    delay(800)
                    onSignUpSuccess(email, username)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0015), Color(0xFF0D0628), Color(0xFF0A1628))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // ── 1. Interactive Neural Network Background ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frame = tick
            val w = size.width
            val h = size.height

            // Connection threshold and line alpha scales down when bursting
            val maxDistance = 280f
            val lineAlphaMultiplier = if (isBursting) 0.15f else 0.5f

            // Draw connecting lines
            for (i in particles.indices) {
                val p1 = particles[i]
                val p1X = p1.x * w
                val p1Y = p1.y * h

                for (j in i + 1 until particles.size) {
                    val p2 = particles[j]
                    val p2X = p2.x * w
                    val p2Y = p2.y * h

                    val dx = p1X - p2X
                    val dy = p1Y - p2Y
                    val distance = sqrt(dx * dx + dy * dy)

                    if (distance < maxDistance) {
                        val alpha = (1f - (distance / maxDistance)) * lineAlphaMultiplier
                        drawLine(
                            color = connectionColor.copy(alpha = alpha),
                            start = Offset(p1X, p1Y),
                            end = Offset(p2X, p2Y),
                            strokeWidth = 2f
                        )
                    }
                }
            }

            // Draw particles (nodes)
            particles.forEach { p ->
                val px = p.x * w
                val py = p.y * h

                drawCircle(
                    color = connectionColor.copy(alpha = if (isBursting) 0.08f else 0.15f),
                    radius = p.radius * 2.5f,
                    center = Offset(px, py)
                )
                drawCircle(
                    color = connectionColor,
                    radius = p.radius,
                    center = Offset(px, py)
                )
            }
        }

        // ── 2. Dark Glassmorphic Sign Up Card ──
        Box(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.9f)
                .scale(cardScale.value)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = Color.Black.copy(alpha = 0.3f),
                    spotColor = Color.Black.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.06f * cardAlpha.value)) // Translucent card
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f * cardAlpha.value),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create Account",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = cardAlpha.value),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Sign up to begin system monitoring",
                    fontSize = 12.sp,
                    color = Color(0xFF818CF8).copy(alpha = cardAlpha.value),
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ── Email Input ──
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; showError = null },
                    label = { Text("Email Address", color = Color.White.copy(alpha = 0.8f)) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF818CF8),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        focusedBorderColor = Color(0xFF818CF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                        cursorColor = Color(0xFF818CF8)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // ── Username Input ──
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; showError = null },
                    label = { Text("Username", color = Color.White.copy(alpha = 0.8f)) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF818CF8),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        focusedBorderColor = Color(0xFF818CF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                        cursorColor = Color(0xFF818CF8)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // ── Password Input ──
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; showError = null },
                    label = { Text("Password", color = Color.White.copy(alpha = 0.8f)) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            EyeIcon(visible = passwordVisible, color = Color.White.copy(alpha = 0.5f))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF818CF8),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        focusedBorderColor = Color(0xFF818CF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                        cursorColor = Color(0xFF818CF8)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // ── Password Strength Bar ──
                AnimatedVisibility(visible = strength != PasswordStrength.EMPTY) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Password Strength",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = strength.label,
                                fontSize = 11.sp,
                                color = strength.color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        // Glowing strength bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(strength.progress)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(strength.color)
                            )
                        }
                    }
                }

                // ── Error Message ──
                AnimatedVisibility(visible = showError != null) {
                    showError?.let { err ->
                        Text(
                            text = err,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Register Button ──
                Button(
                    onClick = { onSignUpClick() },
                    enabled = !isBursting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF818CF8),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF818CF8).copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "Sign Up",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// ── Custom Hand-Drawn Eye Icon ────────────────────────────────────────────────
@Composable
private fun EyeIcon(visible: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val stroke = Stroke(width = 1.8f.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // 1. Draw Eye Outline (bezier curves)
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.15f, cy)
            quadraticTo(cx, cy - h * 0.3f, w * 0.85f, cy)
            quadraticTo(cx, cy + h * 0.3f, w * 0.15f, cy)
        }
        drawPath(path = path, color = color, style = stroke)

        // 2. Draw Iris
        drawCircle(color = color, radius = 3.5f.dp.toPx(), center = Offset(cx, cy))

        // 3. Draw slash line if visibility is disabled (VisibilityOff)
        if (!visible) {
            drawLine(
                color = color,
                start = Offset(w * 0.25f, h * 0.25f),
                end = Offset(w * 0.75f, h * 0.75f),
                strokeWidth = 1.8f.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
