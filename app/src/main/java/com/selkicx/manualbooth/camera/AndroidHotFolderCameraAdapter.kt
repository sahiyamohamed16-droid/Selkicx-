package com.selkicx.manualbooth.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.selkicx.manualbooth.domain.adapters.CameraAdapter
import com.selkicx.manualbooth.domain.adapters.CameraConnectionState
import com.selkicx.manualbooth.domain.adapters.CapturedPhoto
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Receives photos that Canon Camera Connect or an NFC transfer writes to
 * Android's shared image library. The DSLR remains completely independent:
 * SelkicX never controls it and only reacts after Android publishes a new
 * Canon-looking JPEG/PNG in MediaStore.
 *
 * Manual SAF import remains available as a fallback. Every automatically
 * detected image is copied into the active session's private `originals`
 * directory before it enters the normal session pipeline.
 */
class AndroidHotFolderCameraAdapter(context: Context) : CameraAdapter {

    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val manualImporter = ManualImportCameraAdapter(appContext)
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scanMutex = Mutex()
    private val observerRegistered = AtomicBoolean(false)
    private val seenMediaIds = mutableSetOf<Long>()

    @Volatile
    private var activeSessionId: Long? = null

    @Volatile
    private var sessionStartedAtMillis: Long = 0L

    private val automaticIncoming = MutableSharedFlow<CapturedPhoto>(extraBufferCapacity = 32)
    private val _connectionState = MutableStateFlow(CameraConnectionState.DISCONNECTED)

    override val connectionState: Flow<CameraConnectionState> = _connectionState.asStateFlow()
    override val supportsManualImport: Boolean = true
    override val supportsHotFolderImport: Boolean = true

