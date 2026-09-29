package com.selkicx.manualbooth.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.selkicx.manualbooth.di.AppContainer
import com.selkicx.manualbooth.domain.model.PrintStatus
import com.selkicx.manualbooth.ui.common.ViewModelFactory
import com.selkicx.manualbooth.ui.qr.QrDialog

/**
 * Session History > session detail (spec section 48): originals, the
 * Final Output, Reprint - reusing the already-rendered file, with its
 * own single confirmation popup (spec section 49) - and, only when QR
 * Sharing is enabled in Admin, QR (spec rule #26: still never automatic,
 * only on explicit tap).
 */
@Composable
fun SessionDetailScreen(
    appContainer: AppContainer,
    sessionId: Long
) {
    val viewModel: SessionDetailViewModel = viewModel(
        key = "session_detail_$sessionId",
        factory = ViewModelFactory {
            SessionDetailViewModel(
                sessionId = sessionId,
                sessionRepository = appContainer.sessionRepository,
                templateRepository = appContainer.templateRepository,
                printerAdapter = appContainer.printerAdapter
            )
        }
    )
    val state by viewModel.uiState.collectAsState()
    val qrEnabled by appContainer.appSettingsRepository.qrSharingEnabled.collectAsState()
    var showQrDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(state.session?.displayNumber ?: "", style = MaterialTheme.typography.titleLarge)
            Text("${state.printSizeName}  \u2022  ${state.templateName}")
            Text("${state.originals.size} Photos")
            Text(if (state.session?.printStatus == PrintStatus.PRINTED) "Printed" else "Not Printed")

            state.finalOutput?.let { output ->
                Spacer(Modifier.height(16.dp))
                Text("Final Output", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = output.filePath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Originals", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f)) {
                items(state.originals, key = { it.id }) { photo ->
                    AsyncImage(
                        model = photo.thumbnailPath,
                        contentDescription = null,
                        modifier = Modifier.padding(2.dp).aspectRatio(1f)
                    )
                }
            }

            state.finalOutput?.let {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.requestReprint() },
                        enabled = state.reprintPhase == ReprintPhase.IDLE,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("Reprint")
                    }
                    if (qrEnabled) {
                        OutlinedButton(
                            onClick = { showQrDialog = true },
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Text("QR")
                        }
                    }
                }
            }

            state.errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }

        if (state.reprintPhase == ReprintPhase.CONFIRMING) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelReprint() },
                title = { Text("Reprint this Final Output?") },
                confirmButton = { TextButton(onClick = { viewModel.confirmReprint() }) { Text("PRINT") } },
                dismissButton = { TextButton(onClick = { viewModel.cancelReprint() }) { Text("CANCEL") } }
            )
        }

        if (state.reprintPhase == ReprintPhase.PRINTING) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(shape = MaterialTheme.shapes.medium) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Text("Printing...")
                    }
                }
            }
        }
    }

    if (showQrDialog) {
        QrDialog(
            appContainer = appContainer,
            sessionId = sessionId,
            onDismiss = { showQrDialog = false }
        )
    }
}
