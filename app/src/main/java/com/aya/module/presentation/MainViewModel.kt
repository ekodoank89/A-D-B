package com.aya.module.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel : ViewModel() {
    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    fun processIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.CheckStatus -> {
                _state.update { it.copy(isModuleActive = true) }
            }
            is MainIntent.ToggleState -> {
                _state.update { it.copy(isModuleActive = intent.active) }
            }
        }
    }
}
