package com.aya.module

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

data class MapUiState(
    val currentPosition: LatLng = LatLng(-6.217274, 106.848446),
    val zoomLevel: Float = 16f,
    val isLocked: Boolean = false,
    val activePoint: String? = null
)

class MapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun setPointA() {
        _uiState.update { it.copy(activePoint = "A") }
    }

    fun setPointB() {
        _uiState.update { it.copy(activePoint = "B") }
    }

    fun toggleLock() {
        _uiState.update { it.copy(isLocked = !it.isLocked) }
    }

    fun applyJitter() {
        val current = _uiState.value.currentPosition
        val randomLat = current.latitude + (Random.nextDouble(-0.0005, 0.0005))
        val randomLng = current.longitude + (Random.nextDouble(-0.0005, 0.0005))
        
        _uiState.update { 
            it.copy(currentPosition = LatLng(randomLat, randomLng)) 
        }
    }
}
