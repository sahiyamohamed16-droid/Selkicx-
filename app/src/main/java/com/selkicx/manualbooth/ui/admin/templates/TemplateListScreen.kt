package com.selkicx.manualbooth.ui.admin.templates

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.selkicx.manualbooth.di.AppContainer
import com.selkicx.manualbooth.ui.common.ViewModelFactory

/**
 * Admin > Templates > [folder] (spec section 25): lists templates,
 * tapping one opens the holder editor; ADD TEMPLATE starts creation.
 */
@Composable
fun TemplateListScreen(
    appContainer: AppContainer,
    folderId: Long,
    onAddTemplate: () -> Unit,
    onOpenTemplate: (Long) -> Unit
) {
    val viewModel: TemplateListViewModel = viewModel(
        key = "template_list_$folderId",
        factory = ViewModelFactory { TemplateListViewModel(folderId, appContainer.templateRepository) }
    )
    val templates by viewModel.templates.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Templates", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(templates, key = { it.id }) { template ->
                OutlinedButton(
                    onClick = { onOpenTemplate(template.id) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(template.name)
                }
            }
        }

        Button(onClick = onAddTemplate, modifier = Modifier.fillMaxWidth()) {
            Text("+ ADD TEMPLATE")
        }
    }
}
