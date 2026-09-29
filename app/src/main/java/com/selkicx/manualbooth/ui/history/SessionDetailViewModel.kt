package com.selkicx.manualbooth.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selkicx.manualbooth.data.local.entity.FinalOutputEntity
import com.selkicx.manualbooth.data.local.entity.SessionEntity
import com.selkicx.manualbooth.data.local.entity.SessionPhotoEntity
import com.selkicx.manualbooth.data.repository.SessionRepository
import com.selkicx.manualbooth.data.repository.TemplateRepository
import com.selkicx.manualbooth.domain.adapters.PrintJob
import com.selkicx.manualbooth.domain.adapters.PrintResult
import com.selkicx.manualbooth.domain.adapters.PrinterAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ReprintPhase { IDLE, CONFIRMING, PRINTING, PRINTED, FAILED }

data class SessionDetailUiState(
    val session: SessionEntity? = null,
    val templateName: String = "",
    val printSizeName: String = "",
    val originals: List<SessionPhotoEntity> = emptyList(),
    val finalOutput: FinalOutputEntity? = null,
    val reprintPhase: ReprintPhase = ReprintPhase.IDLE,
    val errorMessage: String? = null
)

/**
 * Session History > session detail (spec section 48): view originals and
 * the Final Output, and reprint - which reuses the already-rendered
 * Final Output rather than re-rendering (spec section 49).
 */
class SessionDetailViewModel(
    private val sessionId: Long,
    private val sessionRepository: SessionRepository,
    private val templateRepository: TemplateRepository,
    private val printerAdapter: PrinterAdapter
) : ViewModel() {

    private val reprintPhase = MutableStateFlow(ReprintPhase.IDLE)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val templateName = MutableStateFlow("")
    private val printSizeName = MutableStateFlow("")
    private val finalOutput = MutableStateFlow<FinalOutputEntity?>(null)

    val uiState: StateFlow<SessionDetailUiState> = combine(
        sessionRepository.observeSession(sessionId),
        sessionRepository.observeSessionPhotos(sessionId),
        reprintPhase
    ) { session, photos, phase ->
        SessionDetailUiState(
            session = session,
            templateName = templateName.value,
            printSizeName = printSizeName.value,
            originals = photos,
            finalOutput = finalOutput.value,
            reprintPhase = phase,
            errorMessage = errorMessage.value
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionDetailUiState())

    init {
        viewModelScope.launch {
            val session = sessionRepository.observeSession(sessionId).filterNotNull().first()
            templateName.value = templateRepository.getTemplate(session.templateId)?.name ?: ""
            printSizeName.value = templateRepository.getPrintSize(session.printSizeId)?.name ?: ""
            finalOutput.value = sessionRepository.getFinalOutput(sessionId)
        }
    }

    fun requestReprint() {
        if (finalOutput.value != null) reprintPhase.value = ReprintPhase.CONFIRMING
    }

    fun cancelReprint() {
        reprintPhase.value = ReprintPhase.IDLE
    }

    /** Sends the existing Final Output file to the printer again - no rerender (spec section 49). */
    fun confirmReprint() {
        val output = finalOutput.value ?: return
        viewModelScope.launch {
            reprintPhase.value = ReprintPhase.PRINTING
            val result = printerAdapter.print(
                PrintJob(finalOutputPath = output.filePath, printSizeName = printSizeName.value, copies = 1)
            )
            reprintPhase.value = when (result) {
                is PrintResult.Success -> ReprintPhase.PRINTED
                is PrintResult.Failure -> {
                    errorMessage.value = result.message
                    ReprintPhase.FAILED
                }
            }
        }
    }
}
