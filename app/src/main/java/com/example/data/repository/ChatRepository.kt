package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.ChatEntity
import com.example.data.local.GroupMemberEntity
import com.example.data.local.GroupMemberWithUser
import com.example.data.local.MessageEntity
import com.example.data.local.MoodleConfigEntity
import com.example.data.local.UserEntity
import com.example.data.moodle.ChatSyncBundleJson
import com.example.data.moodle.MessagePayloadJson
import com.example.data.moodle.UcfMoodleClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

class ChatRepository(
    private val database: AppDatabase,
    val moodleClient: UcfMoodleClient = UcfMoodleClient()
) {
    private val userDao = database.userDao()
    private val chatDao = database.chatDao()
    private val messageDao = database.messageDao()
    private val moodleConfigDao = database.moodleConfigDao()
    private val groupMemberDao = database.groupMemberDao()
    private val statusDao = database.statusDao()

    companion object {
        const val GENERAL_GROUP_ID = "group_nexus_general"
        const val ADMIN_USERNAME = "Eliel_21"
        const val ADMIN_PASSWORD = "ElielElielAdmin543345.."
    }

    val currentUserFlow: Flow<UserEntity?> = userDao.getCurrentUserFlow()
    val allChatsFlow: Flow<List<ChatEntity>> = chatDao.getAllChatsFlow()
    val moodleConfigFlow: Flow<MoodleConfigEntity?> = moodleConfigDao.getConfigFlow()

    fun getMessagesFlow(chatId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForChatFlow(chatId)

    fun getChatByIdFlow(chatId: String): Flow<ChatEntity?> =
        chatDao.getChatByIdFlow(chatId)

    suspend fun initializeDefaultsIfNeeded() = withContext(Dispatchers.IO) {
        // 1. Initialize Moodle internal configuration
        val existingConfig = moodleConfigDao.getConfig()
        if (existingConfig == null) {
            val defaultConfig = MoodleConfigEntity(
                id = 1,
                host = "https://cursos.ucf.edu.cu/",
                username = "",
                password = "",
                repoId = 4,
                uploadType = "evidence",
                maxChunkBytes = 4 * 1024 * 1024L,
                lastConnectionStatus = "Listo para usar"
            )
            moodleConfigDao.saveConfig(defaultConfig)
            moodleClient.configure(defaultConfig.host, defaultConfig.repoId)
        } else {
            moodleClient.configure(existingConfig.host, existingConfig.repoId)
            if (existingConfig.username.isNotBlank() && existingConfig.password.isNotBlank()) {
                moodleClient.login(existingConfig.username, existingConfig.password)
            }
        }

        // 2. Register Application Creator & Owner (@Eliel_21)
        val existingEliel = userDao.getUserByUsername(ADMIN_USERNAME)
        if (existingEliel == null) {
            val elielAdmin = UserEntity(
                username = ADMIN_USERNAME,
                displayName = "Eliel",
                bio = "👑 Creador y Propietario de Nexus UCF",
                avatarUrl = "",
                isCurrentUser = false,
                password = ADMIN_PASSWORD,
                role = "OWNER"
            )
            userDao.insertOrUpdate(elielAdmin)
        } else {
            userDao.insertOrUpdate(
                existingEliel.copy(password = ADMIN_PASSWORD, role = "OWNER")
            )
        }

        // 3. Register Official Support user (@soporte_ucf)
        val existingSupport = userDao.getUserByUsername("soporte_ucf")
        if (existingSupport == null) {
            val supportUser = UserEntity(
                username = "soporte_ucf",
                displayName = "Soporte Técnico Nexus",
                bio = "🛡️ Cuenta oficial de asistencia y soporte técnico para la red universitaria UCF.",
                avatarUrl = "",
                isCurrentUser = false
            )
            userDao.insertOrUpdate(supportUser)
        }

        // 4. Create default public group for everyone: "Nexus General • Comunidad UCF"
        val existingGeneralGroup = chatDao.getChatById(GENERAL_GROUP_ID)
        if (existingGeneralGroup == null) {
            val generalGroup = ChatEntity(
                id = GENERAL_GROUP_ID,
                title = "Nexus General • Comunidad UCF",
                type = "GROUP",
                avatarUrl = "",
                description = "Comunidad oficial pública para todos los miembros de Nexus UCF. Comparte ideas, archivos, debates y colabora libremente.",
                unreadCount = 0,
                lastMessageSnippet = "¡Bienvenidos a la comunidad oficial de Nexus UCF!",
                lastMessageTime = System.currentTimeMillis(),
                pinned = true
            )
            chatDao.insertOrUpdate(generalGroup)

            // Seed Owner and Admin
            groupMemberDao.insertOrUpdate(
                GroupMemberEntity(chatId = GENERAL_GROUP_ID, username = "Eliel_21", role = "OWNER")
            )
            groupMemberDao.insertOrUpdate(
                GroupMemberEntity(chatId = GENERAL_GROUP_ID, username = "soporte_ucf", role = "ADMIN")
            )
        }

        // Ensure current user is in the General group if logged in
        val current = userDao.getCurrentUser()
        if (current != null) {
            val isMember = groupMemberDao.getMember(GENERAL_GROUP_ID, current.username)
            if (isMember == null) {
                groupMemberDao.insertOrUpdate(
                    GroupMemberEntity(
                        chatId = GENERAL_GROUP_ID,
                        username = current.username,
                        role = if (current.username.equals("Eliel_21", ignoreCase = true)) "OWNER" else "MEMBER"
                    )
                )
            }
        }
    }

    suspend fun loginUser(username: String, pass: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val clean = username.trim().removePrefix("@")
        if (clean.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Por favor ingresa tu nombre de usuario."))
        }

        val isAdmin = clean.equals(ADMIN_USERNAME, ignoreCase = true)
        if (isAdmin) {
            if (pass != ADMIN_PASSWORD) {
                return@withContext Result.failure(IllegalArgumentException("Contraseña de administrador incorrecta."))
            }
            userDao.clearCurrentUserFlag()
            val existing = userDao.getUserByUsername(ADMIN_USERNAME) ?: UserEntity(
                username = ADMIN_USERNAME,
                displayName = "Eliel",
                bio = "👑 Creador y Propietario de Nexus UCF",
                avatarUrl = "",
                isCurrentUser = true,
                password = ADMIN_PASSWORD,
                role = "OWNER"
            )
            val updated = existing.copy(
                isCurrentUser = true,
                password = ADMIN_PASSWORD,
                role = "OWNER",
                lastSeen = System.currentTimeMillis()
            )
            userDao.insertOrUpdate(updated)
            groupMemberDao.insertOrUpdate(
                GroupMemberEntity(chatId = GENERAL_GROUP_ID, username = ADMIN_USERNAME, role = "OWNER")
            )
            return@withContext Result.success(updated)
        }

        val user = userDao.getUserByUsername(clean)
        if (user == null) {
            return@withContext Result.failure(IllegalArgumentException("El usuario @$clean no existe. Ve a la pestaña 'Crear Cuenta' para registrarte."))
        }
        if (user.password.isNotBlank() && user.password != pass) {
            return@withContext Result.failure(IllegalArgumentException("Contraseña incorrecta."))
        }

        userDao.clearCurrentUserFlag()
        val loggedInUser = user.copy(isCurrentUser = true, lastSeen = System.currentTimeMillis())
        userDao.insertOrUpdate(loggedInUser)

        groupMemberDao.insertOrUpdate(
            GroupMemberEntity(
                chatId = GENERAL_GROUP_ID,
                username = clean,
                role = loggedInUser.role
            )
        )
        Result.success(loggedInUser)
    }

    suspend fun registerInitialUser(
        username: String,
        displayName: String,
        bio: String,
        avatarUrl: String,
        password: String = ""
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanUsername = username.trim().removePrefix("@")
        if (cleanUsername.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("El nombre de usuario no puede estar vacío."))
        }
        if (cleanUsername.length < 3) {
            return@withContext Result.failure(IllegalArgumentException("El nombre de usuario debe tener al menos 3 caracteres."))
        }
        val validPattern = Regex("^[a-zA-Z0-9_]+$")
        if (!validPattern.matches(cleanUsername)) {
            return@withContext Result.failure(IllegalArgumentException("El usuario solo puede contener letras, números y guión bajo (_)."))
        }

        val isAdmin = cleanUsername.equals(ADMIN_USERNAME, ignoreCase = true)
        if (isAdmin && password != ADMIN_PASSWORD) {
            return@withContext Result.failure(IllegalArgumentException("Para acceder como administrador (@$ADMIN_USERNAME) debes ingresar la clave maestra de admin."))
        }

        val existingUser = userDao.getUserByUsername(cleanUsername)
        if (existingUser != null && !existingUser.isCurrentUser && !isAdmin) {
            return@withContext Result.failure(IllegalArgumentException("El nombre de usuario @$cleanUsername ya está en uso en la red."))
        }

        // Clear any previous current user flag
        userDao.clearCurrentUserFlag()

        val newUser = UserEntity(
            username = cleanUsername,
            displayName = displayName.trim().ifBlank { cleanUsername },
            bio = bio.trim(),
            avatarUrl = avatarUrl.trim(),
            isCurrentUser = true,
            lastSeen = System.currentTimeMillis(),
            password = if (isAdmin) ADMIN_PASSWORD else password.trim(),
            role = if (isAdmin) "OWNER" else "MEMBER"
        )
        userDao.insertOrUpdate(newUser)

        // Automatically join default General Community group
        groupMemberDao.insertOrUpdate(
            GroupMemberEntity(
                chatId = GENERAL_GROUP_ID,
                username = cleanUsername,
                role = if (isAdmin) "OWNER" else "MEMBER"
            )
        )

        Result.success(newUser)
    }

    suspend fun checkUsernameAvailability(username: String, currentUsername: String?): Boolean =
        withContext(Dispatchers.IO) {
            val clean = username.trim().removePrefix("@")
            if (clean.isBlank()) return@withContext false
            val currentClean = currentUsername?.trim()?.removePrefix("@")
            if (currentClean != null && clean.equals(currentClean, ignoreCase = true)) {
                return@withContext true
            }
            if (clean.equals("Eliel_21", ignoreCase = true)) {
                return@withContext true
            }
            val user = userDao.getUserByUsername(clean)
            user == null
        }

    suspend fun updateCurrentUserProfile(
        newUsername: String,
        displayName: String,
        bio: String,
        avatarUrl: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanUsername = newUsername.trim().removePrefix("@")
        val currentUser = userDao.getCurrentUser()
            ?: return@withContext Result.failure(IllegalStateException("No hay sesión de usuario activa."))

        if (cleanUsername.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("El nombre de usuario no puede estar vacío."))
        }

        // Validate uniqueness if changing username
        if (!cleanUsername.equals(currentUser.username, ignoreCase = true)) {
            val existing = userDao.getUserByUsername(cleanUsername)
            if (existing != null && !existing.isCurrentUser && !cleanUsername.equals("Eliel_21", ignoreCase = true)) {
                return@withContext Result.failure(
                    IllegalArgumentException("El nombre de usuario @$cleanUsername ya está en uso.")
                )
            }
            userDao.deleteByUsername(currentUser.username)
        }

        val updated = currentUser.copy(
            username = cleanUsername,
            displayName = displayName.ifBlank { cleanUsername },
            bio = bio,
            avatarUrl = avatarUrl,
            isCurrentUser = true
        )
        userDao.insertOrUpdate(updated)
        Result.success(updated)
    }

    suspend fun createDirectChat(targetUsername: String): Result<ChatEntity> =
        withContext(Dispatchers.IO) {
            val clean = targetUsername.trim().removePrefix("@")
            if (clean.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Ingresa un nombre de usuario."))
            }

            val currentUser = userDao.getCurrentUser()
            if (currentUser != null && clean.equals(currentUser.username, ignoreCase = true)) {
                return@withContext Result.failure(IllegalArgumentException("No puedes iniciar una conversación contigo mismo."))
            }

            // Real validation: verify if user exists in the directory!
            val targetUser = userDao.getUserByUsername(clean)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("El usuario @$clean no existe en la red Nexus. Verifica el usuario o pídele que se registre.")
                )

            val chatId = "direct_${clean}"
            val existingChat = chatDao.getChatById(chatId)
            if (existingChat != null) {
                return@withContext Result.success(existingChat)
            }

            val chat = ChatEntity(
                id = chatId,
                title = targetUser.displayName,
                type = "DIRECT",
                avatarUrl = targetUser.avatarUrl,
                description = targetUser.bio,
                unreadCount = 0,
                lastMessageSnippet = "Conversación iniciada con @$clean",
                lastMessageTime = System.currentTimeMillis(),
                pinned = false
            )
            chatDao.insertOrUpdate(chat)
            Result.success(chat)
        }

    suspend fun createGroup(
        title: String,
        description: String = "",
        initialMemberUsernames: List<String> = emptyList()
    ): Result<ChatEntity> = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("El nombre del grupo no puede estar vacío."))
        }
        val currentUser = userDao.getCurrentUser()
            ?: return@withContext Result.failure(IllegalStateException("No hay sesión de usuario activa."))

        val groupId = "group_${UUID.randomUUID().toString().take(8)}"
        val group = ChatEntity(
            id = groupId,
            title = cleanTitle,
            type = "GROUP",
            avatarUrl = "",
            description = description.trim(),
            unreadCount = 0,
            lastMessageSnippet = "Grupo creado por @${currentUser.username}",
            lastMessageTime = System.currentTimeMillis(),
            pinned = false
        )
        chatDao.insertOrUpdate(group)

        // Creator is OWNER
        groupMemberDao.insertOrUpdate(
            GroupMemberEntity(
                chatId = groupId,
                username = currentUser.username,
                role = "OWNER"
            )
        )

        // Add initial members if selected
        initialMemberUsernames.forEach { rawMember ->
            val cleanMember = rawMember.trim().removePrefix("@")
            if (cleanMember.isNotBlank() && !cleanMember.equals(currentUser.username, ignoreCase = true)) {
                val memberUser = userDao.getUserByUsername(cleanMember)
                if (memberUser != null) {
                    groupMemberDao.insertOrUpdate(
                        GroupMemberEntity(
                            chatId = groupId,
                            username = cleanMember,
                            role = "MEMBER"
                        )
                    )
                }
            }
        }

        Result.success(group)
    }

    fun getGroupMembersFlow(chatId: String): Flow<List<GroupMemberWithUser>> {
        return groupMemberDao.getMembersForChatFlow(chatId).map { members ->
            members.map { member ->
                GroupMemberWithUser(
                    member = member,
                    user = userDao.getUserByUsername(member.username)
                )
            }
        }
    }

    suspend fun addMemberToGroup(chatId: String, username: String, role: String = "MEMBER"): Result<Unit> =
        withContext(Dispatchers.IO) {
            val clean = username.trim().removePrefix("@")
            val user = userDao.getUserByUsername(clean)
                ?: return@withContext Result.failure(IllegalArgumentException("El usuario @$clean no existe en la red."))

            val existing = groupMemberDao.getMember(chatId, clean)
            if (existing != null) {
                return@withContext Result.failure(IllegalArgumentException("@$clean ya forma parte de este grupo."))
            }

            groupMemberDao.insertOrUpdate(
                GroupMemberEntity(
                    chatId = chatId,
                    username = clean,
                    role = role
                )
            )
            Result.success(Unit)
        }

    suspend fun removeMemberFromGroup(chatId: String, username: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val clean = username.trim().removePrefix("@")
            val member = groupMemberDao.getMember(chatId, clean)
                ?: return@withContext Result.failure(IllegalArgumentException("Miembro no encontrado."))

            if (member.role == "OWNER") {
                return@withContext Result.failure(IllegalArgumentException("No se puede expulsar al propietario del grupo."))
            }

            groupMemberDao.removeMember(chatId, clean)
            Result.success(Unit)
        }

    suspend fun updateMemberRole(chatId: String, username: String, newRole: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val clean = username.trim().removePrefix("@")
            groupMemberDao.updateMemberRole(chatId, clean, newRole)
            Result.success(Unit)
        }

    suspend fun getAllDirectoryUsers(): List<UserEntity> = withContext(Dispatchers.IO) {
        val current = userDao.getCurrentUser()?.username ?: ""
        userDao.getAllUsersFlow().firstOrNull()?.filter { it.username != current } ?: emptyList()
    }

    suspend fun getUserByUsername(username: String): UserEntity? = withContext(Dispatchers.IO) {
        val clean = username.trim().removePrefix("@")
        userDao.getUserByUsername(clean)
    }

    suspend fun updateCurrentUser(displayName: String, bio: String, avatarUrl: String) =
        withContext(Dispatchers.IO) {
            val current = userDao.getCurrentUser() ?: return@withContext
            val updated = current.copy(
                displayName = displayName,
                bio = bio,
                avatarUrl = avatarUrl
            )
            userDao.insertOrUpdate(updated)
        }

    suspend fun sendMessage(
        chatId: String,
        text: String,
        attachmentBytes: ByteArray? = null,
        attachmentName: String? = null,
        attachmentType: String? = null,
        mimeType: String = "application/octet-stream",
        onUploadProgress: ((Long, Long) -> Unit)? = null
    ): Result<MessageEntity> = withContext(Dispatchers.IO) {
        val currentUser = userDao.getCurrentUser() ?: UserEntity(
            username = "usuario",
            displayName = "Usuario"
        )

        val messageId = "msg_${UUID.randomUUID()}"
        var uploadedUrl: String? = null
        val attachmentSize = attachmentBytes?.size?.toLong() ?: 0L

        // Upload attachment if present under 4MB limit
        if (attachmentBytes != null && !attachmentName.isNullOrEmpty()) {
            if (attachmentSize > UcfMoodleClient.MAX_FILE_SIZE_BYTES) {
                val sizeMb = String.format("%.2f", attachmentSize / (1024.0 * 1024.0))
                return@withContext Result.failure(
                    IllegalArgumentException("El archivo pesa $sizeMb MB. cursos.ucf.edu.cu tiene límite estricto de 4.0 MB.")
                )
            }

            val uploadResult = moodleClient.uploadFile(
                fileBytes = attachmentBytes,
                fileName = attachmentName,
                mimeType = mimeType,
                onProgress = { sent, total ->
                    onUploadProgress?.invoke(sent, total)
                }
            )

            if (uploadResult.isFailure) {
                return@withContext Result.failure(
                    uploadResult.exceptionOrNull() ?: Exception("Fallo al subir a UCF Moodle")
                )
            }

            uploadedUrl = uploadResult.getOrNull()?.url
        }

        val message = MessageEntity(
            id = messageId,
            chatId = chatId,
            senderId = currentUser.username,
            senderName = currentUser.displayName,
            senderAvatar = currentUser.avatarUrl,
            timestamp = System.currentTimeMillis(),
            text = text,
            attachmentUrl = uploadedUrl,
            attachmentName = attachmentName,
            attachmentType = attachmentType,
            attachmentSize = attachmentSize,
            isOutgoing = true,
            status = "DELIVERED"
        )

        messageDao.insertOrUpdate(message)

        val snippet = when {
            text.isNotBlank() -> text
            attachmentType == "IMAGE" -> "📷 Imagen"
            attachmentType == "DOCUMENT" -> "📄 Archivo (${attachmentName ?: "adjunto"})"
            else -> "Adjunto"
        }
        chatDao.updateLastMessage(chatId, snippet, message.timestamp)

        Result.success(message)
    }

    suspend fun saveMoodleConfig(config: MoodleConfigEntity): Unit = withContext(Dispatchers.IO) {
        moodleConfigDao.saveConfig(config)
        moodleClient.configure(config.host, config.repoId)
    }

    suspend fun testMoodleLogin(username: String, pass: String): Result<String> =
        withContext(Dispatchers.IO) {
            val ok = moodleClient.login(username, pass)
            val current = moodleConfigDao.getConfig() ?: MoodleConfigEntity()
            val status = if (ok) "Conectado" else "Error al conectar"
            moodleConfigDao.saveConfig(
                current.copy(
                    username = username,
                    password = pass,
                    lastConnectionStatus = status,
                    lastConnectionTime = System.currentTimeMillis()
                )
            )
            if (ok) Result.success("Conectado con éxito a cursos.ucf.edu.cu") else Result.failure(Exception("Error de autenticación o servidor inalcanzable"))
        }

    /**
     * Exports a chat bundle as JSON string.
     */
    suspend fun exportChatJson(chatId: String): String = withContext(Dispatchers.IO) {
        val chat = chatDao.getChatById(chatId) ?: return@withContext "{}"
        val messages = messageDao.getRecentMessages(chatId, 200).reversed()
        val jsonBundle = ChatSyncBundleJson(
            chatId = chat.id,
            title = chat.title,
            type = chat.type,
            avatarUrl = chat.avatarUrl,
            description = chat.description,
            updatedAt = System.currentTimeMillis(),
            messages = messages.map {
                MessagePayloadJson(
                    id = it.id,
                    chatId = it.chatId,
                    senderId = it.senderId,
                    senderName = it.senderName,
                    senderAvatar = it.senderAvatar,
                    timestamp = it.timestamp,
                    text = it.text,
                    attachmentUrl = it.attachmentUrl,
                    attachmentName = it.attachmentName,
                    attachmentType = it.attachmentType,
                    attachmentSize = it.attachmentSize
                )
            }
        )
        jsonBundle.toJson().toString(2)
    }

    /**
     * Imports a chat bundle from JSON string.
     */
    suspend fun importChatJson(jsonString: String): Result<ChatEntity> = withContext(Dispatchers.IO) {
        try {
            val jsonObject = JSONObject(jsonString)
            val bundle = ChatSyncBundleJson.fromJson(jsonObject)
            if (bundle.chatId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("JSON inválido: no contiene chatId."))
            }

            val chat = ChatEntity(
                id = bundle.chatId,
                title = bundle.title,
                type = bundle.type,
                avatarUrl = bundle.avatarUrl,
                description = bundle.description,
                unreadCount = 0,
                lastMessageSnippet = bundle.messages.lastOrNull()?.text ?: "Chat importado",
                lastMessageTime = bundle.updatedAt,
                pinned = false
            )
            chatDao.insertOrUpdate(chat)

            val currentUserId = userDao.getCurrentUser()?.username ?: ""
            val messageEntities = bundle.messages.map {
                MessageEntity(
                    id = it.id.ifBlank { "msg_${UUID.randomUUID()}" },
                    chatId = bundle.chatId,
                    senderId = it.senderId,
                    senderName = it.senderName,
                    senderAvatar = it.senderAvatar,
                    timestamp = it.timestamp,
                    text = it.text,
                    attachmentUrl = it.attachmentUrl,
                    attachmentName = it.attachmentName,
                    attachmentType = it.attachmentType,
                    attachmentSize = it.attachmentSize,
                    isOutgoing = it.senderId == currentUserId,
                    status = "DELIVERED"
                )
            }
            messageDao.insertAll(messageEntities)
            Result.success(chat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads the chat JSON directly to UCF Moodle.
     */
    suspend fun syncChatToMoodle(chatId: String): Result<String> = withContext(Dispatchers.IO) {
        val jsonStr = exportChatJson(chatId)
        val fileName = "teleucf_${chatId}_sync.json"
        moodleClient.uploadJsonSync(fileName, jsonStr)
    }

    /**
     * Downloads and imports chat JSON directly from a UCF Moodle URL.
     */
    suspend fun syncChatFromMoodleUrl(url: String): Result<ChatEntity> = withContext(Dispatchers.IO) {
        val downloadRes = moodleClient.fetchJsonFromUrl(url)
        if (downloadRes.isFailure) {
            return@withContext Result.failure(
                downloadRes.exceptionOrNull() ?: Exception("No se pudo descargar el archivo JSON de Moodle")
            )
        }
        val jsonStr = downloadRes.getOrNull() ?: ""
        importChatJson(jsonStr)
    }

    // --- Message Deletion & Storage Management (Telegram-Style) ---

    suspend fun deleteMessage(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            messageDao.deleteMessageById(messageId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAttachment(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            messageDao.clearAttachment(messageId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearChatHistory(chatId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            messageDao.deleteMessagesForChat(chatId)
            chatDao.updateLastMessage(chatId, "Historial vaciado", System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Status / Story Management (24h Ephemeral Updates) ---

    fun getActiveStatusesFlow(): Flow<List<com.example.data.local.StatusEntity>> =
        statusDao.getActiveStatusesFlow(System.currentTimeMillis())

    suspend fun publishStatus(
        text: String,
        mediaUrl: String? = null,
        backgroundColorHex: String = "#0284C7"
    ): Result<com.example.data.local.StatusEntity> = withContext(Dispatchers.IO) {
        val currentUser = userDao.getCurrentUser()
            ?: return@withContext Result.failure(IllegalStateException("Inicia sesión para publicar un estado."))

        val newStatus = com.example.data.local.StatusEntity(
            id = "status_${UUID.randomUUID().toString().take(8)}",
            authorUsername = currentUser.username,
            authorDisplayName = currentUser.displayName,
            authorAvatar = currentUser.avatarUrl,
            text = text.trim(),
            mediaUrl = mediaUrl,
            backgroundColorHex = backgroundColorHex,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
        )
        statusDao.insertStatus(newStatus)
        Result.success(newStatus)
    }

    suspend fun deleteStatus(statusId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            statusDao.deleteStatusById(statusId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            userDao.clearCurrentUserFlag()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
