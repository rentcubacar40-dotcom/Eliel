package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.StatusEntity
import com.example.data.local.UserEntity
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NexusGold
import com.example.ui.theme.NexusPrimary
import com.example.ui.theme.NexusViolet
import com.example.ui.theme.OnlineGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatusStoriesBar(
    statuses: List<StatusEntity>,
    currentUser: UserEntity?,
    onAddStatusClick: () -> Unit,
    onStatusClick: (StatusEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val myStatus = statuses.firstOrNull { it.authorUsername == currentUser?.username }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF090E1A))
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Estados",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676))
                )
            }

            Text(
                text = "Duran 24 horas",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // First item: "Mi Estado"
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(68.dp)
                        .clickable {
                            if (myStatus != null) {
                                onStatusClick(myStatus)
                            } else {
                                onAddStatusClick()
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (myStatus != null) {
                            // Ring around my status
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(CyberNeonCyan, NexusPrimary, NexusViolet, CyberNeonCyan)
                                        )
                                    )
                                    .padding(2.dp)
                            ) {
                                TelegramAvatar(
                                    imageUrl = currentUser?.avatarUrl,
                                    name = currentUser?.displayName ?: "Yo",
                                    size = 52.dp
                                )
                            }
                        } else {
                            TelegramAvatar(
                                imageUrl = currentUser?.avatarUrl,
                                name = currentUser?.displayName ?: "Yo",
                                size = 54.dp
                            )
                            // Plus Badge
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(Color(0xFF090E1A))
                                    .padding(1.5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(CyberNeonCyan),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Publicar Estado",
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (myStatus != null) "Mi Estado" else "Añadir",
                        fontSize = 11.sp,
                        fontWeight = if (myStatus != null) FontWeight.Bold else FontWeight.Normal,
                        color = if (myStatus != null) CyberNeonCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Other statuses
            val otherStatuses = statuses.filter { it.authorUsername != currentUser?.username }
            items(otherStatuses, key = { it.id }) { status ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(68.dp)
                        .clickable { onStatusClick(status) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(CyberNeonCyan, NexusPrimary, NexusViolet, OnlineGreen, CyberNeonCyan)
                                )
                            )
                            .padding(2.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TelegramAvatar(
                            imageUrl = status.authorAvatar,
                            name = status.authorDisplayName,
                            size = 51.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = status.authorDisplayName.split(" ").firstOrNull() ?: status.authorUsername,
                        fontSize = 11.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CreateStatusDialog(
    currentUser: UserEntity?,
    onDismiss: () -> Unit,
    onPublish: (text: String, mediaUrl: String?, colorHex: String) -> Unit
) {
    var statusText by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#0284C7") }
    var mediaUriString by remember { mutableStateOf<String?>(null) }

    val colorOptions = listOf(
        "#0284C7" to "Azul",
        "#7C3AED" to "Violeta",
        "#E11D48" to "Carmesí",
        "#059669" to "Esmeralda",
        "#D97706" to "Ámbar",
        "#0F172A" to "Noche"
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            mediaUriString = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Nuevo Estado", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "24h",
                    fontSize = 11.sp,
                    color = CyberNeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Comparte un mensaje o pensamiento con tus compañeros de la UCF.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Status text input
                OutlinedTextField(
                    value = statusText,
                    onValueChange = { statusText = it },
                    placeholder = { Text("¿Qué estás pensando?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberNeonCyan,
                        unfocusedBorderColor = CyberBorder
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Color de fondo:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorOptions.forEach { (hex, _) ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optional Image Picker Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF131D2E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = CyberNeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (mediaUriString != null) "Foto seleccionada ✓" else "Adjuntar foto al estado (opcional)",
                            fontSize = 12.sp,
                            color = if (mediaUriString != null) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (statusText.isNotBlank() || mediaUriString != null) {
                        onPublish(statusText.trim(), mediaUriString, selectedColor)
                    }
                },
                enabled = statusText.isNotBlank() || mediaUriString != null,
                colors = ButtonDefaults.buttonColors(containerColor = NexusPrimary)
            ) {
                Text("Publicar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun StoryViewerDialog(
    status: StatusEntity,
    currentUsername: String?,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(status.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 6000, easing = LinearEasing)
        )
        onDismiss()
    }

    val context = LocalContext.current
    val parsedColor = remember(status.backgroundColorHex) {
        try {
            Color(android.graphics.Color.parseColor(status.backgroundColorHex))
        } catch (e: Exception) {
            Color(0xFF0284C7)
        }
    }

    val timeFormatted = remember(status.createdAt) {
        val diffMinutes = (System.currentTimeMillis() - status.createdAt) / (1000 * 60)
        when {
            diffMinutes < 1 -> "Hace un momento"
            diffMinutes < 60 -> "Hace ${diffMinutes}m"
            else -> "Hace ${diffMinutes / 60}h"
        }
    }

    val canDelete = isAdmin || status.authorUsername == currentUsername || currentUsername == "Eliel_21"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Background Image or Solid Color
            if (!status.mediaUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(status.mediaUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Dark overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(parsedColor)
                )
            }

            // Top Section: Progress Bar & Author Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopCenter)
            ) {
                LinearProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TelegramAvatar(
                            imageUrl = status.authorAvatar,
                            name = status.authorDisplayName,
                            size = 38.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = status.authorDisplayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                if (status.authorUsername.equals("Eliel_21", ignoreCase = true)) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("👑", fontSize = 12.sp)
                                }
                            }
                            Text(
                                text = "@${status.authorUsername} • $timeFormatted",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Row {
                        if (canDelete) {
                            IconButton(onClick = onDelete) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Borrar estado",
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Center Content: Status Text
            if (status.text.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = status.text,
                        color = Color.White,
                        fontSize = if (status.text.length < 60) 24.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 30.sp
                    )
                }
            }
        }
    }
}
