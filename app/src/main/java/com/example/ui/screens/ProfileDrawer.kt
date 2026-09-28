package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserEntity
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.TelegramAvatar
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NexusGold
import com.example.ui.theme.NexusPrimary
import com.example.ui.theme.NexusViolet
import com.example.ui.theme.OnlineGreen
import kotlinx.coroutines.launch

@Composable
fun ProfileDrawerContent(
    viewModel: MainViewModel,
    onCloseDrawer: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showDirectoryDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var directoryUsers by remember { mutableStateOf<List<UserEntity>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()
    val isOwner = currentUser?.username?.equals("Eliel_21", ignoreCase = true) == true

    ModalDrawerSheet(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight(),
        drawerContainerColor = Color(0xFF090E17)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Profile Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                if (isOwner) Color(0xFF2E2005) else Color(0xFF0C192E),
                                Color(0xFF090E17)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // User Avatar with Ring
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        if (isOwner) listOf(NexusGold, Color(0xFFFF6F00), NexusGold)
                                        else listOf(CyberNeonCyan, NexusPrimary, NexusViolet, CyberNeonCyan)
                                    )
                                )
                                .padding(2.5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TelegramAvatar(
                                imageUrl = currentUser?.avatarUrl,
                                name = currentUser?.displayName ?: "Usuario",
                                size = 63.dp,
                                showOnlineDot = true
                            )
                        }

                        if (isOwner) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NexusGold.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NexusGold.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "👑", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Creador",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NexusGold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentUser?.displayName ?: "Usuario",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isOwner) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = NexusGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "@${currentUser?.username ?: "usuario"}",
                        fontSize = 13.sp,
                        color = CyberNeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (!currentUser?.bio.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentUser?.bio ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(OnlineGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "En línea • Red Nexus",
                            fontSize = 11.sp,
                            color = OnlineGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            HorizontalDivider(color = CyberBorder, modifier = Modifier.padding(horizontal = 16.dp))

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Items
            NavigationDrawerItem(
                label = { Text("Mis Conversaciones", fontWeight = FontWeight.Medium) },
                icon = { Icon(Icons.Default.Message, contentDescription = null, tint = CyberNeonCyan) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    viewModel.navigateTo(Screen.ChatList)
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                label = { Text("Directorio de Usuarios", fontWeight = FontWeight.Medium) },
                icon = { Icon(Icons.Default.People, contentDescription = null, tint = NexusViolet) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    coroutineScope.launch {
                        directoryUsers = viewModel.getAllDirectoryUsers()
                        showDirectoryDialog = true
                    }
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                label = { Text("Editar Mi Perfil (@usuario)", fontWeight = FontWeight.Medium) },
                icon = { Icon(Icons.Default.Person, contentDescription = null, tint = CyberNeonCyan) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    showEditProfileDialog = true
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            HorizontalDivider(color = CyberBorder, modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp))

            NavigationDrawerItem(
                label = { Text("Acerca de Nexus", fontWeight = FontWeight.Medium) },
                icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    showAboutDialog = true
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                label = { Text("Cerrar Sesión", fontWeight = FontWeight.Medium, color = ErrorRed) },
                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = ErrorRed) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    viewModel.logout()
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var editUsername by remember(currentUser) { mutableStateOf(currentUser?.username ?: "") }
        var editName by remember(currentUser) { mutableStateOf(currentUser?.displayName ?: "") }
        var editBio by remember(currentUser) { mutableStateOf(currentUser?.bio ?: "") }
        var editAvatar by remember(currentUser) { mutableStateOf(currentUser?.avatarUrl ?: "") }
        var usernameError by remember { mutableStateOf<String?>(null) }
        var isSaving by remember { mutableStateOf(false) }

        val editAvatarPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            if (uri != null) {
                editAvatar = uri.toString()
            }
        }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showEditProfileDialog = false },
            title = { Text("Editar Perfil", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TelegramAvatar(
                            imageUrl = editAvatar,
                            name = editName.ifBlank { editUsername },
                            size = 54.dp,
                            onClick = {
                                editAvatarPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(onClick = {
                            editAvatarPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }) {
                            Text("Cambiar Foto", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nombre Completo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = {
                            editUsername = it.replace(" ", "")
                            usernameError = null
                        },
                        label = { Text("Nombre de Usuario Único (@)") },
                        leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = usernameError != null
                    )

                    if (usernameError != null) {
                        Text(
                            text = usernameError ?: "",
                            color = ErrorRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Biografía / Carrera") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = editUsername.trim().removePrefix("@")
                        if (clean.length < 3) {
                            usernameError = "Mínimo 3 caracteres."
                            return@Button
                        }
                        isSaving = true
                        coroutineScope.launch {
                            val available = viewModel.checkUsernameAvailability(clean)
                            if (!available && !clean.equals(currentUser?.username, ignoreCase = true)) {
                                isSaving = false
                                usernameError = "El usuario @$clean ya está en uso."
                            } else {
                                viewModel.updateProfileWithUsername(
                                    newUsername = clean,
                                    displayName = editName.trim(),
                                    bio = editBio.trim(),
                                    avatarUrl = editAvatar.trim()
                                ) { success, errorMsg ->
                                    isSaving = false
                                    if (success) {
                                        showEditProfileDialog = false
                                    } else {
                                        usernameError = errorMsg
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isSaving
                ) {
                    Text(if (isSaving) "Guardando..." else "Guardar Cambios")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditProfileDialog = false },
                    enabled = !isSaving
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Directory of Users Dialog
    if (showDirectoryDialog) {
        AlertDialog(
            onDismissRequest = { showDirectoryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, contentDescription = null, tint = CyberNeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Directorio de Usuarios", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Usuarios registrados en la red Nexus. Toca para ver su perfil o escribirle un mensaje directo:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (directoryUsers.isEmpty()) {
                        Text(
                            text = "No se encontraron otros usuarios en el directorio.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(directoryUsers) { u ->
                                val isEliel = u.username.equals("Eliel_21", ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0F1728),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            showDirectoryDialog = false
                                            viewModel.inspectUserEntity(u)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TelegramAvatar(
                                            imageUrl = u.avatarUrl,
                                            name = u.displayName,
                                            size = 40.dp,
                                            showOnlineDot = true
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = u.displayName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color.White
                                                )
                                                if (isEliel) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(text = "👑", fontSize = 12.sp)
                                                }
                                            }
                                            Text(
                                                text = "@${u.username}",
                                                fontSize = 11.sp,
                                                color = CyberNeonCyan
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = "Chat",
                                            tint = NexusPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDirectoryDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text("Acerca de Nexus Chat", fontWeight = FontWeight.Black)
            },
            text = {
                Column {
                    Text("Nexus University Messenger", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Versión 2.0 • Arquitectura Cyber-Glass 100% Real", fontSize = 12.sp, color = CyberNeonCyan)
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NexusGold.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NexusGold.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "👑 Creador y Propietario:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NexusGold
                            )
                            Text(
                                text = "Eliel (@Eliel_21)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Sistema de mensajería con sincronización e intercambio estructurado mediante paquetes JSON persistidos localmente en Room y soporte de red.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}
