package com.example.data.moodle

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class MoodleUploadResult(
    val url: String,
    val fileName: String,
    val fileSize: Long,
    val itemId: String,
    val isCloudSynced: Boolean
)

/**
 * Robust Client for cursos.ucf.edu.cu
 * Handles login, session management, file upload (under 4MB limit),
 * and JSON messaging sync with NO mock or simulated data.
 */
class UcfMoodleClient(
    private var host: String = "https://cursos.ucf.edu.cu/",
    private var repoId: Int = 4
) {
    companion object {
        const val TAG = "UcfMoodleClient"
        const val MAX_FILE_SIZE_BYTES = 4 * 1024 * 1024L // 4MB maximum allowed
    }

    private val cookieStore = HashMap<String, MutableList<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val key = url.host
            val currentList = cookieStore.getOrPut(key) { mutableListOf() }
            for (cookie in cookies) {
                currentList.removeAll { it.name == cookie.name }
                currentList.add(cookie)
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private var sesskey: String? = null
    private var isSessionActive = false
    private var activeUsername: String? = null
    private var activePassword: String? = null
    private var activeToken: String? = null
    private var userId: String = "4"

    fun configure(moodleHost: String, repositoryId: Int) {
        this.host = if (moodleHost.endsWith("/")) moodleHost else "$moodleHost/"
        this.repoId = repositoryId
    }

    suspend fun login(user: String, pass: String): Boolean = withContext(Dispatchers.IO) {
        if (user.isBlank() || pass.isBlank()) return@withContext false
        activeUsername = user
        activePassword = pass

        try {
            // 1. Fetch login page to retrieve logintoken
            val loginPageUrl = "${host}login/index.php"
            val getReq = Request.Builder()
                .url(loginPageUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val getResp = httpClient.newCall(getReq).execute()
            val getHtml = getResp.body?.string() ?: ""

            val pattern = Pattern.compile("name=[\"']logintoken[\"']\\s+value=[\"']([^\"']+)[\"']")
            val matcher = pattern.matcher(getHtml)
            val loginToken = if (matcher.find()) matcher.group(1) else ""

            // 2. Post login credentials
            val formBuilder = FormBody.Builder()
                .add("username", user)
                .add("password", pass)

            if (!loginToken.isNullOrEmpty()) {
                formBuilder.add("logintoken", loginToken)
            }

            val postReq = Request.Builder()
                .url(loginPageUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .post(formBuilder.build())
                .build()

            val postResp = httpClient.newCall(postReq).execute()
            val postHtml = postResp.body?.string() ?: ""

            // 3. Extract sesskey from dashboard or home page
            val sessPattern = Pattern.compile("[\"']sesskey[\"']\\s*:\\s*[\"']([^\"']+)[\"']|sesskey=([a-zA-Z0-9]+)")
            val sessMatcher = sessPattern.matcher(postHtml)
            if (sessMatcher.find()) {
                sesskey = sessMatcher.group(1) ?: sessMatcher.group(2)
            }

            // Extract userId
            val userPattern = Pattern.compile("user/profile\\.php\\?id=([0-9]+)|data-userid=[\"']([0-9]+)[\"']")
            val userMatcher = userPattern.matcher(postHtml)
            if (userMatcher.find()) {
                userId = userMatcher.group(1) ?: userMatcher.group(2) ?: "4"
            }

            isSessionActive = sesskey != null
            isSessionActive
        } catch (e: Exception) {
            Log.e(TAG, "Moodle login error: ${e.message}")
            false
        }
    }

    private suspend fun ensureAuthenticated(): Boolean = withContext(Dispatchers.IO) {
        if (isSessionActive && sesskey != null) return@withContext true
        val u = activeUsername
        val p = activePassword
        if (!u.isNullOrEmpty() && !p.isNullOrEmpty()) {
            return@withContext login(u, p)
        }
        false
    }

    /**
     * Automatic image compression to guarantee <= 4MB size before uploading.
     */
    fun compressImageIfNeeded(bytes: ByteArray): ByteArray {
        if (bytes.size <= MAX_FILE_SIZE_BYTES) return bytes
        try {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
            var quality = 90
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

            // Reduce quality until strictly under 3.8 MB
            while (outputStream.size() > (3.8 * 1024 * 1024) && quality > 20) {
                outputStream.reset()
                quality -= 15
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            }

            if (outputStream.size() <= MAX_FILE_SIZE_BYTES) {
                return outputStream.toByteArray()
            }

            // Downscale dimensions if still over 4MB
            val scaled = Bitmap.createScaledBitmap(bitmap, bitmap.width / 2, bitmap.height / 2, true)
            outputStream.reset()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            return outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Image compression error: ${e.message}")
            return bytes
        }
    }

    /**
     * Uploads file to UCF Moodle with real progress reporting.
     * Enforces the 4MB limit strictly.
     * DOES NOT use fake network simulation on error.
     */
    suspend fun uploadFile(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String = "application/octet-stream",
        onProgress: (bytesSent: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<MoodleUploadResult> = withContext(Dispatchers.IO) {
        // Auto compress if image
        val finalBytes = if (mimeType.startsWith("image")) {
            compressImageIfNeeded(fileBytes)
        } else {
            fileBytes
        }

        val totalSize = finalBytes.size.toLong()
        if (totalSize > MAX_FILE_SIZE_BYTES) {
            val sizeMb = String.format("%.2f", totalSize / (1024.0 * 1024.0))
            return@withContext Result.failure(
                IllegalArgumentException("El archivo excede el límite permitido de 4.0 MB (pesa $sizeMb MB).")
            )
        }

        ensureAuthenticated()

        val currentSesskey = sesskey
        if (currentSesskey == null) {
            return@withContext Result.failure(
                IOException("No hay sesión activa en Moodle. Por favor configura o verifica tu usuario en el panel.")
            )
        }

        try {
            val itemPostId = (System.currentTimeMillis() % 10000000).toString()
            val uploadUrl = "${host}repository/repository_ajax.php?action=upload"
            val mediaType = mimeType.toMediaTypeOrNull()

            val countingBody = CountingRequestBody(
                RequestBody.create(mediaType, finalBytes)
            ) { bytesWritten, contentLength ->
                onProgress(bytesWritten, contentLength)
            }

            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("title", fileName)
                .addFormDataPart("author", activeUsername ?: "Nexus User")
                .addFormDataPart("license", "allrightsreserved")
                .addFormDataPart("itemid", itemPostId)
                .addFormDataPart("repo_id", repoId.toString())
                .addFormDataPart("p", "")
                .addFormDataPart("page", "")
                .addFormDataPart("env", "filemanager")
                .addFormDataPart("sesskey", currentSesskey)
                .addFormDataPart("client_id", "nexus_${UUID.randomUUID().toString().take(6)}")
                .addFormDataPart("maxbytes", MAX_FILE_SIZE_BYTES.toString())
                .addFormDataPart("areamaxbytes", MAX_FILE_SIZE_BYTES.toString())
                .addFormDataPart("ctx_id", "1")
                .addFormDataPart("savepath", "/")
                .addFormDataPart("repo_upload_file", fileName, countingBody)

            val request = Request.Builder()
                .url(uploadUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .post(multipartBuilder.build())
                .build()

            val response = httpClient.newCall(request).execute()
            val respText = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Error del servidor Moodle: HTTP ${response.code}")
                )
            }

            if (respText.contains("\"error\"") && !respText.contains("\"url\"")) {
                val errorMsg = try {
                    JSONObject(respText).optString("error", "Error al procesar archivo en Moodle")
                } catch (e: Exception) {
                    "Error al subir archivo a Moodle"
                }
                return@withContext Result.failure(IOException(errorMsg))
            }

            var finalUrl = "${host}draftfile.php/$userId/user/draft/$itemPostId/$fileName"
            var isRealCloud = false

            if (respText.contains("\"url\"")) {
                val jsonObj = JSONObject(respText)
                val rawUrl = jsonObj.optString("url", finalUrl).replace("\\", "")
                finalUrl = if (activeToken != null) {
                    rawUrl.replace("pluginfile.php/", "webservice/pluginfile.php/") + "?token=$activeToken"
                } else {
                    rawUrl
                }
                isRealCloud = true
            }

            Result.success(
                MoodleUploadResult(
                    url = finalUrl,
                    fileName = fileName,
                    fileSize = totalSize,
                    itemId = itemPostId,
                    isCloudSynced = isRealCloud
                )
            )
        } catch (networkEx: Exception) {
            Log.e(TAG, "Moodle upload network error: ${networkEx.message}")
            Result.failure(networkEx)
        }
    }

    /**
     * Uploads JSON payload for sync.
     */
    suspend fun uploadJsonSync(
        fileName: String,
        jsonString: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val bytes = jsonString.toByteArray(Charsets.UTF_8)
        val uploadRes = uploadFile(bytes, fileName, "application/json")
        uploadRes.map { it.url }
    }

    /**
     * Downloads JSON string from cloud URL.
     */
    suspend fun fetchJsonFromUrl(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).build()
            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string()
            if (resp.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(IOException("Respuesta HTTP ${resp.code} al obtener JSON"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Request body wrapper that reports actual upload progress.
 */
class CountingRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (bytesWritten: Long, contentLength: Long) -> Unit
) : RequestBody() {
    override fun contentType() = delegate.contentType()
    override fun contentLength(): Long {
        return try {
            delegate.contentLength()
        } catch (e: IOException) {
            -1L
        }
    }

    override fun writeTo(sink: BufferedSink) {
        val countingSink = CountingSink(sink, contentLength(), onProgress)
        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    private class CountingSink(
        delegate: okio.Sink,
        private val totalBytes: Long,
        private val onProgress: (bytesWritten: Long, contentLength: Long) -> Unit
    ) : ForwardingSink(delegate) {
        private var bytesWritten = 0L

        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            onProgress(bytesWritten, totalBytes)
        }
    }
}
