package com.selkicx.manualbooth.ui.admin.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.selkicx.manualbooth.data.local.entity.TemplateFolderEntity
import com.selkicx.manualbooth.di.AppContainer
import com.selkicx.manualbooth.ui.common.ViewModelFactory

/**
 * Admin > Templates (spec section 24): create/rename/delete folders.
 * Folders are purely organizational (spec section 7) - opening one goes
 * to its template list.
 */
@Composable
fun TemplateFoldersScreen(
    appContainer: AppContainer,
    onOpenFolder: (TemplateFolderEntity) -> Unit
) {
    val viewModel: TemplateFoldersViewModel = viewModel(
        factory = ViewModelFactory { TemplateFoldersViewModel(appContainer.templateRepository) }
    )
    val folders by viewModel.folders.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<TemplateFolderEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Templates", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(folders, key = { it.id }) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onOpenFolder(folder) },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(folder.name, style = MaterialTheme.typography.titleMedium)
                    Row {
                        IconButton(onClick = { renameTarget = folder }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Rename")
                        }
                        IconButton(onClick = { viewModel.deleteFolder(folder) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }

        Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("+ NEW FOLDER")
        }
    }

    if (showAddDialog) {
        NameInputDialog(
            title = "New Folder",
            initialValue = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                viewModel.createFolder(name)
                showAddDialog = false
            }
        )
    }

    renameTarget?.let { folder ->
        NameInputDialog(
            title = "Rename Folder",
            initialValue = folder.name,
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                viewModel.renameFolder(folder, name)
                renameTarget = null
            }
        )
    }
}
