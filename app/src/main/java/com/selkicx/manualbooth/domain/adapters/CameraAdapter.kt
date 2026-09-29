package com.selkicx.manualbooth.domain.adapters

import kotlinx.coroutines.flow.Flow

/** Connection status shown on the Home screen (spec section 5). */
enum class CameraConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * A single photo delivered from the camera. Thumbnail arrives first so
 * the operator isn't waiting on the full DSLR file (spec section 16);
 * fullResPath is filled in once the background download completes.
 */
data class CapturedPhoto(
    val cameraFilename: String,
    val thumbnailPath: String,
    val fullResPath: String?,
    val captureTimestamp: Long,
    val receiveTimestamp: Long
)

/**
 * Implemented per camera brand (Canon, Nikon, Sony, manual import, mock).
 * UI/rendering/printing code must never depend on a concrete adapter -
 * only on this interface (spec section 15).
 */
interface CameraAdapter {
    val connectionState: Flow<CameraConnectionState>

    /** True when this adapter can accept operator-selected image files. */
    val supportsManualImport: Boolean get() = false

    /** True when Android media storage can be watched for camera transfers. */
    val supportsHotFolderImport: Boolean get() = false

    /** Emits each new photo as it's detected, thumbnail-first. */
    fun observeIncomingPhotos(): Flow<CapturedPhoto>

    suspend fun connect()
    suspend fun disconnect()

    /**
     * Points an automatic receiver at the current session. Camera adapters
     * that do not watch an Android hot folder can keep the no-op default.
     */
    suspend fun beginSession(sessionId: Long) = Unit

    /**
     * Imports operator-selected images into [sessionId]. Hardware camera
     * adapters keep the default implementation; the manual fallback copies
     * the selected files into the session's private originals directory.
     */
    suspend fun importPhotos(sessionId: Long, sourceUris: List<String>) {
        throw UnsupportedOperationException("Manual photo import is not supported by this camera adapter")
    }

    /** Downloads/refreshes the full-resolution file for a given photo. */
    suspend fun fetchFullResolution(cameraFilename: String): String
}
