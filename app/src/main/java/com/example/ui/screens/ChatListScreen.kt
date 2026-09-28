package com.example.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatEntity
import com.example.data.local.StatusEntity
import com.example.data.local.UserEntity
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.CreateStatusDialog
import com.example.ui.components.StatusStoriesBar
import com.example.ui.components.StoryViewerDialog
import com.example.ui.components.TelegramAvatar
import com.example.ui.components.UserProfileDialog
import com.example.ui.theme.CheckmarkBlue
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NexusGold
import com.example.ui.theme.NexusPrimary
import com.example.ui.theme.NexusViolet
import com.example.ui.theme.OnlineGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit
) {
    val chats by viewModel.filteredChats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val inspectingUser by viewModel.inspectingUser.collectAsState()
    val activeStatuses by viewModel.activeStatuses.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }
    var showCreateStatusDialog by remember { mutableStateOf(false) }
    var selectedStatusForViewer by remember { mutableStateOf<StatusEntity?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color(0xFF070B12),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B1220))
            ) {
                if (isSearchActive) {
                    TopAppBar(
                        title = {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.searchQuery.value = it },
                                placeholder = { Text("Buscar mensajes y chats...") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberNeonCyan,
                                    unfocusedBorderColor = CyberBorder
                                ),
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Limpiar")
                                        }
                                    }
                                }
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                isSearchActive = false
                                viewModel.searchQuery.value = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar búsqueda")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF0B1220)
                        )
                    )
                } else {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(CyberNeonCyan, NexusPrimary, NexusViolet)
                                            )
                                        )
                                        .padding(2.dp),
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
                                            fontSize = 17.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "NEXUS",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        letterSpacing = 1.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Red Universitaria UCF",
                                        fontSize = 10.sp,
                                        color = CyberNeonCyan,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onOpenDrawer,
                                modifier = Modifier.testTag("menu_button")
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Menú", tint = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Buscar", tint = CyberNeonCyan)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF0B1220)
                        )
                    )
                }

                // Tabs: Todos, Privados, Grupos
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF0B1220),
                    contentColor = CyberNeonCyan
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { viewModel.selectedTab.value = 0 },
                        text = {
                            Text(
                                "Todos",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) CyberNeonCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { viewModel.selectedTab.value = 1 },
                        text = {
                            Text(
                                "Privados",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) CyberNeonCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { viewModel.selectedTab.value = 2 },
                        text = {
                            Text(
                                "Grupos",
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 2) CyberNeonCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(visible = showFabMenu) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        // New Group Action
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFF0F1B2E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                            shadowElevation = 8.dp,
                            modifier = Modifier.clickable {
                                showFabMenu = false
                                showNewGroupDialog = true
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text("Nuevo Grupo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.GroupAdd, contentDescription = null, tint = NexusViolet)
                            }
                        }

                        // New Private Chat Action
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFF0F1B2E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                            shadowElevation = 8.dp,
                            modifier = Modifier.clickable {
                                showFabMenu = false
                                showNewChatDialog = true
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text("Nuevo Chat Privado", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = CyberNeonCyan)
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = NexusPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("new_chat_fab")
                ) {
                    Icon(
                        imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Edit,
                        contentDescription = "Nuevo Chat"
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stories / Estados bar
            item {
                StatusStoriesBar(
                    statuses = activeStatuses,
                    currentUser = currentUser,
                    onAddStatusClick = { showCreateStatusDialog = true },
                    onStatusClick = { status -> selectedStatusForViewer = status }
                )
                HorizontalDivider(
                    color = CyberBorder,
                    thickness = 0.5.dp
                )
            }

            if (chats.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(NexusPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = CyberNeonCyan
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Sin conversaciones aún",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Inicia un chat privado o escribe en la comunidad de la UCF para comenzar.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = { showNewChatDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = NexusPrimary),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("btn_empty_start_chat")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Iniciar primer chat", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(chats, key = { it.id }) { chat ->
                    val isOwnerChat = chat.id.contains("Eliel_21", ignoreCase = true) || chat.title.contains("Eliel", ignoreCase = true)

                    ChatItemRow(
                        chat = chat,
                        isOwnerChat = isOwnerChat,
                        onClick = { viewModel.navigateTo(Screen.ChatDetail(chat.id)) },
                        onAvatarClick = {
                            if (chat.type == "DIRECT") {
                                val username = chat.id.removePrefix("direct_")
                                viewModel.inspectUser(username)
                            } else {
                                viewModel.navigateTo(Screen.ChatDetail(chat.id))
                            }
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 76.dp),
                        color = CyberBorder,
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }

    // Inspecting User Modal Dialog
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

    // Create 24h Status Dialog
    if (showCreateStatusDialog) {
        CreateStatusDialog(
            currentUser = currentUser,
            onDismiss = { showCreateStatusDialog = false },
            onPublish = { text, mediaUrl, colorHex ->
                viewModel.publishStatus(text, mediaUrl, colorHex) {
                    showCreateStatusDialog = false
                }
            }
        )
    }

    // Story / Status Viewer Modal
    if (selectedStatusForViewer != null) {
        val status = selectedStatusForViewer!!
        StoryViewerDialog(
            status = status,
            currentUsername = currentUser?.username,
            isAdmin = currentUser?.role == "OWNER" || currentUser?.username == "Eliel_21",
            onDismiss = { selectedStatusForViewer = null },
            onDelete = {
                viewModel.deleteStatus(status.id)
                selectedStatusForViewer = null
            }
        )
    }

    // New Direct Chat Dialog WITH REAL USER VERIFICATION
    if (showNewChatDialog) {
        var usernameInput by remember { mutableStateOf("") }
        var foundUser by remember { mutableStateOf<UserEntity?>(null) }
        var isSearching by remember { mutableStateOf(false) }
        var directoryUsers by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
        val clean = usernameInput.trim().removePrefix("@")

        LaunchedEffect(Unit) {
            directoryUsers = viewModel.getAllDirectoryUsers()
        }

        // Live verify if user exists
        LaunchedEffect(clean) {
            if (clean.length >= 3) {
                isSearching = true
                foundUser = viewModel.checkUserExists(clean)
                isSearching = false
            } else {
                foundUser = null
            }
        }

        val canStart = foundUser != null && !foundUser!!.isCurrentUser

        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = CyberNeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nuevo Chat Privado", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "Introduce el nombre de usuario (@) del contacto. El sistema verificará que exista en la red Nexus:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it.replace(" ", "") },
                        label = { Text("Nombre de Usuario (@)") },
                        leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = CyberNeonCyan) },
                        trailingIcon = {
                            when {
                                clean.length < 3 -> {}
                                foundUser != null -> {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Verificado", tint = OnlineGreen)
                                }
                                else -> {
                                    Icon(Icons.Default.Error, contentDescription = "No existe", tint = ErrorRed)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_user_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (foundUser != null) OnlineGreen else if (clean.length >= 3) ErrorRed else CyberNeonCyan,
                            unfocusedBorderColor = CyberBorder
                        )
                    )

                    // Real-time user verification card
                    Spacer(modifier = Modifier.height(10.dp))
                    when {
                        foundUser != null -> {
                            val isEliel = foundUser!!.username.equals("Eliel_21", ignoreCase = true)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2038)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isEliel) NexusGold else OnlineGreen)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TelegramAvatar(
                                        imageUrl = foundUser!!.avatarUrl,
                                        name = foundUser!!.displayName,
                                        size = 38.dp,
                                        showOnlineDot = true
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = foundUser!!.displayName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.White
                                            )
                                            if (isEliel) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(text = "👑", fontSize = 11.sp)
                                            }
                                        }
                                        Text(
                                            text = "✓ Usuario verificado en Nexus",
                                            fontSize = 11.sp,
                                            color = OnlineGreen,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        clean.length >= 3 -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ErrorRed.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "El usuario @$clean no existe en la red Nexus.",
                                        fontSize = 11.sp,
                                        color = ErrorRed
                                    )
                                }
                            }
                        }
                    }

                    // Directory suggestions list
                    if (directoryUsers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "O selecciona un usuario del directorio:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                            items(directoryUsers) { u ->
                                val isEliel = u.username.equals("Eliel_21", ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0F1829),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clickable {
                                            usernameInput = u.username
                                            foundUser = u
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TelegramAvatar(
                                            imageUrl = u.avatarUrl,
                                            name = u.displayName,
                                            size = 32.dp,
                                            showOnlineDot = true
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = u.displayName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White
                                                )
                                                if (isEliel) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(text = "👑", fontSize = 10.sp)
                                                }
                                            }
                                            Text(
                                                text = "@${u.username}",
                                                fontSize = 11.sp,
                                                color = CyberNeonCyan
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (clean.equals(u.username, ignoreCase = true)) OnlineGreen else Color.Transparent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (canStart) {
                            viewModel.createDirectChat(clean)
                            showNewChatDialog = false
                        }
                    },
                    enabled = canStart,
                    colors = ButtonDefaults.buttonColors(containerColor = NexusPrimary),
                    modifier = Modifier.testTag("confirm_new_chat_button")
                ) {
                    Text("Iniciar Chat", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // New Group Dialog WITH MEMBER SELECTION
    if (showNewGroupDialog) {
        var groupTitle by remember { mutableStateOf("") }
        var groupDesc by remember { mutableStateOf("") }
        var directoryUsers by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
        val selectedMembers = remember { mutableStateListOf<String>() }

        LaunchedEffect(Unit) {
            directoryUsers = viewModel.getAllDirectoryUsers()
        }

        AlertDialog(
            onDismissRequest = { showNewGroupDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null, tint = NexusViolet)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crear Nuevo Grupo", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "Crea una comunidad para compartir mensajes y archivos:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = groupTitle,
                        onValueChange = { groupTitle = it },
                        label = { Text("Nombre del Grupo *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_group_title_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = groupDesc,
                        onValueChange = { groupDesc = it },
                        label = { Text("Descripción o Tema (opcional)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (directoryUsers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Añadir miembros iniciales:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                            items(directoryUsers) { u ->
                                val isSelected = u.username in selectedMembers
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSelected) selectedMembers.remove(u.username)
                                            else selectedMembers.add(u.username)
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TelegramAvatar(
                                        imageUrl = u.avatarUrl,
                                        name = u.displayName,
                                        size = 32.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = u.displayName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text(text = "@${u.username}", fontSize = 11.sp, color = CyberNeonCyan)
                                    }
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) selectedMembers.add(u.username)
                                            else selectedMembers.remove(u.username)
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = NexusPrimary)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupTitle.isNotBlank()) {
                            viewModel.createGroup(groupTitle.trim(), groupDesc.trim(), selectedMembers.toList())
                            showNewGroupDialog = false
                        }
                    },
                    enabled = groupTitle.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NexusPrimary),
                    modifier = Modifier.testTag("confirm_new_group_button")
                ) {
                    Text("Crear Grupo", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGroupDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ChatItemRow(
    chat: ChatEntity,
    isOwnerChat: Boolean = false,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val formattedDate = remember(chat.lastMessageTime) {
        val date = Date(chat.lastMessageTime)
        val now = Date()
        val diffHours = (now.time - date.time) / (1000 * 60 * 60)
        when {
            diffHours < 24 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            diffHours < 48 -> "Ayer"
            else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        TelegramAvatar(
            imageUrl = chat.avatarUrl,
            name = chat.title,
            size = 52.dp,
            isGroup = chat.type == "GROUP",
            showOnlineDot = chat.type == "DIRECT",
            onClick = onAvatarClick
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Center: Title & Snippet
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (chat.type == "GROUP") {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = NexusViolet,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = chat.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isOwnerChat) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NexusGold)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "👑 Creador",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Entregado",
                        tint = CheckmarkBlue,
                        modifier = Modifier
                            .size(15.dp)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = chat.lastMessageSnippet.ifBlank { "Toca para abrir la conversación" },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chat.pinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Fijado",
                            tint = CyberNeonCyan,
                            modifier = Modifier
                                .size(15.dp)
                                .padding(end = 6.dp)
                        )
                    }

                    if (chat.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(NexusPrimary)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
