package com.selkicx.manualbooth.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selkicx.manualbooth.data.repository.SessionRepository
import com.selkicx.manualbooth.domain.adapters.CameraAdapter
import com.selkicx.manualbooth.domain.adapters.CameraConnectionState
import com.selkicx.manualbooth.domain.adapters.PrinterAdapter
import com.selkicx.manualbooth.domain.adapters.PrinterConnectionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Backs the Home screen (spec section 5): live camera/printer status + recent sessions. */
class HomeViewModel(
    cameraAdapter: CameraAdapter,
    printerAdapter: PrinterAdapter,
    sessionRepository: SessionRepository
) : ViewModel() {

    val cameraState: StateFlow<CameraConnectionState> = cameraAdapter.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CameraConnectionState.DISCONNECTED)

    val printerState: StateFlow<PrinterConnectionState> = printerAdapter.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrinterConnectionState.DISCONNECTED)

    val recentSessions = sessionRepository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
