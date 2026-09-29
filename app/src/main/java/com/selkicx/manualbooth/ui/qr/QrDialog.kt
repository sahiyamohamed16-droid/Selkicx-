package com.selkicx.manualbooth.ui.qr

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.selkicx.manualbooth.di.AppContainer
import com.selkicx.manualbooth.ui.common.ViewModelFactory

/**
 * Shown only when the photographer explicitly taps QR (spec rule #26) -
 * never automatically. The share link this encodes covers everything
 * the package is meant to contain: all originals + the Final Output
 * (spec rule #27), once the real SelkicX backend resolves it.
 */
@Composable
fun QrDialog(appContainer: AppContainer, sessionId: Long, onDismiss: () -> Unit) {
    val viewModel: QrViewModel = viewModel(
        key = "qr_$sessionId",
        factory = ViewModelFactory {
            QrViewModel(sessionId, appContainer.qrRepository, appContainer.qrCodeGenerator)
        }
    )
    val state by viewModel.uiState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share via QR") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.isLoading) {
                    CircularProgressIndicator()
                } else {
                    state.qrBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(220.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(state.shareUrl)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
