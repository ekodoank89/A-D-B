package awali.dengan.bismillah.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import awali.dengan.bismillah.R
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale
import kotlin.math.roundToInt

private val DEFAULT_CENTER = LatLng(-6.2088, 106.8456) // Monas, Jakarta
private const val DEFAULT_ZOOM = 17f
private val PIN_SIZE = 40.dp

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val locationGranted = rememberAppPermissions()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_CENTER, DEFAULT_ZOOM)
    }

    // Target kamera = titik tengah layar (posisi pin). Live mengikuti pergeseran map.
    val target = cameraPositionState.position.target

    // ===== Status play/stop =====
    var grbPlaying by remember { mutableStateOf(false) }
    var gjkPlaying by remember { mutableStateOf(false) }

    // ===== Koordinat TERKUNCI pada saat tombol di-PLAY =====
    // null = belum pernah di-play
    var grbCoord by remember { mutableStateOf<LatLng?>(null) }
    var gjkCoord by remember { mutableStateOf<LatLng?>(null) }

    Box(modifier = modifier.fillMaxSize()) {

        // ===== Google Map full width =====
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = remember { MapUiSettings(zoomControlsEnabled = false) },
            properties = remember(locationGranted) {
                MapProperties(isMyLocationEnabled = locationGranted)
            }
        )

        // ===== Pin tetap di tengah layar =====
        CenterPin(modifier = Modifier.align(Alignment.Center))

        // ===== Panel chip koordinat: PIN + GRB + GJK (1 kontainer, atas-tengah, presisi) =====
        CoordinatePanel(
            pinCoord = target,
            grbCoord = grbCoord,
            grbPlaying = grbPlaying,
            gjkCoord = gjkCoord,
            gjkPlaying = gjkPlaying,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 4.dp) // padding kecil agar presisi
        )

        // ===== Panel tombol GRB & GJK vertikal (moveable + lock) =====
        PlayControlPanel(
            grbPlaying = grbPlaying,
            gjkPlaying = gjkPlaying,
            onGrbToggle = {
                if (!grbPlaying) {
                    // PLAY: kunci koordinat tengah layar SAAT INI
                    grbCoord = cameraPositionState.position.target
                    grbPlaying = true
                } else {
                    // STOP: koordinat terkunci tetap tersimpan di chip
                    grbPlaying = false
                }
            },
            onGjkToggle = {
                if (!gjkPlaying) {
                    // PLAY: kunci koordinat tengah layar SAAT INI
                    gjkCoord = cameraPositionState.position.target
                    gjkPlaying = true
                } else {
                    // STOP: koordinat terkunci tetap tersimpan di chip
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
// Panel chip koordinat (PIN + GRB + GJK) — tap chip PIN = collapse jadi icon mata
// =====================================================================

@Composable
private fun CoordinatePanel(
    pinCoord: LatLng,
    grbCoord: LatLng?,
    grbPlaying: Boolean,
    gjkCoord: LatLng?,
    gjkPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

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
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp) // rapat & presisi
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

                // Chip GRB — koordinat terkunci saat PLAY, tap = salin
                ChipRow(
                    leading = { StatusDot(grbPlaying) },
                    label = "GRB",
                    text = formatCoord(grbCoord),
                    onClick = {
                        val t = formatCoord(grbCoord)
                        clipboard.setText(AnnotatedString(t))
                        Toast.makeText(context, "GRB disalin: $t", Toast.LENGTH_SHORT).show()
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Chip GJK — koordinat terkunci saat PLAY, tap = salin
                ChipRow(
                    leading = { StatusDot(gjkPlaying) },
                    label = "GJK",
                    text = formatCoord(gjkCoord),
                    onClick = {
                        val t = formatCoord(gjkCoord)
                        clipboard.setText(AnnotatedString(t))
                        Toast.makeText(context, "GJK disalin: $t", Toast.LENGTH_SHORT).show()
                    }
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
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
// Panel kontrol GRB & GJK — susunan VERTIKAL:
//   [GRB label] -> [tombol GRB] -> [lock/unlock] -> [GJK label] -> [tombol GJK]
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), // rapat & presisi
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Tombol GRB (label di atas tombol)
            PlayButton(
                label = "GRB",
                playing = grbPlaying,
                activeColor = Color(0xFFE53935),
                onClick = onGrbToggle
            )

            // 2. Lock/unlock moveable (di antara GRB dan GJK)
            IconButton(
                onClick = { locked = !locked },
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (locked) R.drawable.ic_lock else R.drawable.ic_unlock
                    ),
                    contentDescription = if (locked) "Buka kunci" else "Kunci posisi",
                    tint = if (locked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
            }

            // 3. Tombol GJK (label di atas tombol)
            PlayButton(
                label = "GJK",
                playing = gjkPlaying,
                activeColor = Color(0xFF1E88E5),
                onClick = onGjkToggle
            )
        }
    }
}

@Composable
private fun PlayButton(
    label: String,
    playing: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Label di ATAS tombol
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (playing) activeColor else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(3.dp))
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
                    contentDescription = if (playing) "Stop $label" else "Play $label",
                    tint = if (playing) Color.White
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
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
