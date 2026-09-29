package com.selkicx.manualbooth.ui.qr

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selkicx.manualbooth.data.repository.QrRepository
import com.selkicx.manualbooth.qr.QrCodeGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QrUiState(
    val isLoading: Boolean = true,
    val shareUrl: String = "",
    val qrBitmap: Bitmap? = null
)

/**
 * Generates/fetches the share link and QR bitmap for one session. Only
 * ever constructed when the photographer explicitly taps QR (spec rules
 * #25-26) - there is no code path that shows this without that tap.
 */
class QrViewModel(
    private val sessionId: Long,
    private val qrRepository: QrRepository,
    private val qrCodeGenerator: QrCodeGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrUiState())
    val uiState: StateFlow<QrUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val shareLink = qrRepository.getOrCreateShareLink(sessionId)
            val url = qrRepository.shareUrl(shareLink)
            val bitmap = qrCodeGenerator.generate(url)
            _uiState.value = QrUiState(isLoading = false, shareUrl = url, qrBitmap = bitmap)
        }
    }
}
