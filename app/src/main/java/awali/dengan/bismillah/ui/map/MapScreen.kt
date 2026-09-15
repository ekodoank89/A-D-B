package awali.dengan.bismillah.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import awali.dengan.bismillah.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private val DEFAULT_CENTER = LatLng(-6.2088, 106.8456) // Monas, Jakarta
private const val DEFAULT_ZOOM = 17f
private val PIN_SIZE = 40.dp

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val locationGranted = rememberAppPermissions()
    val scope = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_CENTER, DEFAULT_ZOOM)
    }

    // Target kamera = titik tengah layar (posisi pin). Live mengikuti pergeseran map.
    val target = cameraPositionState.position.target

    // ===== Status play/stop =====
    var grbPlaying by remember { mutableStateOf(false) }
    var gjkPlaying by remember { mutableStateOf(false) }

    // ===== Koordinat TERKUNCI saat tombol di-PLAY =====
    // null = stop / belum pernah play (marker hilang + chip kosong)
    var grbCoord by remember { mutableStateOf<LatLng?>(null) }
    var gjkCoord by remember { mutableStateOf<LatLng?>(null) }

    // Animasi kamera (pin tengah) menuju koordinat marker
    fun flyTo(coord: LatLng) {
        scope.launch {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    coord,
                    cameraPositionState.position.zoom // pertahankan zoom saat ini
                )
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        // ===== Google Map full width + marker GRB/GJK =====
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = remember { MapUiSettings(zoomControlsEnabled = false) },
            properties = remember(locationGranted) {
                MapProperties(isMyLocationEnabled = locationGranted)
            }
        ) {
            // Marker GRB — muncul saat PLAY, hilang saat STOP
            grbCoord?.let { coord ->
                Marker(
                    state = rememberMarkerState(key = "grb_$coord", position = coord),
                    title = "GRB",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
            }
            // Marker GJK — muncul saat PLAY, hilang saat STOP
            gjkCoord?.let { coord ->
                Marker(
                    state = rememberMarkerState(key = "gjk_$coord", position = coord),
                    title = "GJK",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                )
            }
        }

        // ===== Pin tetap di tengah layar =====
        CenterPin(modifier = Modifier.align(Alignment.Center))

        // ===== Panel chip koordinat: PIN + GRB + GJK (atas-tengah, center) =====
        CoordinatePanel(
            pinCoord = target,
            grbCoord = grbCoord,
            grbPlaying = grbPlaying,
            gjkCoord = gjkCoord,
            gjkPlaying = gjkPlaying,
            onGrbChipClick = { grbCoord?.let { flyTo(it) } },
            onGjkChipClick = { gjkCoord?.let { flyTo(it) } },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 4.dp)
        )

        // ===== Panel tombol vertikal (moveable + lock) =====
        PlayControlPanel(
            grbPlaying = grbPlaying,
            gjkPlaying = gjkPlaying,
            onGrbToggle = {
                if (!grbPlaying) {
                    // PLAY: kunci koordinat tengah layar saat ini -> chip + marker
                    grbCoord = cameraPositionState.position.target
                    grbPlaying = true
                } else {
                    // STOP: hapus marker + kosongkan chip
                    grbCoord = null
                    grbPlaying = false
                }
            },
            onGjkToggle = {
                if (!gjkPlaying) {
                    // PLAY: kunci koordinat tengah layar saat ini -> chip + marker
                    gjkCoord = cameraPositionState.position.target
                    gjkPlaying = true
                } else {
                    // STOP: hapus marker + kosongkan chip
                    gjkCoord = null
                    gjkPlaying = false
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )
    }
}

// =====================================================================
// Pin tengah
// =====================================================================

@Composable
private fun CenterPin(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_pin),
        contentDescription = null,
        tint = Color(0xFFE53935),
        modifier = modifier
            .size(PIN_SIZE)
            .offset(y = -(PIN_SIZE / 2))
    )
}

// =====================================================================
// Panel chip koordinat (PIN + GRB + GJK)
//  - Chip wrap-content (tidak melebar rapat kanan-kiri), center di panel
//  - Tap PIN  = collapse jadi icon mata
//  - Tap GRB/GJK = pin/kamera animasi menuju markernya
// =====================================================================

