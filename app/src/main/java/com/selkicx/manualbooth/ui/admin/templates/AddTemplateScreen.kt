package com.selkicx.manualbooth.ui.admin.templates

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.selkicx.manualbooth.di.AppContainer
import com.selkicx.manualbooth.ui.common.ViewModelFactory

/**
 * Admin > Add Template (spec section 25): Template Name -> print size
 * -> artwork upload (JPG/PNG, via Storage Access Framework - gallery,
 * Android Files, or mounted USB, never a hardcoded path).
 */
@Composable
fun AddTemplateScreen(
    appContainer: AppContainer,
    folderId: Long,
    onTemplateCreated: (Long) -> Unit
) {
    val viewModel: AddTemplateViewModel = viewModel(
        key = "add_template_$folderId",
        factory = ViewModelFactory {
            AddTemplateViewModel(
                folderId = folderId,
                templateRepository = appContainer.templateRepository,
                artworkImporter = appContainer.artworkImporter
            )
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val printSizes by viewModel.printSizes.collectAsState()

    val pickArtwork = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.importArtwork(it) } }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Add Template", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.name,
            onValueChange = { viewModel.setName(it) },
            label = { Text("Template Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Text("Size", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyRow {
            items(printSizes, key = { it.id }) { size ->
                FilterChip(
                    selected = uiState.selectedPrintSizeId == size.id,
                    onClick = { viewModel.selectPrintSize(size.id) },
                    label = { Text(size.name) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { pickArtwork.launch(arrayOf("image/jpeg", "image/png")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.artworkPath != null) "Artwork Selected \u2713" else "Choose File")
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.save(onSaved = onTemplateCreated) },
            enabled = uiState.canSave && !uiState.isSaving,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Save & Add Photo Holders")
        }
    }
}
