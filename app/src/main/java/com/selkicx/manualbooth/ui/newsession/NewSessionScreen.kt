package com.selkicx.manualbooth.ui.newsession

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.selkicx.manualbooth.di.AppContainer
import com.selkicx.manualbooth.domain.model.PhotoMode
import com.selkicx.manualbooth.ui.common.ViewModelFactory

/**
 * New Session wizard (spec sections 6-13): size -> folder -> template ->
 * photo mode -> confirm/start. One screen, step-driven - no extra page
 * levels (spec section 4).
 */
@Composable
fun NewSessionScreen(
    appContainer: AppContainer,
    onSessionStarted: (Long) -> Unit,
    onCancel: () -> Unit
) {
    val viewModel: NewSessionViewModel = viewModel(
        factory = ViewModelFactory {
            NewSessionViewModel(
                templateRepository = appContainer.templateRepository,
                sessionRepository = appContainer.sessionRepository
            )
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val printSizes by viewModel.printSizes.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val templates by viewModel.templates.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        TextButton(onClick = onCancel) { Text("Cancel") }

        when (uiState.step) {
            NewSessionStep.SIZE -> StepList(
                title = "Choose Print Size",
                items = printSizes,
                label = { it.name },
                onSelect = { viewModel.selectSize(it) }
            )
            NewSessionStep.FOLDER -> StepList(
                title = "Choose Template Folder",
                items = folders,
                label = { it.name },
                onSelect = { viewModel.selectFolder(it) }
            )
            NewSessionStep.TEMPLATE -> StepList(
                title = "Choose Template",
                items = templates,
                label = { it.name },
                onSelect = { viewModel.selectTemplate(it) }
            )
            NewSessionStep.PHOTO_MODE -> PhotoModeStep(
                onSelect = { viewModel.selectPhotoMode(it) }
            )
            NewSessionStep.CONFIRM -> ConfirmStep(
                uiState = uiState,
                onStart = { viewModel.startSession(onStarted = onSessionStarted) }
            )
        }
    }
}

@Composable
private fun <T> StepList(
    title: String,
    items: List<T>,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(16.dp))
    LazyColumn {
        items(items) { item ->
            OutlinedButton(
                onClick = { onSelect(item) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(label(item))
            }
        }
    }
}

@Composable
private fun PhotoModeStep(onSelect: (PhotoMode) -> Unit) {
    Text("Photo Mode", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { onSelect(PhotoMode.ORIGINAL) },
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) { Text("ORIGINAL") }
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = { onSelect(PhotoMode.BLACK_AND_WHITE) },
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) { Text("BLACK & WHITE") }
}

@Composable
private fun ConfirmStep(uiState: NewSessionUiState, onStart: () -> Unit) {
    Text(uiState.selectedSize?.name ?: "", style = MaterialTheme.typography.headlineSmall)
    Text(uiState.selectedTemplate?.name ?: "")
    Text("${uiState.requiredPhotoCount} Photos")
    Text(if (uiState.selectedPhotoMode == PhotoMode.BLACK_AND_WHITE) "Black & White" else "Original")

    Spacer(Modifier.height(24.dp))
    Button(
        onClick = onStart,
        enabled = !uiState.isStarting,
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Text("START SESSION")
    }
}
