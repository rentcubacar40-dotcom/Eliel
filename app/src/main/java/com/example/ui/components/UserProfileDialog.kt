package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.UserEntity
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.NexusGold
import com.example.ui.theme.NexusPrimary
import com.example.ui.theme.NexusViolet
import com.example.ui.theme.OnlineGreen

@Composable
fun UserProfileDialog(
    user: UserEntity,
    isCurrentUser: Boolean = false,
    onDismiss: () -> Unit,
    onStartDirectChat: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val isAppOwner = user.username.equals("Eliel_21", ignoreCase = true) ||
            user.displayName.contains("Eliel", ignoreCase = true)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = if (isAppOwner) {
                            listOf(NexusGold, Color(0xFFFF8F00), NexusGold)
                        } else {
                            listOf(CyberNeonCyan, NexusPrimary, NexusViolet)
                        }
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .testTag("user_profile_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0C1322)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Perfil de Usuario",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberNeonCyan,
                        letterSpacing = 1.sp
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Avatar with Neon Glowing Ring
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                colors = if (isAppOwner) {
                                    listOf(NexusGold, Color(0xFFFF6F00), NexusGold)
                                } else {
                                    listOf(CyberNeonCyan, NexusPrimary, NexusViolet, CyberNeonCyan)
                                }
                            )
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TelegramAvatar(
                        imageUrl = user.avatarUrl,
                        name = user.displayName,
                        size = 90.dp,
                        isGroup = false,
                        showOnlineDot = true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Display Name + Crown / Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = user.displayName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    if (isAppOwner) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verificado",
                            tint = NexusGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Username handle with copy chip
                Surface(
                    onClick = {
                        clipboardManager.setText(AnnotatedString("@${user.username}"))
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "@${user.username}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyberNeonCyan
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar usuario",
                            tint = CyberNeonCyan.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Special Role Pill
                if (isAppOwner) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = NexusGold.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NexusGold.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "👑 Creador y Propietario de Nexus",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusGold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bio Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "INFORMACIÓN Y BIOGRAFÍA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (user.bio.isNotBlank()) user.bio else "Miembro activo de la comunidad universitaria UCF.",
                            fontSize = 13.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(OnlineGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "En línea • Red Nexus UCF",
                                fontSize = 11.sp,
                                color = OnlineGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Button: Direct Chat
                if (!isCurrentUser) {
                    Button(
                        onClick = onStartDirectChat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_direct_chat_from_profile_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NexusPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enviar Mensaje Directo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
