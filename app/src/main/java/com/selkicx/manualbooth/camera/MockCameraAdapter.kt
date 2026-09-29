package com.selkicx.manualbooth.camera

import com.selkicx.manualbooth.domain.adapters.CameraAdapter
import com.selkicx.manualbooth.domain.adapters.CameraConnectionState
import com.selkicx.manualbooth.domain.adapters.CapturedPhoto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Feeds a stream of fake photos so the full app workflow (spec sections
 * 56-58) can be built and tested before any real camera SDK integration
 * (spec section 15: "do not waste the first development phase").
 *
 * Point [sampleImagePaths] at a folder of test JPGs on device; each call
 * to [simulateCapture] (wired to a debug button) emits the next one.
 */
class MockCameraAdapter(
    private val sampleImagePaths: List<String> = emptyList()
) : CameraAdapter {

    private val _connectionState = MutableStateFlow(CameraConnectionState.DISCONNECTED)
    override val connectionState: Flow<CameraConnectionState> = _connectionState.asStateFlow()

    private var nextSampleIndex = 0
    private val incoming = MutableSharedFlow<CapturedPhoto>(extraBufferCapacity = 16)

    override fun observeIncomingPhotos(): Flow<CapturedPhoto> = incoming.asSharedFlow()

    override suspend fun connect() {
        _connectionState.value = CameraConnectionState.CONNECTING
        delay(300) // simulate handshake
        _connectionState.value = CameraConnectionState.CONNECTED
    }

    override suspend fun disconnect() {
        _connectionState.value = CameraConnectionState.DISCONNECTED
    }

    override suspend fun fetchFullResolution(cameraFilename: String): String {
        delay(200) // simulate background full-res download
        return sampleImagePaths.firstOrNull { it.endsWith(cameraFilename) } ?: cameraFilename
    }

    /** Call from a debug/test control to simulate the photographer taking a shot. */
    suspend fun simulateCapture() {
        if (sampleImagePaths.isEmpty()) return
        val path = sampleImagePaths[nextSampleIndex % sampleImagePaths.size]
        nextSampleIndex++
        val now = System.currentTimeMillis()
        incoming.emit(
            CapturedPhoto(
                cameraFilename = path.substringAfterLast('/'),
                thumbnailPath = path,
                fullResPath = path,
                captureTimestamp = now,
                receiveTimestamp = now
            )
        )
    }
}
