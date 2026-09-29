package com.selkicx.manualbooth.ui.activesession

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.selkicx.manualbooth.data.local.entity.SessionPhotoEntity
import com.selkicx.manualbooth.di.AppContainer
import com.selkicx.manualbooth.domain.model.PhotoMode
import com.selkicx.manualbooth.ui.common.ViewModelFactory
import com.selkicx.manualbooth.ui.qr.QrDialog

/**
 * Active Session screen (spec sections 14, 18-22): live photo gallery,
 * numbered ordered selection, gated PRINT button, a single confirmation
 * popup, a lightweight status overlay, and Next Customer. After a
 * successful print, also offers Reprint (opens this session's detail,
 * spec section 22) and - only when QR Sharing is enabled in Admin - QR
 * (spec rules #24-26: never shown when the setting is off, never shown
 * before printing, never appears on its own).
 */
@Composable
fun ActiveSessionScreen(
    appContainer: AppContainer,
    sessionId: Long,
    onNextCustomer: (Long) -> Unit,
    onHome: () -> Unit,
    onViewSessionDetail: (Long) -> Unit
) {
    val viewModel: ActiveSessionViewModel = viewModel(
        key = "active_session_$sessionId",
        factory = ViewModelFactory {
            ActiveSessionViewModel(
                sessionId = sessionId,
                sessionRepository = appContainer.sessionRepository,
                templateRepository = appContainer.templateRepository,
                cameraAdapter = appContainer.cameraAdapter,
                printerAdapter = appContainer.printerAdapter,
                finalOutputUseCase = appContainer.finalOutputUseCase
            )
        }
    )
    val state by viewModel.uiState.collectAsState()
    val qrEnabled by appContainer.appSettingsRepository.qrSharingEnabled.collectAsState()
    var showQrDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imageReadPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasImageReadPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, imageReadPermission) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val imagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasImageReadPermission = granted
        if (granted) viewModel.startHotFolderImport()
    }
    val photoImporter = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        viewModel.importPhotos(uris.map(Uri::toString))
    }

    LaunchedEffect(state.canAutoImportPhotos) {
        if (state.canAutoImportPhotos) {
            if (hasImageReadPermission) {
                viewModel.startHotFolderImport()
            } else {
                imagePermissionLauncher.launch(imageReadPermission)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "Session ${state.session?.displayNumber ?: ""}",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = when (state.session?.photoMode) {
                    PhotoMode.BLACK_AND_WHITE -> "Black & White"
                    PhotoMode.ORIGINAL -> "Original"
                    null -> ""
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(8.dp))
            Text("Select ${state.requiredPhotoCount} Photos", style = MaterialTheme.typography.titleMedium)

            if (state.canAutoImportPhotos) {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (hasImageReadPermission) {
                        "AUTO IMPORT ON • Send a Canon photo to this device"
                    } else {
                        "Allow full photo access to receive Canon transfers automatically"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasImageReadPermission) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                if (!hasImageReadPermission) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { imagePermissionLauncher.launch(imageReadPermission) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ALLOW PHOTO ACCESS")
                    }
                }
            }

            if (state.canImportPhotos) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { photoImporter.launch(arrayOf("image/jpeg", "image/png")) },
                    enabled = state.printPhase == PrintPhase.IDLE || state.printPhase == PrintPhase.FAILED,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("IMPORT PHOTOS")
                }
            }

            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f)) {
                items(state.photos, key = { it.id }) { photo ->
                    PhotoThumbnail(
                        photo = photo,
                        selectionNumber = state.selectedPhotoIds.indexOf(photo.id)
                            .let { if (it >= 0) it + 1 else null },
                        onClick = { viewModel.togglePhoto(photo.id) }
                    )
                }
            }

            Text("Selected ${state.selectedPhotoIds.size} / ${state.requiredPhotoCount}")

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.requestPrintConfirmation() },
                enabled = state.canPrint && state.printPhase == PrintPhase.IDLE,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("PRINT")
            }

            state.errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            if (state.printPhase == PrintPhase.PRINTED) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.nextCustomer(onStarted = onNextCustomer) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("NEXT CUSTOMER")
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onViewSessionDetail(sessionId) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reprint")
                    }
                    if (qrEnabled) {
                        OutlinedButton(
                            onClick = { showQrDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("QR")
                        }
                    }
                }
            }
        }

        if (state.printPhase == PrintPhase.CONFIRMING) {
            PrintConfirmationDialog(
                templateName = state.templateName,
                photoCount = state.selectedPhotoIds.size,
                photoMode = state.session?.photoMode,
                onCancel = { viewModel.cancelPrintConfirmation() },
                onConfirm = { viewModel.confirmPrint() }
            )
        }

        if (state.printPhase == PrintPhase.RENDERING || state.printPhase == PrintPhase.PRINTING) {
            PrintStatusOverlay(state.printPhase)
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

@Composable
private fun PhotoThumbnail(
    photo: SessionPhotoEntity,
    selectionNumber: Int?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.padding(4.dp).aspectRatio(1f),
        contentAlignment = Alignment.TopStart
    ) {
        AsyncImage(
            model = photo.thumbnailPath,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().clickable(onClick = onClick)
        )
        if (selectionNumber != null) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    "$selectionNumber",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
