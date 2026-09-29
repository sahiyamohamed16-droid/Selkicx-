package com.selkicx.manualbooth.camera

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.selkicx.manualbooth.domain.adapters.CameraAdapter
import com.selkicx.manualbooth.domain.adapters.CameraConnectionState
import com.selkicx.manualbooth.domain.adapters.CapturedPhoto
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Production-safe fallback for events where no tethered camera SDK is
 * available. The operator selects JPEG/PNG files through Android's Storage
 * Access Framework. Every selected file is copied immediately into the
 * active session's app-private `originals` directory before it is emitted to
 * the rest of the capture pipeline.
 */
class ManualImportCameraAdapter(context: Context) : CameraAdapter {

    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    private val _connectionState = MutableStateFlow(CameraConnectionState.CONNECTED)
    override val connectionState: Flow<CameraConnectionState> = _connectionState.asStateFlow()
    override val supportsManualImport: Boolean = true

    private val incoming = MutableSharedFlow<CapturedPhoto>(extraBufferCapacity = 32)
    override fun observeIncomingPhotos(): Flow<CapturedPhoto> = incoming.asSharedFlow()

    override suspend fun connect() {
        _connectionState.value = CameraConnectionState.CONNECTED
    }

    override suspend fun disconnect() {
        _connectionState.value = CameraConnectionState.DISCONNECTED
    }

    override suspend fun fetchFullResolution(cameraFilename: String): String = cameraFilename

    override suspend fun importPhotos(sessionId: Long, sourceUris: List<String>) {
        if (sourceUris.isEmpty()) return

        withContext(Dispatchers.IO) {
            val originalsDirectory = File(
                appContext.filesDir,
                "sessions/$sessionId/originals"
            ).apply { mkdirs() }

            sourceUris.forEachIndexed { index, source ->
                val uri = Uri.parse(source)
                val originalName = queryDisplayName(uri)
                val safeName = originalName
                    .replace(Regex("[^A-Za-z0-9._-]"), "_")
                    .ifBlank { "photo.jpg" }
                val timestamp = System.currentTimeMillis()
                val destination = File(originalsDirectory, "${timestamp}_${index}_$safeName")

                resolver.openInputStream(uri)?.use { input ->
                    destination.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Unable to open selected image: $originalName")

                incoming.emit(
                    CapturedPhoto(
                        cameraFilename = originalName,
                        thumbnailPath = destination.absolutePath,
                        fullResPath = destination.absolutePath,
                        captureTimestamp = timestamp,
                        receiveTimestamp = timestamp
                    )
                )
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String {
        if (uri.scheme == "content") {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column >= 0 && cursor.moveToFirst()) {
                    cursor.getString(column)?.takeIf { it.isNotBlank() }?.let { return it }
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: "photo.jpg"
    }
}
