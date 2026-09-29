package com.selkicx.manualbooth.ui.admin.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selkicx.manualbooth.data.local.entity.TemplateFolderEntity
import com.selkicx.manualbooth.data.repository.TemplateRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Admin > Templates root: folder create/rename/delete (spec section 24). */
class TemplateFoldersViewModel(
    private val templateRepository: TemplateRepository
) : ViewModel() {

    val folders = templateRepository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { templateRepository.createFolder(name) }
    }

    fun renameFolder(folder: TemplateFolderEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { templateRepository.renameFolder(folder, newName) }
    }

    fun deleteFolder(folder: TemplateFolderEntity) {
        viewModelScope.launch { templateRepository.deleteFolder(folder) }
    }
}
