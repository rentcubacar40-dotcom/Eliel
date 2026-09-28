package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: ChatRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = AppDatabase.getDatabase(context)
        repository = ChatRepository(database)
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Nexus", appName)
    }

    @Test
    fun `test initial defaults include Eliel and default public general group`() = runBlocking {
        repository.initializeDefaultsIfNeeded()

        // 1. Creator Eliel must exist in directory
        val eliel = repository.getUserByUsername("Eliel_21")
        assertNotNull(eliel)
        assertEquals("Eliel", eliel?.displayName)

        // 2. Default Public General Community group must exist
        val generalGroup = database.chatDao().getChatById(ChatRepository.GENERAL_GROUP_ID)
        assertNotNull(generalGroup)
        assertEquals("GROUP", generalGroup?.type)
        assertTrue(generalGroup?.pinned == true)
    }

    @Test
    fun `test direct chat creation validates user existence`() = runBlocking {
        repository.initializeDefaultsIfNeeded()

        // Attempting to chat with non-existent user should fail
        val failureResult = repository.createDirectChat("usuario_inexistente_999")
        assertTrue(failureResult.isFailure)

        // Attempting to chat with existing user (@Eliel_21) should succeed
        val successResult = repository.createDirectChat("Eliel_21")
        assertTrue(successResult.isSuccess)
        assertNotNull(successResult.getOrNull())
    }

    @Test
    fun `test username registration and uniqueness check`() = runBlocking {
        repository.initializeDefaultsIfNeeded()

        // Eliel_21 is the creator handle and always accessible for setup/login
        val isElielAvailable = repository.checkUsernameAvailability("Eliel_21", null)
        assertTrue(isElielAvailable)

        // New username should be available before registration
        val isCarlosAvailableBefore = repository.checkUsernameAvailability("carlos_ucf", null)
        assertTrue(isCarlosAvailableBefore)

        // Register new user
        val regResult = repository.registerInitialUser(
            username = "carlos_ucf",
            displayName = "Carlos Perez",
            bio = "Estudiante UCF",
            avatarUrl = ""
        )
        assertTrue(regResult.isSuccess)

        // Now carlos_ucf should not be available for someone else without current handle
        val isCarlosAvailable = repository.checkUsernameAvailability("carlos_ucf", null)
        assertFalse(isCarlosAvailable)

        // But carlos_ucf is available when checking as Carlos himself
        val isCarlosSelfAvailable = repository.checkUsernameAvailability("carlos_ucf", "carlos_ucf")
        assertTrue(isCarlosSelfAvailable)
    }

    @Test
    fun `test admin login with password ElielElielAdmin543345`() = runBlocking {
        repository.initializeDefaultsIfNeeded()

        // 1. Wrong admin password fails
        val wrongLogin = repository.loginUser("Eliel_21", "clave_incorrecta")
        assertTrue(wrongLogin.isFailure)

        // 2. Correct admin password succeeds and grants OWNER role
        val adminLogin = repository.loginUser("Eliel_21", "ElielElielAdmin543345..")
        assertTrue(adminLogin.isSuccess)
        val adminUser = adminLogin.getOrNull()
        assertNotNull(adminUser)
        assertEquals("OWNER", adminUser?.role)
        assertTrue(adminUser?.isCurrentUser == true)
    }

    @Test
    fun `test room local message storage and deletion`() = runBlocking {
        repository.initializeDefaultsIfNeeded()
        repository.loginUser("Eliel_21", "ElielElielAdmin543345..")

        // Send local message stored in Room
        val sendRes = repository.sendMessage(ChatRepository.GENERAL_GROUP_ID, "Mensaje de prueba local")
        assertTrue(sendRes.isSuccess)
        val msg = sendRes.getOrNull()
        assertNotNull(msg)

        // Delete message from Room
        val delRes = repository.deleteMessage(msg!!.id)
        assertTrue(delRes.isSuccess)
    }

    @Test
    fun `test 24h status publication in room`() = runBlocking {
        repository.initializeDefaultsIfNeeded()
        repository.loginUser("Eliel_21", "ElielElielAdmin543345..")

        val statusRes = repository.publishStatus("Estado de bienvenida en Nexus", null, "#0284C7")
        assertTrue(statusRes.isSuccess)
        val status = statusRes.getOrNull()
        assertNotNull(status)
        assertEquals("Eliel_21", status?.authorUsername)
    }
}
