package com.aya.module.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun MapScreen() {
    // Koordinat awal (misal: Jakarta)
    val defaultLocation = LatLng(-6.200000, 106.816666)
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }

    // Mendeteksi perubahan lokasi tengah saat peta bergeser
    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.position.target }
            .distinctUntilChanged()
            .collect { centerLatLng ->
                // Lokasi tengah peta saat ini:
                // centerLatLng.latitude, centerLatLng.longitude
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Google Maps Fullscreen
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false, // Menghilangkan tombol zoom agar tampilan bersih
                myLocationButtonEnabled = true
            )
        )

        // 2. Fixed Center Pin (Pin diam di tengah layar)
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Center Pin",
            tint = Color.Red,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
                // Menggeser padding bawah sedikit agar ujung bawah jarum pin tepat berada di titik tengah layar
                .padding(bottom = 24.dp) 
        )
    }
}
