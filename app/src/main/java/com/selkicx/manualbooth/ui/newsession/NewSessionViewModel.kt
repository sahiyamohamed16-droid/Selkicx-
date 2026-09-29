package com.selkicx.manualbooth.ui.newsession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selkicx.manualbooth.data.local.entity.PrintSizeEntity
import com.selkicx.manualbooth.data.local.entity.TemplateEntity
import com.selkicx.manualbooth.data.local.entity.TemplateFolderEntity
import com.selkicx.manualbooth.data.repository.SessionRepository
import com.selkicx.manualbooth.data.repository.TemplateRepository
import com.selkicx.manualbooth.domain.model.PhotoMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NewSessionStep { SIZE, FOLDER, TEMPLATE, PHOTO_MODE, CONFIRM }

data class NewSessionUiState(
    val step: NewSessionStep = NewSessionStep.SIZE,
    val selectedSize: PrintSizeEntity? = null,
    val selectedFolder: TemplateFolderEntity? = null,
    val selectedTemplate: TemplateEntity? = null,
    val requiredPhotoCount: Int = 0,
    val selectedPhotoMode: PhotoMode? = null,
    val isStarting: Boolean = false
)

/**
 * Drives the New Session flow (spec sections 6-13) as a single linear
 * wizard: size -> folder -> template -> photo mode -> confirm/start. One
 * shared ViewModel avoids threading selections through nav arguments.
 */
class NewSessionViewModel(
    private val templateRepository: TemplateRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewSessionUiState())
    val uiState: StateFlow<NewSessionUiState> = _uiState.asStateFlow()

    val printSizes = templateRepository.observePrintSizes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders = templateRepository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var templatesJob: Job? = null
    private val _templates = MutableStateFlow<List<TemplateEntity>>(emptyList())
    val templates: StateFlow<List<TemplateEntity>> = _templates.asStateFlow()

    fun selectSize(size: PrintSizeEntity) {
        _uiState.value = _uiState.value.copy(selectedSize = size, step = NewSessionStep.FOLDER)
    }

    fun selectFolder(folder: TemplateFolderEntity) {
        val size = _uiState.value.selectedSize ?: return
        _uiState.value = _uiState.value.copy(selectedFolder = folder, step = NewSessionStep.TEMPLATE)

        templatesJob?.cancel()
        templatesJob = templateRepository.observeTemplates(folder.id, size.id)
            .onEach { _templates.value = it }
            .launchIn(viewModelScope)
    }

    fun selectTemplate(template: TemplateEntity) {
        viewModelScope.launch {
            val count = templateRepository.requiredPhotoCount(template.id)
            _uiState.value = _uiState.value.copy(
                selectedTemplate = template,
                requiredPhotoCount = count,
                step = NewSessionStep.PHOTO_MODE
            )
        }
    }

    fun selectPhotoMode(mode: PhotoMode) {
        _uiState.value = _uiState.value.copy(selectedPhotoMode = mode, step = NewSessionStep.CONFIRM)
    }

    fun back() {
        val previous = when (_uiState.value.step) {
            NewSessionStep.SIZE -> NewSessionStep.SIZE
            NewSessionStep.FOLDER -> NewSessionStep.SIZE
            NewSessionStep.TEMPLATE -> NewSessionStep.FOLDER
            NewSessionStep.PHOTO_MODE -> NewSessionStep.TEMPLATE
            NewSessionStep.CONFIRM -> NewSessionStep.PHOTO_MODE
        }
        _uiState.value = _uiState.value.copy(step = previous)
    }

    /** Starts the session and reports the new session id via [onStarted]. */
    fun startSession(onStarted: (Long) -> Unit) {
        val state = _uiState.value
        val template = state.selectedTemplate ?: return
        val size = state.selectedSize ?: return
        val mode = state.selectedPhotoMode ?: return

        _uiState.value = state.copy(isStarting = true)
        viewModelScope.launch {
            val displayNumber = sessionRepository.generateNextDisplayNumber()
            val sessionId = sessionRepository.startSession(
                displayNumber = displayNumber,
                templateId = template.id,
                printSizeId = size.id,
                photoMode = mode
            )
            onStarted(sessionId)
        }
    }
}
