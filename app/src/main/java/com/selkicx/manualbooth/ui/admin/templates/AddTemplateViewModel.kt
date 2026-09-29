package com.selkicx.manualbooth.ui.admin.templates

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selkicx.manualbooth.data.repository.TemplateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AddTemplateUiState(
    val name: String = "",
    val selectedPrintSizeId: Long? = null,
    val artworkPath: String? = null,
    val isSaving: Boolean = false
) {
    val canSave: Boolean get() = name.isNotBlank() && selectedPrintSizeId != null && artworkPath != null
}

/**
 * Admin > Add Template (spec section 25): name -> size -> artwork
 * upload. Required photo count is never asked here - it comes from
 * holders added afterward in the holder editor (spec rule #13).
 */
class AddTemplateViewModel(
    private val folderId: Long,
    private val templateRepository: TemplateRepository,
    private val artworkImporter: ArtworkImporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTemplateUiState())
    val uiState: StateFlow<AddTemplateUiState> = _uiState.asStateFlow()

    val printSizes = templateRepository.observePrintSizes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun selectPrintSize(id: Long) {
        _uiState.value = _uiState.value.copy(selectedPrintSizeId = id)
    }

    fun importArtwork(uri: Uri) {
        viewModelScope.launch {
            val path = artworkImporter.importArtwork(uri)
            _uiState.value = _uiState.value.copy(artworkPath = path)
        }
    }

    fun save(onSaved: (Long) -> Unit) {
        val state = _uiState.value
        val sizeId = state.selectedPrintSizeId ?: return
        val artwork = state.artworkPath ?: return

        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            val templateId = templateRepository.createTemplate(
                folderId = folderId,
                printSizeId = sizeId,
                name = state.name,
                artworkPath = artwork,
                thumbnailPath = artwork
            )
            onSaved(templateId)
        }
    }
}
