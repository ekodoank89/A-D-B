package awali.dengan.bismillah.ui.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import awali.dengan.bismillah.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
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
private const val MAX_ZOOM = 21f // zoomTo() otomatis clamp ke max map
private val PIN_SIZE = 40.dp

// ===== Warna =====
private val PIN_GREEN = Color(0xFF2E7D32) // pin tengah: HIJAU
private val GRB_RED = Color(0xFFE53935)   // marker GRB: MERAH
private val GJK_BLUE = Color(0xFF1E88E5)  // marker GJK: BIRU

// ===== Style gelap untuk Google Map =====
private val DARK_MAP_STYLE = """
    [
      {"elementType":"geometry","stylers":[{"color":"#242f3e"}]},
      {"elementType":"labels.text.fill","stylers":[{"color":"#746855"}]},
      {"elementType":"labels.text.stroke","stylers":[{"color":"#242f3e"}]},
      {"featureType":"administrative.locality","elementType":"labels.text.fill","stylers":[{"color":"#d59563"}]},
      {"featureType":"poi","elementType":"labels.text.fill","stylers":[{"color":"#d59563"}]},
      {"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#263c3f"}]},
      {"featureType":"poi.park","elementType":"labels.text.fill","stylers":[{"color":"#6b9a76"}]},
      {"featureType":"road","elementType":"geometry","stylers":[{"color":"#38414e"}]},
      {"featureType":"road","elementType":"geometry.stroke","stylers":[{"color":"#212a37"}]},
      {"featureType":"road","elementType":"labels.text.fill","stylers":[{"color":"#9ca5b3"}]},
      {"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#746855"}]},
      {"featureType":"road.highway","elementType":"geometry.stroke","stylers":[{"color":"#1f2835"}]},
      {"featureType":"road.highway","elementType":"labels.text.fill","stylers":[{"color":"#f3d19c"}]},
      {"featureType":"transit","elementType":"geometry","stylers":[{"color":"#2f3948"}]},
      {"featureType":"transit.station","elementType":"labels.text.fill","stylers":[{"color":"#d59563"}]},
      {"featureType":"water","elementType":"geometry","stylers":[{"color":"#17263c"}]},
      {"featureType":"water","elementType":"labels.text.fill","stylers":[{"color":"#515c6d"}]},
      {"featureType":"water","elementType":"labels.text.stroke","stylers":[{"color":"#17263c"}]}
    ]
""".trimIndent()

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val locationGranted = rememberAppPermissions()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // ===== Mode gelap (map style + tema panel UI) =====
    var darkMode by remember { mutableStateOf(false) }

    // Override tema lokal: semua panel mengikuti mode terang/gelap
    val colorScheme = remember(darkMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (darkMode) darkColorScheme() else lightColorScheme()
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_CENTER, DEFAULT_ZOOM)
    }

    // Target kamera = titik tengah layar (posisi pin). Live mengikuti pergeseran map.
    val target = cameraPositionState.position.target

    // Ikon marker berbentuk pin (aman: fallback ke defaultMarker jika render gagal)
    val grbMarkerIcon = rememberPinMarkerIcon(
        tint = GRB_RED,
        fallbackHue = BitmapDescriptorFactory.HUE_RED
    )
    val gjkMarkerIcon = rememberPinMarkerIcon(
        tint = GJK_BLUE,
        fallbackHue = BitmapDescriptorFactory.HUE_BLUE
    )

    // ===== Status play/stop =====
    var grbPlaying by remember { mutableStateOf(false) }
    var gjkPlaying by remember { mutableStateOf(false) }

    // ===== Koordinat TERKUNCI saat tombol di-PLAY =====
    var grbCoord by remember { mutableStateOf<LatLng?>(null) }
    var gjkCoord by remember { mutableStateOf<LatLng?>(null) }

    // Animasi kamera (pin tengah) menuju koordinat
    fun flyTo(coord: LatLng) {
        scope.launch {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    coord,
                    cameraPositionState.position.zoom
                )
            )
        }
    }

    // ===== Autofocus: animasi kamera ke lokasi terakhir perangkat =====
    fun autoFocus() {
        if (!locationGranted) {
            Toast.makeText(context, "Izin lokasi belum diberikan", Toast.LENGTH_SHORT).show()
            return
        }
        val ok = runCatching {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (loc != null) {
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(loc.latitude, loc.longitude),
                            cameraPositionState.position.zoom
                        )
                    )
                }
                true
            } else {
                false
            }
        }.getOrDefault(false)

        if (!ok) {
            Toast.makeText(context, "Lokasi belum tersedia, coba lagi", Toast.LENGTH_SHORT).show()
        }
    }

    // ===== Zoom IN: sekali tap langsung ke zoom MAKSIMAL =====
    fun zoomInMax() {
        scope.launch {
            cameraPositionState.animate(CameraUpdateFactory.zoomTo(MAX_ZOOM))
        }
    }

    // ===== Zoom OUT: mundur 2 level per tap =====
    fun zoomOut() {
        scope.launch {
            cameraPositionState.animate(CameraUpdateFactory.zoomBy(-2f))
        }
    }

    MaterialTheme(colorScheme = colorScheme) {
        Box(modifier = modifier.fillMaxSize()) {

            // ===== Google Map full width + marker GRB/GJK =====
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = remember { MapUiSettings(zoomControlsEnabled = false) },
                properties = remember(locationGranted, darkMode) {
                    MapProperties(
                        isMyLocationEnabled = locationGranted,
                        mapStyleOptions = if (darkMode) MapStyleOptions(DARK_MAP_STYLE) else null
                    )
                }
            ) {
                // Marker GRB — pin MERAH, muncul saat PLAY, hilang saat STOP
                grbCoord?.let { coord ->
                    Marker(
                        state = rememberMarkerState(key = "grb_$coord", position = coord),
                        title = "GRB",
                        icon = grbMarkerIcon,
                        anchor = Offset(0.5f, 1.0f)
                    )
                }
                // Marker GJK — pin BIRU, muncul saat PLAY, hilang saat STOP
                gjkCoord?.let { coord ->
                    Marker(
                        state = rememberMarkerState(key = "gjk_$coord", position = coord),
                        title = "GJK",
                        icon = gjkMarkerIcon,
                        anchor = Offset(0.5f, 1.0f)
                    )
                }
            }

            // ===== Pin HIJAU tetap di tengah layar =====
            CenterPin(modifier = Modifier.align(Alignment.Center))

            // ===== Panel chip koordinat: PIN + GRB + GJK (atas-tengah, wrap-content) =====
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

            // ===== Panel tombol GRB/GJK (moveable + lock) =====
            PlayControlPanel(
                grbPlaying = grbPlaying,
                gjkPlaying = gjkPlaying,
                onGrbToggle = {
                    if (!grbPlaying) {
                        grbCoord = cameraPositionState.position.target
                        grbPlaying = true
                    } else {
                        grbCoord = null
                        grbPlaying = false
                    }
                },
                onGjkToggle = {
                    if (!gjkPlaying) {
                        gjkCoord = cameraPositionState.position.target
                        gjkPlaying = true
                    } else {
                        gjkCoord = null
                        gjkPlaying = false
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            )

            // ===== Panel utilitas icon-only (moveable + lock) =====
            UtilityPanel(
                darkMode = darkMode,
                onAutoFocus = { autoFocus() },
                onToggleDark = { darkMode = !darkMode },
                onZoomIn = { zoomInMax() },
                onZoomOut = { zoomOut() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 16.dp)
            )
        }
    }
}

