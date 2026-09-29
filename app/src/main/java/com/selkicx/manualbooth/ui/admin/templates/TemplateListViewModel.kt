package com.selkicx.manualbooth.ui.admin.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selkicx.manualbooth.data.repository.TemplateRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/** Templates within one folder, any size (spec section 24-25). */
class TemplateListViewModel(
    folderId: Long,
    templateRepository: TemplateRepository
) : ViewModel() {
    val templates = templateRepository.observeTemplatesInFolder(folderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