    private val mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            receiverScope.launch { scanForNewCanonImages() }
        }
    }

    override fun observeIncomingPhotos(): Flow<CapturedPhoto> = merge(
        automaticIncoming.asSharedFlow(),
        manualImporter.observeIncomingPhotos()
    )

    override suspend fun connect() {
        if (!hasImageReadPermission()) {
            _connectionState.value = CameraConnectionState.ERROR
            return
        }

        if (observerRegistered.compareAndSet(false, true)) {
            resolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaObserver
            )
        }
        _connectionState.value = CameraConnectionState.CONNECTED
    }

    override suspend fun disconnect() {
        if (observerRegistered.compareAndSet(true, false)) {
            resolver.unregisterContentObserver(mediaObserver)
        }
        _connectionState.value = CameraConnectionState.DISCONNECTED
    }

    override suspend fun beginSession(sessionId: Long) {
        if (activeSessionId != sessionId) {
            activeSessionId = sessionId
            sessionStartedAtMillis = System.currentTimeMillis()
            scanMutex.withLock { seenMediaIds.clear() }
        }
        connect()
        if (hasImageReadPermission()) scanForNewCanonImages()
    }

    override suspend fun importPhotos(sessionId: Long, sourceUris: List<String>) {
        manualImporter.importPhotos(sessionId, sourceUris)
    }

    override suspend fun fetchFullResolution(cameraFilename: String): String = cameraFilename

    private suspend fun scanForNewCanonImages() {
        val sessionId = activeSessionId ?: return
        if (!hasImageReadPermission()) {
            _connectionState.value = CameraConnectionState.ERROR
            return
        }

        scanMutex.withLock {
            val columns = buildList {
                add(MediaStore.Images.Media._ID)
                add(MediaStore.Images.Media.DISPLAY_NAME)
                add(MediaStore.Images.Media.MIME_TYPE)
                add(MediaStore.Images.Media.DATE_ADDED)
                add(MediaStore.Images.Media.DATE_TAKEN)
                add(MediaStore.Images.Media.SIZE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(MediaStore.Images.Media.RELATIVE_PATH)
                    add(MediaStore.Images.Media.OWNER_PACKAGE_NAME)
                }
            }.toTypedArray()

            val selectionParts = mutableListOf(
                "${MediaStore.Images.Media.DATE_ADDED} >= ?",
                "${MediaStore.Images.Media.SIZE} > 0"
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                selectionParts += "${MediaStore.Images.Media.IS_PENDING} = 0"
            }

            val selection = selectionParts.joinToString(" AND ")
            val selectionArgs = arrayOf((sessionStartedAtMillis / 1000L).toString())
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} ASC, ${MediaStore.Images.Media._ID} ASC"

            try {
                resolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    columns,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                    val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    val takenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                    val pathColumn = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                    val ownerColumn = cursor.getColumnIndex(MediaStore.Images.Media.OWNER_PACKAGE_NAME)

                    while (cursor.moveToNext()) {
                        val mediaId = cursor.getLong(idColumn)
                        if (mediaId in seenMediaIds) continue

                        val displayName = cursor.getString(nameColumn) ?: "photo.jpg"
                        val mimeType = cursor.getString(mimeColumn).orEmpty()
                        val relativePath = pathColumn.takeIf { it >= 0 }?.let(cursor::getString)
                        val ownerPackage = ownerColumn.takeIf { it >= 0 }?.let(cursor::getString)

                        if (!looksLikeCanonTransfer(displayName, mimeType, relativePath, ownerPackage)) {
                            seenMediaIds += mediaId
                            continue
                        }

                        val contentUri = Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            mediaId.toString()
                        )
                        val destination = copyIntoSessionWithRetry(
                            sessionId = sessionId,
                            mediaId = mediaId,
                            displayName = displayName,
                            contentUri = contentUri
                        ) ?: continue

                        seenMediaIds += mediaId
                        val dateTaken = cursor.getLong(takenColumn)
                        val dateAdded = cursor.getLong(addedColumn) * 1000L
                        val receivedAt = System.currentTimeMillis()
                        automaticIncoming.emit(
                            CapturedPhoto(
                                cameraFilename = displayName,
                                thumbnailPath = destination.absolutePath,
                                fullResPath = destination.absolutePath,
                                captureTimestamp = dateTaken.takeIf { it > 0L } ?: dateAdded,
                                receiveTimestamp = receivedAt
                            )
                        )
                    }
                }
                _connectionState.value = CameraConnectionState.CONNECTED
            } catch (_: SecurityException) {
                _connectionState.value = CameraConnectionState.ERROR
            }
        }
    }

    private suspend fun copyIntoSessionWithRetry(
        sessionId: Long,
        mediaId: Long,
        displayName: String,
        contentUri: Uri
    ): File? {
        val safeName = displayName
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "photo.jpg" }
        val directory = File(appContext.filesDir, "sessions/$sessionId/originals").apply { mkdirs() }
        val destination = File(directory, "${System.currentTimeMillis()}_${mediaId}_$safeName")

        repeat(3) { attempt ->
            try {
                resolver.openInputStream(contentUri)?.use { input ->
                    destination.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Unable to open $displayName")
                if (destination.length() > 0L) return destination
            } catch (_: Exception) {
                destination.delete()
                if (attempt < 2) delay((attempt + 1) * 500L)
            }
        }
        return null
    }

    private fun looksLikeCanonTransfer(
        displayName: String,
        mimeType: String,
        relativePath: String?,
        ownerPackage: String?
    ): Boolean {
        if (mimeType != "image/jpeg" && mimeType != "image/png") return false

        val canonOwned = ownerPackage?.contains("canon", ignoreCase = true) == true
        val canonFolder = relativePath?.contains("canon", ignoreCase = true) == true
        val canonFilename = CANON_FILENAME.matches(displayName)
        return canonOwned || canonFolder || canonFilename
    }

    private fun hasImageReadPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        val CANON_FILENAME = Regex("(?i)^(IMG_|_MG_).+\\.(JPE?G|PNG)$")
    }
}