// =====================================================================
// Ikon marker pin-shape — AMAN dari FC (runCatching + fallback defaultMarker)
// =====================================================================

@Composable
private fun rememberPinMarkerIcon(tint: Color, fallbackHue: Float): BitmapDescriptor {
    val context = LocalContext.current
    return remember(tint) {
        runCatching {
            MapsInitializer.initialize(context.applicationContext)
            val density = context.resources.displayMetrics.density
            val sizePx = (34 * density).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_pin)!!
            drawable.setTint(tint.toArgb())
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }.getOrElse {
            BitmapDescriptorFactory.defaultMarker(fallbackHue)
        }
    }
}

// =====================================================================
// Pin tengah — HIJAU
// =====================================================================

@Composable
private fun CenterPin(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_pin),
        contentDescription = null,
        tint = PIN_GREEN,
        modifier = modifier
            .size(PIN_SIZE)
            .offset(y = -(PIN_SIZE / 2))
    )
}

// =====================================================================
// Panel chip koordinat (PIN + GRB + GJK) — wrap-content + IntrinsicSize.Max
// Tap PIN = collapse; tap GRB/GJK = fly ke marker
// Icon chip GRB = pin MERAH, GJK = pin BIRU (penuh saat play, redup saat stop)
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
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ChipRow(
                    leading = {
                        Icon(
                            painter = painterResource(R.drawable.ic_pin),
                            contentDescription = null,
                            tint = PIN_GREEN,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    label = "PIN",
                    text = formatLatLng(pinCoord),
                    onClick = { expanded = false }
                )

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                ChipRow(
                    leading = {
                        Icon(
                            painter = painterResource(R.drawable.ic_pin),
                            contentDescription = null,
                            tint = if (grbPlaying) GRB_RED else GRB_RED.copy(alpha = 0.4f),
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    label = "GRB",
                    text = formatCoord(grbCoord),
                    enabled = grbCoord != null,
                    onClick = onGrbChipClick
                )

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                ChipRow(
                    leading = {
                        Icon(
                            painter = painterResource(R.drawable.ic_pin),
                            contentDescription = null,
                            tint = if (gjkPlaying) GJK_BLUE else GJK_BLUE.copy(alpha = 0.4f),
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    label = "GJK",
                    text = formatCoord(gjkCoord),
                    enabled = gjkCoord != null,
                    onClick = onGjkChipClick
                )
            }
        }
    } else {
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
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp),
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

// =====================================================================
// Panel tombol GRB/GJK — vertikal: [GRB] [label] [lock] [label] [GJK], movable
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
            PlayCircleButton(
                playing = grbPlaying,
                activeColor = GRB_RED,
                contentDesc = if (grbPlaying) "Stop GRB" else "Play GRB",
                onClick = onGrbToggle
            )

            Spacer(Modifier.height(5.dp))

            PlayLabel(text = "GRB", playing = grbPlaying, activeColor = GRB_RED)

            Spacer(Modifier.height(8.dp))

            LockButton(locked = locked, onToggle = { locked = !locked })

            Spacer(Modifier.height(8.dp))

            PlayLabel(text = "GJK", playing = gjkPlaying, activeColor = GJK_BLUE)

            Spacer(Modifier.height(5.dp))

            PlayCircleButton(
                playing = gjkPlaying,
                activeColor = GJK_BLUE,
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
// Panel utilitas — ICON-ONLY (tanpa label), vertikal:
//   [Autofocus] -> [Terang/Gelap] -> [lock/unlock] -> [+] -> [-]
// Movable (drag) dengan lock independen dari panel play.
// =====================================================================

@Composable
private fun UtilityPanel(
    darkMode: Boolean,
    onAutoFocus: () -> Unit,
    onToggleDark: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Autofocus (icon saja)
            UtilityButton(
                iconRes = R.drawable.ic_my_location,
                contentDesc = "Autofocus",
                active = false,
                onClick = onAutoFocus
            )

            Spacer(Modifier.height(8.dp))

            // 2. Terang/Gelap (icon saja)
            UtilityButton(
                iconRes = R.drawable.ic_brightness,
                contentDesc = "Terang/Gelap",
                active = darkMode,
                onClick = onToggleDark
            )

            Spacer(Modifier.height(8.dp))

            // 3. Lock/unlock movable panel ini
            LockButton(locked = locked, onToggle = { locked = !locked })

            Spacer(Modifier.height(8.dp))

            // 4. Zoom In (icon +, tap = langsung zoom maksimal)
            UtilityButton(
                iconRes = R.drawable.ic_plus,
                contentDesc = "Zoom In",
                active = false,
                onClick = onZoomIn
            )

            Spacer(Modifier.height(8.dp))

            // 5. Zoom Out (icon -, mundur 2 level)
            UtilityButton(
                iconRes = R.drawable.ic_minus,
                contentDesc = "Zoom Out",
                active = false,
                onClick = onZoomOut
            )
        }
    }
}

// Tombol bulat icon-only (tanpa label)
@Composable
private fun UtilityButton(
    iconRes: Int,
    contentDesc: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (active) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDesc,
                tint = if (active) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun LockButton(locked: Boolean, onToggle: () -> Unit) {
    IconButton(
        onClick = onToggle,
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
}

// =====================================================================
// Util
// =====================================================================

private fun formatLatLng(latLng: LatLng): String =
    String.format(Locale.US, "%.6f, %.6f", latLng.latitude, latLng.longitude)

private fun formatCoord(latLng: LatLng?): String =
    latLng?.let { formatLatLng(it) } ?: "--.------, --.------"

// =====================================================================
// Sistem izin BERURUTAN + DOUBLE CHECK:
//   1. LOKASI (foreground: fine + coarse)
//   2. LOKASI "Selalu izinkan" (background) — hanya jika foreground granted
//   3. NOTIFIKASI (Android 13+)
//   4. BATERAI "Tanpa pembatasan" (dialog Doze exemption)
// Dijalankan setiap aplikasi dibuka; jika ada izin hilang saat kembali
// ke aplikasi (ON_RESUME), urutan dijalankan ulang.
// =====================================================================

private enum class PermissionStep { LOCATION, BACKGROUND, NOTIFICATION, BATTERY, DONE }

@Composable
private fun rememberAppPermissions(): Boolean {
    val context = LocalContext.current

    // Status izin lokasi (dipakai layer lokasi biru di map)
    var locationGranted by remember {
        mutableStateOf(
            isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    // Step urutan izin (state machine)
    var step by remember { mutableStateOf(PermissionStep.LOCATION) }

    val lifecycleOwner = LocalLifecycleOwner.current

    // ===== Double check: setiap kembali ke aplikasi, verifikasi ulang =====
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                locationGranted =
                    isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)

                val anyMissing = !locationGranted ||
                    !isGranted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !isGranted(context, Manifest.permission.POST_NOTIFICATIONS)) ||
                    !isIgnoringBatteryOptimizations(context)

                if (anyMissing && step == PermissionStep.DONE) {
                    step = PermissionStep.LOCATION // jalankan ulang urutan
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 1) Launcher LOKASI foreground
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        locationGranted =
            (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
            (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        step = PermissionStep.BACKGROUND
    }

    // 2) Launcher LOKASI "Selalu izinkan" (background)
    val bgLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        step = PermissionStep.NOTIFICATION
    }

    // 3) Launcher NOTIFIKASI
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        step = PermissionStep.BATTERY
    }

    // 4) Launcher BATERAI (dialog "Tanpa pembatasan")
    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        step = PermissionStep.DONE
    }

    // Jalankan step berikutnya (urutan: Lokasi -> Selalu -> Notifikasi -> Baterai)
    LaunchedEffect(step) {
        when (step) {
            PermissionStep.LOCATION -> {
                if (locationGranted) {
                    step = PermissionStep.BACKGROUND
                } else {
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }

            PermissionStep.BACKGROUND -> {
                // Background hanya valid jika foreground sudah granted (aturan Android 11+)
                if (!locationGranted ||
                    isGranted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                ) {
                    step = PermissionStep.NOTIFICATION
                } else {
                    bgLocationLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    )
                }
            }

            PermissionStep.NOTIFICATION -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    isGranted(context, Manifest.permission.POST_NOTIFICATIONS)
                ) {
                    step = PermissionStep.BATTERY
                } else {
                    notifLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                }
            }

            PermissionStep.BATTERY -> {
                if (isIgnoringBatteryOptimizations(context)) {
                    step = PermissionStep.DONE
                } else {
                    batteryLauncher.launch(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                }
            }

            PermissionStep.DONE -> { /* Semua izin selesai diverifikasi */ }
        }
    }

    return locationGranted
}

private fun isGranted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun isIgnoringBatteryOptimizations(context: Context): Boolean =
    runCatching {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isIgnoringBatteryOptimizations(context.packageName)
    }.getOrDefault(false)
