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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose,heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.unit.Dp
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
import kotlin.random.Random

private val DEFAULT_CENTER = LatLng(-6.2088, 106.8456)
private const val DEFAULT_ZOOM = 17f
private const val MAX_ZOOM = 21f
private val PIN_SIZE = 40.dp

private const val PREFS_NAME = "adb_persistent_state"
private const val KEY_CAM_LAT = "cam_lat"
private const val KEY_CAM_LNG = "cam_lng"
private const val KEY_CAM_ZOOM = "cam_zoom"

// Warna
private val PIN_GREEN = Color(0xFF2E7D32)
private val GRB_RED = Color(0xFFE53935)
private val GJK_BLUE = Color(0xFF1E88E5)
private val FAV_GOLD = Color(0xFFFFB300)

private const val JITTER_MAX_DEG = 0.00005

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
      {"featureName":"road.highway","elementType":"geometry","stylers":[{"color":"#746855"}]},
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

// =====================================================================
// Model Favorite
// =====================================================================

private data class FavItem(
    val id: Long,
    val name: String,
    val lat: Double,
    val lng: Double
)

private enum class FavTab { GRB, GJK }

// =====================================================================
// Penyimpanan favorite — list per tab (GRB/GJK), JSON sederhana
// =====================================================================

private object FavStore {

    private const val KEY_GRB = "fav_list_grb"
    private const val KEY_GJK = "fav_list_gjk"
    private const val KEY_LAST_TAB = "fav_last_tab"

    private fun keyOf(tab: FavTab) = if (tab == FavTab.GRB) KEY_GRB else KEY_GJK

    fun loadList(prefs: android.content.SharedPreferences, tab: FavTab): List<FavItem> {
        val raw = prefs.getString(keyOf(tab), null) ?: return emptyList()
        return runCatching {
            raw.split(";").filter { it.isNotBlank() }.map { entry ->
                val p = entry.split("|")
                FavItem(p[0].toLong(), p[1], p[2].toDouble(), p[3].toDouble())
            }
        }.getOrDefault(emptyList())
    }

    fun saveList(prefs: android.content.SharedPreferences, tab: FavTab, list: List<FavItem>) {
        val raw = list.joinToString(";") { "${it.id}|${it.name}|${it.lat}|${it.lng}" }
        prefs.edit().putString(keyOf(tab), raw).apply()
    }

    fun lastTab(prefs: android.content.SharedPreferences): FavTab =
        if (prefs.getString(KEY_LAST_TAB, "GRB") == "GJK") FavTab.GJK else FavTab.GRB

