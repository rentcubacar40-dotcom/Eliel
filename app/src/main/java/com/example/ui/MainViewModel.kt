package com.example.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatEntity
import com.example.data.local.GroupMemberWithUser
import com.example.data.local.MessageEntity
import com.example.data.local.MoodleConfigEntity
import com.example.data.local.UserEntity
import com.example.data.moodle.UcfMoodleClient
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream

sealed class Screen {
    object Onboarding : Screen()
    object ChatList : Screen()
    data class ChatDetail(val chatId: String) : Screen()
}

data class UploadProgressState(
    val isUploading: Boolean = false,
    val fileName: String = "",
    val bytesSent: Long = 0L,
    val totalBytes: Long = 0L,
    val percentage: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = ChatRepository(database)

    // Navigation State
    private val _currentScreen = MutableStateFlow<Screen>(Screen.ChatList)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Active Chat Selection
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    // Upload Progress
    private val _uploadProgress = MutableStateFlow(UploadProgressState())
    val uploadProgress: StateFlow<UploadProgressState> = _uploadProgress.asStateFlow()

    // UI Snackbars / Alerts
    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    // Search query in chat list
    val searchQuery = MutableStateFlow("")

    // Tab filter: 0 = Todos, 1 = Privados, 2 = Grupos
    val selectedTab = MutableStateFlow(0)

    // Current user
    val currentUser: StateFlow<UserEntity?> = repository.currentUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Moodle Config
    val moodleConfig: StateFlow<MoodleConfigEntity?> = repository.moodleConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Filtered chats list
    val filteredChats: StateFlow<List<ChatEntity>> = combine(
        repository.allChatsFlow,
        searchQuery,
        selectedTab
    ) { chats, query, tab ->
        chats.filter { chat ->
            val matchesQuery = query.isBlank() ||
                    chat.title.contains(query, ignoreCase = true) ||
                    chat.lastMessageSnippet.contains(query, ignoreCase = true)
            val matchesTab = when (tab) {
                1 -> chat.type == "DIRECT"
                2 -> chat.type == "GROUP"
                else -> true
            }
            matchesQuery && matchesTab
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Statuses (24h ephemeral updates)
    val activeStatuses: StateFlow<List<com.example.data.local.StatusEntity>> = repository.getActiveStatusesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Chat Entity
    private val _activeChat = MutableStateFlow<ChatEntity?>(null)
    val activeChat: StateFlow<ChatEntity?> = _activeChat.asStateFlow()

    // Active Messages
    private val _activeMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val activeMessages: StateFlow<List<MessageEntity>> = _activeMessages.asStateFlow()

    // Active Group Members
    private val _activeGroupMembers = MutableStateFlow<List<GroupMemberWithUser>>(emptyList())
    val activeGroupMembers: StateFlow<List<GroupMemberWithUser>> = _activeGroupMembers.asStateFlow()

    // Inspecting User Profile (Interactive Sheet/Dialog)
    private val _inspectingUser = MutableStateFlow<UserEntity?>(null)
    val inspectingUser: StateFlow<UserEntity?> = _inspectingUser.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()
            // Check if current user is already registered
            val user = repository.currentUserFlow.firstOrNull()
            if (user == null || !user.isCurrentUser) {
                _currentScreen.value = Screen.Onboarding
            } else {
                _currentScreen.value = Screen.ChatList
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen is Screen.ChatDetail) {
            _activeChatId.value = screen.chatId
            observeChat(screen.chatId)
        }
    }

    fun navigateBack() {
        val current = _currentScreen.value
        if (current is Screen.ChatDetail) {
            _currentScreen.value = Screen.ChatList
            _activeChatId.value = null
            _activeGroupMembers.value = emptyList()
        }
    }

    private fun observeChat(chatId: String) {
        viewModelScope.launch {
            repository.getChatByIdFlow(chatId).collect {
                _activeChat.value = it
            }
        }
        viewModelScope.launch {
            repository.getMessagesFlow(chatId).collect {
                _activeMessages.value = it
            }
        }
        viewModelScope.launch {
            repository.getGroupMembersFlow(chatId).collect {
                _activeGroupMembers.value = it
            }
        }
    }

    fun inspectUser(username: String) {
        viewModelScope.launch {
            val user = repository.getUserByUsername(username)
            if (user != null) {
                _inspectingUser.value = user
            } else {
                _snackbarEvent.emit("Usuario @$username no encontrado")
            }
        }
    }

    fun inspectUserEntity(user: UserEntity) {
        _inspectingUser.value = user
    }

    fun closeUserInspection() {
        _inspectingUser.value = null
    }

    fun loginUser(
        username: String,
        pass: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.loginUser(username, pass)
            if (res.isSuccess) {
                _currentScreen.value = Screen.ChatList
                val u = res.getOrNull()!!
                val welcome = if (u.role == "OWNER") "¡Bienvenido de nuevo, Administrador @${u.username}!" else "¡Bienvenido a Nexus, @${u.username}!"
                _snackbarEvent.emit(welcome)
                onComplete(true, welcome)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Error al iniciar sesión"
                _snackbarEvent.emit(err)
                onComplete(false, err)
            }
        }
    }

    fun registerUser(
        username: String,
        displayName: String,
        bio: String,
        avatarUrl: String,
        password: String = "",
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.registerInitialUser(
                username = username,
                displayName = displayName,
                bio = bio,
                avatarUrl = avatarUrl,
                password = password
            )
            if (res.isSuccess) {
                _currentScreen.value = Screen.ChatList
                _snackbarEvent.emit("¡Bienvenido a Nexus, @$username!")
                onComplete(true, "Cuenta creada con éxito")
            } else {
                val err = res.exceptionOrNull()?.message ?: "Error al crear cuenta"
                _snackbarEvent.emit(err)
                onComplete(false, err)
            }
        }
    }

    fun sendMessage(text: String) {
        val chatId = _activeChatId.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            val res = repository.sendMessage(chatId = chatId, text = text)
            if (res.isFailure) {
                _snackbarEvent.emit("Error: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun sendAttachment(uri: Uri, isImage: Boolean) {
        val chatId = _activeChatId.value ?: return
        val context = getApplication<Application>()
        viewModelScope.launch {
            try {
                var fileName = "adjunto_${System.currentTimeMillis()}"
                var fileSize = 0L
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    }
                }

                // Check 4MB limit before reading
                if (fileSize > UcfMoodleClient.MAX_FILE_SIZE_BYTES) {
                    val sizeMb = String.format("%.2f", fileSize / (1024.0 * 1024.0))
                    _snackbarEvent.emit("El archivo pesa $sizeMb MB. El límite máximo en cursos.ucf.edu.cu es de 4.0 MB.")
                    return@launch
                }

                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.use { it.readBytes() } ?: return@launch

                if (bytes.size > UcfMoodleClient.MAX_FILE_SIZE_BYTES) {
                    val sizeMb = String.format("%.2f", bytes.size / (1024.0 * 1024.0))
                    _snackbarEvent.emit("El archivo supera el límite de 4.0 MB ($sizeMb MB).")
                    return@launch
                }

                val mimeType = context.contentResolver.getType(uri) ?: if (isImage) "image/jpeg" else "application/octet-stream"
                val attachmentType = if (isImage || mimeType.startsWith("image")) "IMAGE" else "DOCUMENT"

                _uploadProgress.value = UploadProgressState(
                    isUploading = true,
                    fileName = fileName,
                    bytesSent = 0,
                    totalBytes = bytes.size.toLong(),
                    percentage = 0
                )

                val result = repository.sendMessage(
                    chatId = chatId,
                    text = "",
                    attachmentBytes = bytes,
                    attachmentName = fileName,
                    attachmentType = attachmentType,
                    mimeType = mimeType,
                    onUploadProgress = { sent, total ->
                        val pct = if (total > 0) ((sent * 100) / total).toInt() else 0
                        _uploadProgress.value = UploadProgressState(
                            isUploading = true,
                            fileName = fileName,
                            bytesSent = sent,
                            totalBytes = total,
                            percentage = pct
                        )
                    }
                )

                _uploadProgress.value = UploadProgressState(isUploading = false)

                if (result.isSuccess) {
                    _snackbarEvent.emit("✓ Archivo subido y enviado con éxito")
                } else {
                    _snackbarEvent.emit("Error: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _uploadProgress.value = UploadProgressState(isUploading = false)
                _snackbarEvent.emit("Error al procesar archivo: ${e.message}")
            }
        }
    }

    fun isAppOwner(username: String?): Boolean {
        if (username == null) return false
        val clean = username.trim().removePrefix("@")
        return clean.equals("Eliel_21", ignoreCase = true)
    }

    suspend fun checkUsernameAvailability(username: String): Boolean {
        val current = currentUser.value?.username
        return repository.checkUsernameAvailability(username, current)
    }

    fun updateProfileWithUsername(
        newUsername: String,
        displayName: String,
        bio: String,
        avatarUrl: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updateCurrentUserProfile(newUsername, displayName, bio, avatarUrl)
            if (result.isSuccess) {
                _snackbarEvent.emit("Perfil actualizado correctamente")
                onComplete(true, "Perfil actualizado")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Error al actualizar perfil"
                _snackbarEvent.emit(err)
                onComplete(false, err)
            }
        }
    }

    suspend fun checkUserExists(username: String): UserEntity? {
        return repository.getUserByUsername(username)
    }

    suspend fun getAllDirectoryUsers(): List<UserEntity> {
        return repository.getAllDirectoryUsers()
    }

    fun createGroup(title: String, description: String, initialMembers: List<String> = emptyList()) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val res = repository.createGroup(title, description, initialMembers)
            if (res.isSuccess) {
                val group = res.getOrNull()!!
                _snackbarEvent.emit("Grupo \"${group.title}\" creado con éxito")
                navigateTo(Screen.ChatDetail(group.id))
            } else {
                _snackbarEvent.emit("Error: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun createDirectChat(username: String, onResult: ((Boolean, String) -> Unit)? = null) {
        if (username.isBlank()) return
        viewModelScope.launch {
            val res = repository.createDirectChat(username)
            if (res.isSuccess) {
                val chat = res.getOrNull()!!
                _snackbarEvent.emit("Chat con @${username.removePrefix("@")} iniciado")
                navigateTo(Screen.ChatDetail(chat.id))
                onResult?.invoke(true, chat.id)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Usuario no encontrado"
                _snackbarEvent.emit(err)
                onResult?.invoke(false, err)
            }
        }
    }

    fun addMemberToGroup(chatId: String, username: String) {
        viewModelScope.launch {
            val res = repository.addMemberToGroup(chatId, username)
            if (res.isSuccess) {
                _snackbarEvent.emit("@$username añadido al grupo")
            } else {
                _snackbarEvent.emit("Error: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun removeMemberFromGroup(chatId: String, username: String) {
        viewModelScope.launch {
            val res = repository.removeMemberFromGroup(chatId, username)
            if (res.isSuccess) {
                _snackbarEvent.emit("@$username fue expulsado del grupo")
            } else {
                _snackbarEvent.emit("Error: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun updateMemberRole(chatId: String, username: String, newRole: String) {
        viewModelScope.launch {
            val res = repository.updateMemberRole(chatId, username, newRole)
            if (res.isSuccess) {
                val roleName = when (newRole) {
                    "ADMIN" -> "Administrador"
                    "OWNER" -> "Propietario"
                    else -> "Miembro"
                }
                _snackbarEvent.emit("Rol de @$username cambiado a $roleName")
            } else {
                _snackbarEvent.emit("Error: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun openDirectChatWithInspectedUser() {
        val user = _inspectingUser.value ?: return
        closeUserInspection()
        createDirectChat(user.username)
    }

    fun updateProfile(displayName: String, bio: String, avatarUrl: String) {
        viewModelScope.launch {
            repository.updateCurrentUser(displayName, bio, avatarUrl)
            _snackbarEvent.emit("Perfil actualizado")
        }
    }

    fun testMoodleLogin(username: String, pass: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.testMoodleLogin(username, pass)
            if (res.isSuccess) {
                val msg = res.getOrNull() ?: "Conectado"
                _snackbarEvent.emit(msg)
                onComplete(true, msg)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Fallo de conexión"
                _snackbarEvent.emit(err)
                onComplete(false, err)
            }
        }
    }

    fun saveMoodleConfig(config: MoodleConfigEntity) {
        viewModelScope.launch {
            repository.saveMoodleConfig(config)
            _snackbarEvent.emit("Configuración de cursos.ucf.edu.cu guardada")
        }
    }

    fun syncChatToMoodleCloud(chatId: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.syncChatToMoodle(chatId)
            if (res.isSuccess) {
                val url = res.getOrNull() ?: ""
                _snackbarEvent.emit("Chat respaldado en JSON en Moodle UCF: $url")
                onResult(url)
            } else {
                _snackbarEvent.emit("Error al sincronizar: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun importChatFromJson(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.importChatJson(jsonString)
            if (res.isSuccess) {
                val chat = res.getOrNull()!!
                _snackbarEvent.emit("Chat \"${chat.title}\" importado desde JSON")
                onResult(true, chat.id)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Error al procesar JSON"
                _snackbarEvent.emit(err)
                onResult(false, err)
            }
        }
    }

    fun downloadChatFromMoodleUrl(url: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.syncChatFromMoodleUrl(url)
            if (res.isSuccess) {
                val chat = res.getOrNull()!!
                _snackbarEvent.emit("Chat descargado e importado de UCF Moodle")
                onResult(true, chat.id)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Error al descargar de UCF"
                _snackbarEvent.emit(err)
                onResult(false, err)
            }
        }
    }

    suspend fun getExportJson(chatId: String): String {
        return repository.exportChatJson(chatId)
    }

    // --- Message and Storage Operations ---

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            val res = repository.deleteMessage(messageId)
            if (res.isSuccess) {
                _snackbarEvent.emit("Mensaje eliminado localmente")
            } else {
                _snackbarEvent.emit("Error al eliminar mensaje: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun deleteAttachment(messageId: String) {
        viewModelScope.launch {
            val res = repository.deleteAttachment(messageId)
            if (res.isSuccess) {
                _snackbarEvent.emit("Archivo adjunto borrado para liberar espacio")
            } else {
                _snackbarEvent.emit("Error al borrar adjunto: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun clearChat(chatId: String) {
        viewModelScope.launch {
            val res = repository.clearChatHistory(chatId)
            if (res.isSuccess) {
                _snackbarEvent.emit("Historial del chat vaciado (almacenamiento liberado)")
            } else {
                _snackbarEvent.emit("Error al vaciar chat: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    // --- Stories / Statuses ---

    fun publishStatus(
        text: String,
        mediaUrl: String? = null,
        backgroundColorHex: String = "#0284C7",
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.publishStatus(text, mediaUrl, backgroundColorHex)
            if (res.isSuccess) {
                _snackbarEvent.emit("¡Estado publicado por 24 horas!")
                onComplete(true)
            } else {
                _snackbarEvent.emit("Error al publicar estado: ${res.exceptionOrNull()?.message}")
                onComplete(false)
            }
        }
    }

    fun deleteStatus(statusId: String) {
        viewModelScope.launch {
            repository.deleteStatus(statusId)
            _snackbarEvent.emit("Estado eliminado")
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _currentScreen.value = Screen.Onboarding
            _activeChatId.value = null
            _snackbarEvent.emit("Has cerrado sesión.")
        }
    }
}
