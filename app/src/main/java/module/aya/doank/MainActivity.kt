package module.aya.doank

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current

    // Set Up Permission Request Launcher
    val permissionsToRequest = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasLocationPermission) {
            launcher.launch(permissionsToRequest)
        }
    }

    // Coordinates State (Jakarta Default)
    val defaultLocation = LatLng(-6.2088, 106.8456)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }

    // Ambil koordinat saat map dipindah/geser
    val currentLatLng = cameraPositionState.position.target

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Google Map (Full Width & Height)
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = true
            ),
            properties = MapProperties(
                isMyLocationEnabled = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            )
        )

        // 2. Fixed Center Marker Pin (Selalu di tengah layar)
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Center Pin",
            tint = Color.Red,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
        )

        // 3. Chip Koordinat di Atas Tengah Layar
        ElevatedAssistChip(
            onClick = { },
            label = {
                Text(
                    text = "Lat: ${String.format("%.5f", currentLatLng.latitude)}, " +
                           "Lng: ${String.format("%.5f", currentLatLng.longitude)}",
                    fontSize = 13.sp,
                    style = MaterialTheme.typography.labelMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        )
    }
}