@Composable
private fun CoordinatePanel(
    pinCoord: LatLng,
    grbCoord: LatLng?,
    grbPlaying: Boolean,
    gjkCoord: LatLng?,
    gjkPlaying: Boolean,
    onGrbChipClick: () -> Unit,
    onGjkChipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }

    if (expanded) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 2.dp,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally // chip center, wrap-content
            ) {
                // Chip PIN — tap = collapse semua chip menjadi icon mata
                ChipRow(
                    leading = {
                        Icon(
                            painter = painterResource(R.drawable.ic_pin),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    label = "PIN",
                    text = formatLatLng(pinCoord),
                    onClick = { expanded = false }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Chip GRB — tap = pin menuju marker GRB
                ChipRow(
                    leading = { StatusDot(grbPlaying) },
                    label = "GRB",
                    text = formatCoord(grbCoord),
                    enabled = grbCoord != null, // aktif hanya jika ada marker
                    onClick = onGrbChipClick
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Chip GJK — tap = pin menuju marker GJK
                ChipRow(
                    leading = { StatusDot(gjkPlaying) },
                    label = "GJK",
                    text = formatCoord(gjkCoord),
                    enabled = gjkCoord != null, // aktif hanya jika ada marker
                    onClick = onGjkChipClick
                )
            }
        }
    } else {
        // State collapsed: icon mata — tap = tampilkan chip lagi
        Surface(
            modifier = modifier
                .clip(CircleShape)
                .clickable { expanded = true },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 2.dp,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_eye),
                contentDescription = "Tampilkan chip koordinat",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(8.dp)
                    .size(20.dp)
            )
        }
    }
}

@Composable
private fun ChipRow(
    leading: @Composable () -> Unit,
    label: String,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        // wrap-content: tidak fillMaxWidth, tidak rapat kanan-kiri
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun StatusDot(playing: Boolean) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(
                color = if (playing) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
    )
}

// =====================================================================
// Panel kontrol — susunan vertikal:
//   [tombol GRB] -> [label GRB] -> [lock/unlock] -> [label GJK] -> [tombol GJK]
// Movable (drag) dengan tombol lock/unlock.
// =====================================================================

@Composable
private fun PlayControlPanel(
    grbPlaying: Boolean,
    gjkPlaying: Boolean,
    onGrbToggle: () -> Unit,
    onGjkToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var locked by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    Surface(
        modifier = modifier
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .then(
                if (!locked) {
                    Modifier.pointerInput(locked) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount
                        }
                    }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(
            1.dp,
            if (locked) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Tombol play/stop GRB
            PlayCircleButton(
                playing = grbPlaying,
                activeColor = Color(0xFFE53935),
                contentDesc = if (grbPlaying) "Stop GRB" else "Play GRB",
                onClick = onGrbToggle
            )

            Spacer(Modifier.height(5.dp))

            // 2. Label GRB (di bawah tombol)
            PlayLabel(
                text = "GRB",
                playing = grbPlaying,
                activeColor = Color(0xFFE53935)
            )

            Spacer(Modifier.height(8.dp))

            // 3. Lock/unlock movable (di tengah)
            IconButton(
                onClick = { locked = !locked },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (locked) R.drawable.ic_lock else R.drawable.ic_unlock
                    ),
                    contentDescription = if (locked) "Buka kunci" else "Kunci posisi",
                    tint = if (locked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // 4. Label GJK (di atas tombol)
            PlayLabel(
                text = "GJK",
                playing = gjkPlaying,
                activeColor = Color(0xFF1E88E5)
            )

            Spacer(Modifier.height(5.dp))

            // 5. Tombol play/stop GJK
            PlayCircleButton(
                playing = gjkPlaying,
                activeColor = Color(0xFF1E88E5),
                contentDesc = if (gjkPlaying) "Stop GJK" else "Play GJK",
                onClick = onGjkToggle
            )
        }
    }
}

@Composable
private fun PlayCircleButton(
    playing: Boolean,
    activeColor: Color,
    contentDesc: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (playing) activeColor else MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(
                    if (playing) R.drawable.ic_stop else R.drawable.ic_play
                ),
                contentDescription = contentDesc,
                tint = if (playing) Color.White
                else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun PlayLabel(
    text: String,
    playing: Boolean,
    activeColor: Color
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = if (playing) activeColor else MaterialTheme.colorScheme.onSurface
    )
}

// =====================================================================
// Util
// =====================================================================

private fun formatLatLng(latLng: LatLng): String =
    String.format(Locale.US, "%.6f, %.6f", latLng.latitude, latLng.longitude)

private fun formatCoord(latLng: LatLng?): String =
    latLng?.let { formatLatLng(it) } ?: "--.------, --.------"

/**
 * Izin LOKASI + NOTIFIKASI saat pertama kali aplikasi dibuka.
 * POST_NOTIFICATIONS hanya di Android 13+ (API 33).
 */
@Composable
private fun rememberAppPermissions(): Boolean {
    val context = LocalContext.current

    var locationGranted by remember {
        mutableStateOf(
            isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    val permissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if ((result[Manifest.permission.ACCESS_FINE_LOCATION] ?: false) ||
            (result[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false)
        ) {
            locationGranted = true
        }
    }

    LaunchedEffect(Unit) {
        val notGranted = permissions.filterNot { isGranted(context, it) }
        if (notGranted.isNotEmpty()) {
            launcher.launch(notGranted.toTypedArray())
        }
    }

    return locationGranted
}

private fun isGranted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
