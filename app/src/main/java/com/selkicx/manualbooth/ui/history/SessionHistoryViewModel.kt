package com.selkicx.manualbooth.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selkicx.manualbooth.data.repository.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/** Session History list (spec section 48), newest first. */
class SessionHistoryViewModel(sessionRepository: SessionRepository) : ViewModel() {
    val sessions = sessionRepository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