    fun saveLastTab(prefs: android.content.SharedPreferences, tab: FavTab) {
        prefs.edit().putString(KEY_LAST_TAB, if (tab == FavTab.GJK) "GJK" else "GRB").apply()
    }
}

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val locationGranted = rememberAppPermissions()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var darkMode by rememberPersistentBoolean("dark_mode", false)

    val colorScheme = remember(darkMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (darkMode) darkColorScheme() else lightColorScheme()
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position =
            if (prefs.contains(KEY_CAM_LAT) && prefs.contains(KEY_CAM_LNG)) {
                CameraPosition.fromLatLngZoom(
                    LatLng(
                        Double.fromBits(prefs.getLong(KEY_CAM_LAT, 0L)),
                        Double.fromBits(prefs.getLong(KEY_CAM_LNG, 0L))
                    ),
                    prefs.getFloat(KEY_CAM_ZOOM, DEFAULT_ZOOM)
                )
            } else {
                CameraPosition.fromLatLngZoom(DEFAULT_CENTER, DEFAULT_ZOOM)
            }
    }

    val target = cameraPositionState.position.target

    val grbMarkerIcon = rememberPinMarkerIcon(GRB_RED, BitmapDescriptorFactory.HUE_RED)
    val gjkMarkerIcon = rememberPinMarkerIcon(GJK_BLUE, BitmapDescriptorFactory.HUE_BLUE)
    val favMarkerIcon = rememberPinMarkerIcon(FAV_GOLD, BitmapDescriptorFactory.HUE_YELLOW)

    var grbPlaying by rememberPersistentBoolean("grb_playing", false)
    var gjkPlaying by rememberPersistentBoolean("gjk_playing", false)
    var grbCoord by rememberPersistentLatLng("grb_coord")
    var gjkCoord by rememberPersistentLatLng("gjk_coord")

    // ===== Favorite dialog & state =====
    var showFavDialog by remember { mutableStateOf(false) }

    // Chip collapse tetap persisten
    var chipsExpanded by rememberPersistentBoolean("chips_expanded", true)

    var playLocked by rememberPersistentBoolean("play_locked", false)
    var playDragOffset by rememberPersistentOffset("play_drag", Offset.Zero)
    var utilLocked by rememberPersistentBoolean("util_locked", false)
    var utilDragOffset by rememberPersistentOffset("util_drag", Offset.Zero)

    var firstLaunchDone by rememberPersistentBoolean("first_launch_done", false)

    LaunchedEffect(cameraPositionState) {
        var lastSave = 0L
        snapshotFlow { cameraPositionState.position }.collect { pos ->
            val now = System.currentTimeMillis()
            if (now - lastSave >= 1000L) {
                lastSave = nextSave(now)
                prefs.edit()
                    .putLong(KEY_CAM_LAT, pos.target.latitude.toRawBits())
                    .putLong(KEY_CAM_LNG, pos.target.longitude.toRawBits())
                    .putFloat(KEY_CAM_ZOOM, pos.zoom)
                    .apply()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val pos = cameraPositionState.position
                prefs.edit()
                    .putLong(KEY_CAM_LAT, pos.target.latitude.toRawBits())
                    .putLong(KEY_CAM_LNG, pos.target.longitude.toRawBits())
                    .putFloat(KEY_CAM_ZOOM, pos.zoom)
                    .apply()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(locationGranted) {
        if (locationGranted && !firstLaunchDone) {
            val ok = runCatching {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                if (loc != null) {
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition(LatLng(loc.latitude, loc.longitude), DEFAULT_ZOOM, 0f, 0f)
                            )
                        )
                    }
                    true
                } else {
                    false
                }
            }.getOrDefault(false)
            if (ok) firstLaunchDone = true
        }
    }

    fun flyTo(coord: LatLng) {
        scope.launch {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(coord, cameraPositionState.position.zoom)
            )
        }
    }

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
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition(
                                LatLng(loc.latitude, loc.longitude),
                                cameraPositionState.position.zoom, 0f, 0f
                            )
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

    fun zoomInMax() {
        scope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomTo(MAX_ZOOM)) }
    }

    fun zoomOut() {
        scope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomBy(-2f)) }
    }

    MaterialTheme(colorScheme = colorScheme) {
        Box(modifier = modifier.fillMaxSize()) {

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = remember {
                    MapUiSettings(
                        zoomControlsEnabled = false,
                        compassEnabled = false,
                        myLocationButtonEnabled = false
                    )
                },
                properties = remember(locationGranted, darkMode) {
                    MapProperties(
                        isMyLocationEnabled = locationGranted,
                        mapStyleOptions = if (darkMode) MapStyleOptions(DARK_MAP_STYLE) else null
                    )
                }
            ) {
                grbCoord?.let { coord ->
                    Marker(
                        state = rememberMarkerState(key = "grb_$coord", position = coord),
                        title = "GRB",
                        icon = grbMarkerIcon,
                        anchor = Offset(0.5f, 1.0f)
                    )
                }
                gjkCoord?.let { coord ->
                    Marker(
                        state = rememberMarkerState(key = "gjk_$coord", per position = coord),
                        title = "GJK",
                        icon = gjkMarkerIcon,
                        anchor = Offset(0.5f, 1.0f)
                    )
                }
            }

            CenterPin(modifier = Modifier.align(Alignment.Center))

            CoordinatePanel(
                pinCoord = target,
                grbCoord = grbCoord,
                grbPlaying = grbPlaying,
                gjkCoord = gjkCoord,
                gjkPlaying = gjkPlaying,
                expanded = chipsExpanded,
                onExpandedChange = { chipsExpanded = it },
                onGrbChipClick = { grbCoord?.let { flyTo(it) } },
                onGjkChipClick = { pemGjkCoord?.let { flyTo(it) } },
                onFavChipClick = { showFavDialog = true },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 4.dp)
            )

            PlayControlPanel(
                grbPlaying = grbPlaying,
                gjkPlaying = gjkPlaying,
                onGrbToggle = {
                    if (!grbPlaying) {
                        val base = cameraPositionState.position.target
                        grbCoord = if (jitterEnabled) applyJitter(base) else base
                        grbPlaying = true
                    } else {
                        grbCoord = null
                        grbPlaying = false
                    }
                },
                ...
            )
        }
    }
}
