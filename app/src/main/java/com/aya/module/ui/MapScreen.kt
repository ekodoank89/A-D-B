package com.aya.module.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aya.module.MapViewModel
import com.aya.module.ui.components.CoordinateBadge
import com.aya.module.ui.components.LeftControlPanel
import com.aya.module.ui.components.RightControlPanel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@Composable
fun MapScreen(viewModel: MapViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(uiState.currentPosition, uiState.zoomLevel)
    }

    LaunchedEffect(uiState.currentPosition) {
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLng(uiState.currentPosition)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = false
            )
        ) {
            Marker(
                state = MarkerState(position = uiState.currentPosition),
                title = "Lokasi"
            )
        }

        CoordinateBadge(
            position = cameraPositionState.position.target,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        )

        LeftControlPanel(
            isLocked = uiState.isLocked,
            onPointAClick = { viewModel.setPointA() },
            onPointBClick = { viewModel.setPointB() },
            onLockClick = { viewModel.toggleLock() },
            onFavClick = { },
            onJitterClick = { viewModel.applyJitter() },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
        )

        RightControlPanel(
            onFullscreenClick = { },
            onLockClick = { viewModel.toggleLock() },
            onZoomInClick = {
                coroutineScope.launch {
                    cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                }
            },
            onZoomOutClick = {
                coroutineScope.launch {
                    cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        )
    }
}
