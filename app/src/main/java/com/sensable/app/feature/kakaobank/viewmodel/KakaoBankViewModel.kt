package com.sensable.app.feature.kakaobank.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KakaoBankViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(KakaoBankUiState())
    val uiState: StateFlow<KakaoBankUiState> = _uiState.asStateFlow()

    init {
        // 시연을 위해 홈 화면 진입 후 3초 뒤에 바텀시트가 올라오도록 설정
        viewModelScope.launch {
            delay(2500)
            showBrailleInterface()
        }
    }

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
