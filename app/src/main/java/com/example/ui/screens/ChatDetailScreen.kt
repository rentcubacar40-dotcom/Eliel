package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.MessageEntity
import com.example.ui.MainViewModel
import com.example.ui.components.GroupInfoDialog
import com.example.ui.components.TelegramAvatar
import com.example.ui.components.UserProfileDialog
import com.example.ui.theme.CheckmarkBlue
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NexusGold
import com.example.ui.theme.NexusPrimary
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TelegramDarkIncomingBubble
import com.example.ui.theme.TelegramDarkOutgoingBubble
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeChat by viewModel.activeChat.collectAsState()
    val messages by viewModel.activeMessages.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val activeGroupMembers by viewModel.activeGroupMembers.collectAsState()
    val inspectingUser by viewModel.inspectingUser.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var messageInput by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showChatInfoDialog by remember { mutableStateOf(false) }
    var showExportJsonDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }
    var selectedImageForViewer by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var selectedMessageForOptions by remember { mutableStateOf<MessageEntity?>(null) }

    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Google Play compliant Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendAttachment(uri, isImage = true)
        }
    }

    // Document Picker for files <= 4MB
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendAttachment(uri, isImage = false)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                if (activeChat?.type == "GROUP") {
                                    showChatInfoDialog = true
                                } else {
                                    val targetUsername = activeChat?.id?.removePrefix("direct_") ?: ""
                                    if (targetUsername.isNotBlank()) {
                                        viewModel.inspectUser(targetUsername)
                                    }
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        TelegramAvatar(
                            imageUrl = activeChat?.avatarUrl,
                            name = activeChat?.title ?: "Chat",
                            size = 40.dp,
                            isGroup = activeChat?.type == "GROUP",
                            showOnlineDot = activeChat?.type == "DIRECT"
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = activeChat?.title ?: "Chat",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            val isOwnerChat = activeChat?.id?.contains("Eliel_21", ignoreCase = true) == true ||
                                    activeChat?.title?.contains("Eliel", ignoreCase = true) == true
                            val subtitle = when {
                                isOwnerChat -> "👑 Creador y Propietario • en línea"
                                activeChat?.type == "GROUP" -> activeChat?.description?.takeIf { it.isNotBlank() } ?: "Grupo"
                                else -> "en línea"
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isOwnerChat && activeChat?.type != "GROUP") {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00E676))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                }
                                Text(
                                    text = subtitle,
                                    fontSize = 12.sp,
                                    color = if (isOwnerChat) Color(0xFFFFB300) else if (activeChat?.type != "GROUP") Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isOwnerChat) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Información del Chat") },
                                onClick = {
                                    menuExpanded = false
                                    showChatInfoDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Exportar JSON") },
                                onClick = {
                                    menuExpanded = false
                                    activeChat?.let { c ->
                                        coroutineScope.launch {
                                            exportedJsonText = viewModel.getExportJson(c.id)
                                            showExportJsonDialog = true
                                        }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Vaciar chat (Borrar historial local)") },
                                onClick = {
                                    menuExpanded = false
                                    showClearChatDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = ErrorRed) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Upload Progress Banner
                AnimatedVisibility(visible = uploadProgress.isUploading) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = NexusPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Subiendo archivo...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "${uploadProgress.percentage}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NexusPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { uploadProgress.percentage / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = NexusPrimary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${uploadProgress.fileName} (${String.format(Locale.US, "%.1f", uploadProgress.totalBytes / (1024.0 * 1024.0))} MB)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Input Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showAttachmentSheet = true },
                        modifier = Modifier.testTag("attach_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Adjuntar archivo",
                            tint = NexusPrimary
                        )
                    }

                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("Escribe un mensaje...", fontSize = 15.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("message_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexusPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (messageInput.isNotBlank()) {
                                viewModel.sendMessage(messageInput.trim())
                                messageInput = ""
                            }
                        })
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                viewModel.sendMessage(messageInput.trim())
                                messageInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NexusPrimary)
                            .testTag("send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "💬 Conversación activa en Nexus",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, bottom = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay mensajes en esta conversación.\nEscribe el primer mensaje para comenzar.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            items(messages, key = { it.id }) { message ->
                TelegramMessageBubble(
                    message = message,
                    isGroup = activeChat?.type == "GROUP",
                    onImageClick = { url -> selectedImageForViewer = url },
                    onCopyLink = { url ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Enlace", url)
                        clipboard.setPrimaryClip(clip)
                    },
                    onOpenBrowser = { url ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (ignored: Exception) {}
                    },
                    onSenderClick = { senderId ->
                        viewModel.inspectUser(senderId)
                    },
                    onBubbleClick = {
                        selectedMessageForOptions = message
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Attachment Modal Bottom Sheet
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Compartir Multimedia",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Límite máximo por archivo: 4.0 MB",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(18.dp))

                // Gallery Photo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showAttachmentSheet = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(NexusPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = NexusPrimary)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Foto de Galería", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("Envía una foto de alta calidad (≤ 4.0 MB)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // File/Document
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showAttachmentSheet = false
                            filePickerLauncher.launch("*/*")
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8F51E8).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, tint = Color(0xFF8F51E8))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Archivo / Documento", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("PDF, ZIP, APK, Word (≤ 4.0 MB)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // Group Info & Member Management Dialog
    if (showChatInfoDialog && activeChat != null && activeChat?.type == "GROUP") {
        val chat = activeChat!!
        GroupInfoDialog(
            chat = chat,
            members = activeGroupMembers,
            currentUser = currentUser,
            onDismiss = { showChatInfoDialog = false },
            onInspectMember = { user ->
                viewModel.inspectUserEntity(user)
            },
            onAddMember = { username ->
                viewModel.addMemberToGroup(chat.id, username)
            },
            onRemoveMember = { username ->
                viewModel.removeMemberFromGroup(chat.id, username)
            },
            onChangeRole = { username, newRole ->
                viewModel.updateMemberRole(chat.id, username, newRole)
            },
            onLoadDirectoryUsers = {
                viewModel.getAllDirectoryUsers()
            }
        )
    }

    // Interactive User Profile Dialog
    if (inspectingUser != null) {
        UserProfileDialog(
            user = inspectingUser!!,
            isCurrentUser = inspectingUser!!.isCurrentUser,
            onDismiss = { viewModel.closeUserInspection() },
            onStartDirectChat = {
                viewModel.openDirectChatWithInspectedUser()
            }
        )
    }

    // Export JSON Dialog
    if (showExportJsonDialog) {
        AlertDialog(
            onDismissRequest = { showExportJsonDialog = false },
            title = { Text("Estructura JSON del Chat") },
            text = {
                Column {
                    Text(
                        "Datos del chat formateados en JSON:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = exportedJsonText,
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Chat JSON", exportedJsonText))
                    showExportJsonDialog = false
                }) {
                    Text("Copiar JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportJsonDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Fullscreen Image Viewer
    if (selectedImageForViewer != null) {
        Dialog(
            onDismissRequest = { selectedImageForViewer = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(selectedImageForViewer)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto ampliada",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { selectedImageForViewer = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
            }
        }
    }

    // Message Contextual Options Dialog (Copy, Delete Attachment, Delete Message)
    if (selectedMessageForOptions != null) {
        val msg = selectedMessageForOptions!!
        val isAuthor = msg.isOutgoing || msg.senderId == currentUser?.username
        val isAdmin = currentUser?.role == "OWNER" || currentUser?.username == "Eliel_21"
        val canDelete = isAuthor || isAdmin

        AlertDialog(
            onDismissRequest = { selectedMessageForOptions = null },
            title = {
                Text(text = "Opciones del Mensaje", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (msg.text.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Mensaje", msg.text))
                                    selectedMessageForOptions = null
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyberNeonCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Copiar texto", fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (!msg.attachmentUrl.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.deleteAttachment(msg.id)
                                    selectedMessageForOptions = null
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = NexusGold, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Borrar archivo adjunto", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Libera espacio local en tu dispositivo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (canDelete) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ErrorRed.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.deleteMessage(msg.id)
                                    selectedMessageForOptions = null
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Eliminar mensaje", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ErrorRed)
                                    Text(
                                        if (isAdmin && !isAuthor) "Moderación de Administrador (@Eliel_21)" else "Se borra de la base de datos Room local",
                                        fontSize = 11.sp,
                                        color = ErrorRed.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedMessageForOptions = null }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Confirm Clear Chat Dialog
    if (showClearChatDialog && activeChat != null) {
        val chat = activeChat!!
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = {
                Text("Vaciar historial del chat", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas vaciar todos los mensajes de \"${chat.title}\"? Se borrarán de la base de datos Room de tu teléfono para liberar almacenamiento.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearChat(chat.id)
                        showClearChatDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Vaciar Chat", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun TelegramMessageBubble(
    message: MessageEntity,
    isGroup: Boolean,
    onImageClick: (String) -> Unit,
    onCopyLink: (String) -> Unit,
    onOpenBrowser: (String) -> Unit,
    onSenderClick: ((String) -> Unit)? = null,
    onBubbleClick: (() -> Unit)? = null
) {
    val isOutgoing = message.isOutgoing
    val alignment = if (isOutgoing) Alignment.End else Alignment.Start
    val bubbleColor = if (isOutgoing) {
        TelegramDarkOutgoingBubble
    } else {
        TelegramDarkIncomingBubble
    }

    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 4.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 4.dp,
            bottomEnd = 16.dp
        )
    }

    val formattedTime = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 80.dp, max = 320.dp)
                .clip(bubbleShape)
                .background(bubbleColor)
                .then(
                    if (onBubbleClick != null) Modifier.clickable { onBubbleClick() } else Modifier
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column {
                // Sender Name and Admin/Owner Verification Badge
                val isOwnerMessage = message.senderId.equals("Eliel_21", ignoreCase = true) ||
                        message.senderName.contains("Eliel_21", ignoreCase = true) ||
                        message.senderName.contains("Eliel", ignoreCase = true)

                if (!isOutgoing && (isGroup || isOwnerMessage)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onSenderClick?.invoke(message.senderId) }
                            .padding(bottom = 2.dp)
                    ) {
                        Text(
                            text = message.senderName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOwnerMessage) Color(0xFFFFB300) else NexusPrimary
                        )
                        if (isOwnerMessage) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFFB300))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "👑 CREADOR",
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                // Image Attachment
                if (message.attachmentType == "IMAGE" && !message.attachmentUrl.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onImageClick(message.attachmentUrl) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(message.attachmentUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = message.attachmentName ?: "Foto",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                        if (message.attachmentSize > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", message.attachmentSize / (1024.0 * 1024.0))} MB",
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Document / File Attachment (<= 4MB)
                if (message.attachmentType == "DOCUMENT" || (message.attachmentUrl != null && message.attachmentType != "IMAGE")) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(NexusPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = message.attachmentName ?: "Archivo adjunto",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val sizeText = if (message.attachmentSize > 0) {
                                    "${String.format(Locale.US, "%.2f", message.attachmentSize / (1024.0 * 1024.0))} MB"
                                } else {
                                    "Documento"
                                }
                                Text(
                                    text = sizeText,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Copy Link / Open Button
                            IconButton(
                                onClick = { message.attachmentUrl?.let { onCopyLink(it) } },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar Enlace",
                                    modifier = Modifier.size(16.dp),
                                    tint = NexusPrimary
                                )
                            }
                            IconButton(
                                onClick = { message.attachmentUrl?.let { onOpenBrowser(it) } },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = "Abrir Enlace",
                                    modifier = Modifier.size(16.dp),
                                    tint = NexusPrimary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Text Content
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                // Timestamp and Delivery Status
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (message.status) {
                            "PENDING" -> Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Enviando",
                                tint = Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.size(12.dp)
                            )
                            "SENT", "DELIVERED" -> Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Entregado",
                                tint = CheckmarkBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            else -> Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "Enviado",
                                tint = Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
