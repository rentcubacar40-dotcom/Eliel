package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.MainViewModel
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NexusGold
import com.example.ui.theme.NexusPrimary
import com.example.ui.theme.NexusViolet
import com.example.ui.theme.OnlineGreen

@Composable
fun SetupProfileScreen(
    viewModel: MainViewModel,
    onSetupComplete: () -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var avatarUriString by remember { mutableStateOf("") }

    // Validation state
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    val cleanUsername = username.trim().removePrefix("@")
    val isAppCreator = cleanUsername.equals("Eliel_21", ignoreCase = true)

    // Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            avatarUriString = uri.toString()
        }
    }

    // Live real-time check for username availability
    LaunchedEffect(cleanUsername) {
        if (cleanUsername.length >= 3) {
            isUsernameAvailable = viewModel.checkUsernameAvailability(cleanUsername)
        } else {
            isUsernameAvailable = null
        }
    }

    val isValid = cleanUsername.length >= 3 &&
            isUsernameAvailable == true &&
            displayName.trim().isNotBlank() &&
            !isSubmitting

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF070B12)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // App Cyber-Glass Emblem
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CyberNeonCyan, NexusPrimary, NexusViolet)
                        )
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFF0C1322)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "N",
                        color = CyberNeonCyan,
                        fontWeight = FontWeight.Black,
                        fontSize = 36.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "NEXUS CHAT",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = Color.White
            )

            Text(
                text = "Plataforma de Mensajería y Red Universitaria",
                fontSize = 12.sp,
                color = CyberNeonCyan,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Avatar Selector with Cyber Ring
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(listOf(CyberNeonCyan, NexusViolet)),
                        shape = CircleShape
                    )
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (avatarUriString.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(avatarUriString)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF141E2C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Elegir foto",
                                tint = CyberNeonCyan,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Añadir foto",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Text(
                text = "Toca para elegir tu foto de perfil",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Registration Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1626)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "CREA TU IDENTIDAD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberNeonCyan,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Display Name Field
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = {
                            displayName = it
                            errorMessage = null
                        },
                        label = { Text("Nombre Completo") },
                        placeholder = { Text("Ej. Eliel Roselló") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = CyberNeonCyan)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("display_name_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberNeonCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedLabelColor = CyberNeonCyan,
                            focusedContainerColor = Color(0xFF121B2F),
                            unfocusedContainerColor = Color(0xFF0F1728)
                        ),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Username Field with real-time uniqueness validation
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it.replace(" ", "")
                            errorMessage = null
                        },
                        label = { Text("Nombre de Usuario Único") },
                        placeholder = { Text("usuario (sin espacios)") },
                        leadingIcon = {
                            Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = CyberNeonCyan)
                        },
                        trailingIcon = {
                            when {
                                cleanUsername.length < 3 -> {}
                                isUsernameAvailable == true -> {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Disponible",
                                        tint = OnlineGreen
                                    )
                                }
                                isUsernameAvailable == false -> {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Ocupado",
                                        tint = ErrorRed
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = when {
                                isUsernameAvailable == true -> OnlineGreen
                                isUsernameAvailable == false -> ErrorRed
                                else -> CyberNeonCyan
                            },
                            unfocusedBorderColor = CyberBorder,
                            focusedContainerColor = Color(0xFF121B2F),
                            unfocusedContainerColor = Color(0xFF0F1728)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                    )

                    // Real-time username feedback badge
                    Spacer(modifier = Modifier.height(6.dp))
                    when {
                        isAppCreator -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Text(
                                    text = "👑 Cuenta Oficial del Creador y Propietario de la Aplicación",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NexusGold
                                )
                            }
                        }
                        cleanUsername.length in 1..2 -> {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Mínimo 3 caracteres (solo letras, números y _)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        isUsernameAvailable == true -> {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OnlineGreen, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "✓ @$cleanUsername está disponible en la red",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnlineGreen
                                )
                            }
                        }
                        isUsernameAvailable == false -> {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "❌ @$cleanUsername ya existe. Elige otro nombre.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ErrorRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bio Field
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Biografía / Carrera / Intereses (Opcional)") },
                        placeholder = { Text("Ej. Estudiante de Ingeniería Informática UCF...") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bio_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberNeonCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedContainerColor = Color(0xFF121B2F),
                            unfocusedContainerColor = Color(0xFF0F1728)
                        )
                    )
                }
            }

            // Error Banner if needed
            AnimatedVisibility(visible = errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = ErrorRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Enter Nexus Neon Button
            Button(
                onClick = {
                    if (!isValid) return@Button
                    isSubmitting = true
                    errorMessage = null

                    viewModel.registerUser(
                        username = cleanUsername,
                        displayName = displayName.trim(),
                        bio = bio.trim(),
                        avatarUrl = avatarUriString.trim()
                    ) { success, msg ->
                        isSubmitting = false
                        if (success) {
                            onSetupComplete()
                        } else {
                            errorMessage = msg
                        }
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("setup_profile_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NexusPrimary,
                    disabledContainerColor = NexusPrimary.copy(alpha = 0.3f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Configurando red...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                } else {
                    Text(
                        text = "Entrar a Nexus",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Nexus Chat • 100% Comunicación Real • Sincronización JSON",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
