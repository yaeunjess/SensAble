package com.sensable.app.feature.kakaobank.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class KakaoBankViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(KakaoBankUiState())
    val uiState: StateFlow<KakaoBankUiState> = _uiState.asStateFlow()

    fun showBrailleInterface() {
        _uiState.update { it.copy(isBrailleVisible = true) }
    }

    fun hideBrailleInterface() {
        _uiState.update { it.copy(isBrailleVisible = false) }
    }
}

data class KakaoBankUiState(
    val isBrailleVisible: Boolean = false
)