package com.selkicx.manualbooth.ui.activesession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selkicx.manualbooth.data.local.entity.SessionEntity
import com.selkicx.manualbooth.data.local.entity.SessionPhotoEntity
import com.selkicx.manualbooth.data.repository.SessionRepository
import com.selkicx.manualbooth.data.repository.TemplateRepository
import com.selkicx.manualbooth.domain.adapters.CameraAdapter
import com.selkicx.manualbooth.domain.adapters.PrintJob
import com.selkicx.manualbooth.domain.adapters.PrintResult
import com.selkicx.manualbooth.domain.adapters.PrinterAdapter
import com.selkicx.manualbooth.domain.model.PrintStatus
import com.selkicx.manualbooth.domain.model.SessionState
import com.selkicx.manualbooth.rendering.FinalOutputUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PrintPhase { IDLE, CONFIRMING, RENDERING, PRINTING, PRINTED, FAILED }

data class ActiveSessionUiState(
    val session: SessionEntity? = null,
    val templateName: String = "",
    val photos: List<SessionPhotoEntity> = emptyList(),
    /** Ordered; order = holder assignment (spec rule #14). */
    val selectedPhotoIds: List<Long> = emptyList(),
    val requiredPhotoCount: Int = 0,
    val canImportPhotos: Boolean = false,
    val printPhase: PrintPhase = PrintPhase.IDLE,
    val errorMessage: String? = null
) {
    val canPrint: Boolean get() = requiredPhotoCount > 0 && selectedPhotoIds.size == requiredPhotoCount
}

/**
 * Drives the Active Session screen (spec sections 14, 18-22): live photo
 * gallery, ordered selection, PRINT gating + a single confirmation popup
 * (no separate preview page), the real render pipeline on confirm, and
 * Next Customer session-cycling.
 */
class ActiveSessionViewModel(
    private val sessionId: Long,
    private val sessionRepository: SessionRepository,
    private val templateRepository: TemplateRepository,
    private val cameraAdapter: CameraAdapter,
    private val printerAdapter: PrinterAdapter,
    private val finalOutputUseCase: FinalOutputUseCase
) : ViewModel() {

    private val selectedIds = MutableStateFlow<List<Long>>(emptyList())
    private val printPhase = MutableStateFlow(PrintPhase.IDLE)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val requiredCount = MutableStateFlow(0)
    private val templateName = MutableStateFlow("")

    private val coreState = combine(
        sessionRepository.observeSession(sessionId),
        sessionRepository.observeSessionPhotos(sessionId),
        selectedIds,
        requiredCount,
        printPhase
    ) { session, photos, selected, required, phase ->
        ActiveSessionUiState(
            session = session,
            photos = photos,
            selectedPhotoIds = selected,
            requiredPhotoCount = required,
            canImportPhotos = cameraAdapter.supportsManualImport,
            printPhase = phase
        )
    }

    val uiState: StateFlow<ActiveSessionUiState> = combine(
        coreState,
        templateName,
        errorMessage
    ) { state, name, error ->
        state.copy(templateName = name, errorMessage = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActiveSessionUiState())

    init {
        viewModelScope.launch {
            sessionRepository.observeSession(sessionId).collect { session ->
                if (session != null) {
                    requiredCount.value = templateRepository.requiredPhotoCount(session.templateId)
                    templateName.value = templateRepository.getTemplate(session.templateId)?.name ?: ""
                }
            }
        }
    }

    /** Tap to select (next holder number); tap again to deselect and shift order. */
    fun togglePhoto(photoId: Long) {
        val current = selectedIds.value
        selectedIds.value = if (current.contains(photoId)) {
            current - photoId
        } else if (current.size < requiredCount.value) {
            current + photoId
        } else {
            current
        }
    }

    fun requestPrintConfirmation() {
        if (uiState.value.canPrint) printPhase.value = PrintPhase.CONFIRMING
    }

    fun cancelPrintConfirmation() {
        printPhase.value = PrintPhase.IDLE
    }

    /** Imports SAF-selected images through the configured fallback adapter. */
    fun importPhotos(sourceUris: List<String>) {
        if (!cameraAdapter.supportsManualImport || sourceUris.isEmpty()) return

        viewModelScope.launch {
            errorMessage.value = null
            try {
                cameraAdapter.importPhotos(sessionId, sourceUris)
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Unable to import selected photos"
            }
        }
    }

    /**
     * Only path that actually prints - always follows the single
     * confirmation popup (spec rules #16, #17). Renders the real Final
     * Output via [FinalOutputUseCase] (Milestone 4: crop/mask/B&W/
     * template composite), then sends it to the printer.
     */
    fun confirmPrint() {
        viewModelScope.launch {
            printPhase.value = PrintPhase.RENDERING
            errorMessage.value = null
            try {
                sessionRepository.saveSelection(sessionId, selectedIds.value)
                sessionRepository.updatePrintProgress(
                    sessionId,
                    SessionState.RENDERING,
                    PrintStatus.NOT_PRINTED
                )
                val rendered = finalOutputUseCase.render(sessionId, selectedIds.value)
                printPhase.value = PrintPhase.PRINTING
                sessionRepository.updatePrintProgress(
                    sessionId,
                    SessionState.PRINTING,
                    PrintStatus.PRINTING
                )
                val result = printerAdapter.print(
                    PrintJob(finalOutputPath = rendered.absolutePath, printSizeName = "", copies = 1)
                )
                when (result) {
                    is PrintResult.Success -> {
                        printPhase.value = PrintPhase.PRINTED
                        sessionRepository.updatePrintProgress(
                            sessionId,
                            SessionState.PRINTED,
                            PrintStatus.PRINTED
                        )
                    }
                    is PrintResult.Failure -> {
                        printPhase.value = PrintPhase.FAILED
                        errorMessage.value = result.message
                        sessionRepository.updatePrintProgress(
                            sessionId,
                            SessionState.ERROR,
                            PrintStatus.FAILED
                        )
                    }
                }
            } catch (e: Exception) {
                printPhase.value = PrintPhase.FAILED
                errorMessage.value = e.message ?: "Unable to print"
                sessionRepository.updatePrintProgress(
                    sessionId,
                    SessionState.ERROR,
                    PrintStatus.FAILED
                )
            }
        }
    }

    /** Closes this session and opens a fresh one, same template/photo mode (spec section 22). */
    fun nextCustomer(onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            val nextId = sessionRepository.nextCustomer(sessionId)
            if (nextId >= 0) onStarted(nextId)
        }
    }
}
