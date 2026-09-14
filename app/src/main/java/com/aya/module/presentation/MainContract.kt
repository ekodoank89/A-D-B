package com.aya.module.presentation

data class MainState(
    val isModuleActive: Boolean = false,
    val isLoading: Boolean = false
)

sealed interface MainIntent {
    data object CheckStatus : MainIntent
    data class ToggleState(val active: Boolean) : MainIntent
}
