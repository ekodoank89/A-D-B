package awali.dengan.bismillah.ui.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import kotlin.random.Random

private val DEFAULT_CENTER = LatLng(-6.2088, 106.8456) // Monas, Jakarta
private const val DEFAULT_ZOOM = 17f
private const val MAX_ZOOM = 21f // zoomTo() otomatis clamp ke max map
private val PIN_SIZE = 40.dp

// ===== Penyimpanan state persisten (tahan force stop) =====
private const val PREFS_NAME = "adb_persistent_state"
private const val KEY_CAM_LAT = "cam_lat"
private const val KEY_CAM_LNG = "cam_lng"
private const val KEY_CAM_ZOOM = "cam_zoom"

// ===== Warna =====
private val PIN_GREEN = Color(0xFF2E7D32)  // pin tengah: HIJAU
private val GRB_RED = Color(0xFFE53935)    // marker GRB: MERAH
private val GJK_BLUE = Color(0xFF1E88E5)   // marker GJK: BIRU
private val FAV_GOLD = Color(0xFFFFB300)   // aksen favorite: EMAS

// ===== Jitter: offset acak maksimal (derajat) ~= +-5 meter =====
private const val JITTER_MAX_DEG = 0.00005

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

// =====================================================================
// Model & penyimpanan Favorite — list TERPISAH per tab (GRB/GJK)
// Encoding: "id|nama|lat|lng", antar item dipisah ";"
// Juga menyimpan: tab terakhir + section (Dari Pin/Manual) terakhir.
// =====================================================================

private data class FavItem(
    val id: Long,
    val name: String,
    val lat: Double,
    val lng: Double
)

private enum class FavTab { GRB, GJK }

private object FavStore {

    private const val KEY_GRB = "fav_list_grb"
    private const val KEY_GJK = "fav_list_gjk"
    private const val KEY_LAST_TAB = "fav_last_tab"
    private const val KEY_LAST_SECTION = "fav_last_section"

    private fun keyOf(tab: FavTab) = if (tab == FavTab.GRB) KEY_GRB else KEY_GJK

    fun loadList(prefs: SharedPreferences, tab: FavTab): List<FavItem> {
        val raw = prefs.getString(keyOf(tab), null) ?: return emptyList()
        return runCatching {
            raw.split(";")
                .filter { it.isNotBlank() }
                .mapNotNull { entry ->
                    val p = entry.split("|")
                    if (p.size < 4) return@mapNotNull null
                    val id = p[0].toLongOrNull() ?: return@mapNotNull null
                    val lat = p[2].toDoubleOrNull() ?: return@mapNotNull null
                    val lng = p[3].toDoubleOrNull() ?: return@mapNotNull null
                    FavItem(id, p[1], lat, lng)
                }
        }.getOrDefault(emptyList())
    }

    fun saveList(prefs: SharedPreferences, tab: FavTab, list: List<FavItem>) {
        val raw = list.joinToString(";") {
            "${it.id}|${it.name.replace("|", "/").replace(";", ",")}|${it.lat}|${it.lng}"
        }
        prefs.edit().putString(keyOf(tab), raw).apply()
    }

    // Tab terakhir yang di-tap — dibuka lagi di tab yang sama
    fun lastTab(prefs: SharedPreferences): FavTab =
        if (prefs.getString(KEY_LAST_TAB, "GRB") == "GJK") FavTab.GJK else FavTab.GRB

    fun saveLastTab(prefs: SharedPreferences, tab: FavTab) {
        prefs.edit().putString(KEY_LAST_TAB, if (tab == FavTab.GJK) "GJK" else "GRB").apply()
    }

    // Section (form) terakhir yang di-tap: "PIN" atau "MANUAL" — tahan force stop
    fun lastSection(prefs: SharedPreferences): String =
        prefs.getString(KEY_LAST_SECTION, "PIN") ?: "PIN"

    fun saveLastSection(prefs: SharedPreferences, section: String) {
        prefs.edit().putString(KEY_LAST_SECTION, section).apply()
    }
}

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val locationGranted = rememberAppPermissions()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    // ===== Mode gelap — PERSISTEN =====
    var darkMode by rememberPersistentBoolean("dark_mode", false)

    // Override tema lokal: semua panel mengikuti mode terang/gelap
    val colorScheme = remember(darkMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (darkMode) darkColorScheme() else lightColorScheme()
        }
    }

    // ===== Kamera: RESTORE posisi terakhir (tahan force stop) =====
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

    // Target kamera = titik tengah layar (posisi pin). Live mengikuti pergeseran map.
    val target = cameraPositionState.position.target

    // Ikon marker pin-shape (aman: fallback defaultMarker jika render gagal)
    val grbMarkerIcon = rememberPinMarkerIcon(GRB_RED, BitmapDescriptorFactory.HUE_RED)
    val gjkMarkerIcon = rememberPinMarkerIcon(GJK_BLUE, BitmapDescriptorFactory.HUE_BLUE)

    // ===== Status play/stop — PERSISTEN =====
    var grbPlaying by rememberPersistentBoolean("grb_playing", false)
    var gjkPlaying by rememberPersistentBoolean("gjk_playing", false)

    // ===== Koordinat TERKUNCI saat PLAY — PERSISTEN (marker ikut muncul lagi) =====
    var grbCoord by rememberPersistentLatLng("grb_coord")
    var gjkCoord by rememberPersistentLatLng("gjk_coord")

    // ===== Jitter — PERSISTEN =====
    var jitterEnabled by rememberPersistentBoolean("jitter_enabled", false)

    // ===== Chip expanded/collapsed — PERSISTEN =====
    var chipsExpanded by rememberPersistentBoolean("chips_expanded", true)

    // ===== Lock & posisi geser panel play — PERSISTEN =====
    var playLocked by rememberPersistentBoolean("play_locked", false)
    var playDragOffset by rememberPersistentOffset("play_drag", Offset.Zero)

    // ===== Lock & posisi geser panel utilitas — PERSISTEN =====
    var utilLocked by rememberPersistentBoolean("util_locked", false)
    var utilDragOffset by rememberPersistentOffset("util_drag", Offset.Zero)

    // ===== First launch: pin otomatis ke titik biru (sekali saja) =====
    var firstLaunchDone by rememberPersistentBoolean("first_launch_done", false)

    // ===== Favorite =====
    var showFavDialog by remember { mutableStateOf(false) }
    var grbFavs by remember { mutableStateOf(FavStore.loadList(prefs, FavTab.GRB)) }
    var gjkFavs by remember { mutableStateOf(FavStore.loadList(prefs, FavTab.GJK)) }

    // ===== Simpan posisi kamera tiap berubah (throttle 1 detik) =====
    LaunchedEffect(cameraPositionState) {
        var lastSave = 0L
        snapshotFlow { cameraPositionState.position }.collect { pos ->
            val now = System.currentTimeMillis()
            if (now - lastSave >= 1000L) {
                lastSave = now
                prefs.edit()
                    .putLong(KEY_CAM_LAT, pos.target.latitude.toRawBits())
                    .putLong(KEY_CAM_LNG, pos.target.longitude.toRawBits())
                    .putFloat(KEY_CAM_ZOOM, pos.zoom)
                    .apply()
            }
        }
    }

    // ===== Simpan final saat app ke background =====
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

    // ===== FIRST LAUNCH: pin animasi ke titik biru (sekali saja) =====
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
                                CameraPosition(
                                    LatLng(loc.latitude, loc.longitude),
                                    DEFAULT_ZOOM, 0f, 0f
                                )
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

    // ===== Autofocus + KOMPAS: ke lokasi perangkat + normalisasi rotasi map =====
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
                                cameraPositionState.position.zoom,
                                0f, // tilt dinormalkan
                                0f  // bearing dinormalkan (utara di atas)
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

    // ===== Zoom IN: sekali tap langsung ke zoom MAKSIMAL =====
    fun zoomInMax() {
        scope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomTo(MAX_ZOOM)) }
    }

    // ===== Zoom OUT: mundur 2 level per tap =====
    fun zoomOut() {
        scope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomBy(-2f)) }
    }

    // ===== Tambah favorite (dari pin atau manual) =====
    fun addFav(tab: FavTab, name: String, coord: LatLng) {
        val item = FavItem(
            id = System.currentTimeMillis(),
            name = name.trim(),
            lat = coord.latitude,
            lng = coord.longitude
        )
        if (tab == FavTab.GRB) {
            grbFavs = grbFavs + item
            FavStore.saveList(prefs, FavTab.GRB, grbFavs)
        } else {
            gjkFavs = gjkFavs + item
            FavStore.saveList(prefs, FavTab.GJK, gjkFavs)
        }
        Toast.makeText(context, "Favorite \"${item.name}\" disimpan", Toast.LENGTH_SHORT).show()
    }

    // ===== Update favorite (edit nama + koordinat) =====
    fun updateFav(tab: FavTab, id: Long, newName: String, newLat: Double, newLng: Double) {
        fun patch(list: List<FavItem>) = list.map {
            if (it.id == id) it.copy(name = newName.trim(), lat = newLat, lng = newLng) else it
        }
        if (tab == FavTab.GRB) {
            grbFavs = patch(grbFavs)
            FavStore.saveList(prefs, FavTab.GRB, grbFavs)
        } else {
            gjkFavs = patch(gjkFavs)
            FavStore.saveList(prefs, FavTab.GJK, gjkFavs)
        }
        Toast.makeText(context, "Favorite diperbarui", Toast.LENGTH_SHORT).show()
    }

    // ===== Hapus favorite =====
    fun deleteFav(tab: FavTab, id: Long) {
        if (tab == FavTab.GRB) {
            grbFavs = grbFavs.filterNot { it.id == id }
            FavStore.saveList(prefs, FavTab.GRB, grbFavs)
        } else {
            gjkFavs = gjkFavs.filterNot { it.id == id }
            FavStore.saveList(prefs, FavTab.GJK, gjkFavs)
        }
        Toast.makeText(context, "Favorite dihapus", Toast.LENGTH_SHORT).show()
    }

        MaterialTheme(colorScheme = colorScheme) {
        Box(modifier = modifier.fillMaxSize()) {

            // ===== Google Map full width + marker GRB/GJK (tanpa marker favorite) =====
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
                expanded = chipsExpanded,
                onExpandedChange = { chipsExpanded = it },
                onGrbChipClick = { grbCoord?.let { flyTo(it) } },
                onGjkChipClick = { gjkCoord?.let { flyTo(it) } },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 4.dp)
            )

            // ===== Panel tombol utama (favorite membuka dialog) =====
            PlayControlPanel(
                grbPlaying = grbPlaying,
                gjkPlaying = gjkPlaying,
                onGrbToggle = {
                    if (!grbPlaying) {
                        val base = cameraPositionState.position.target
                        // Jitter ON -> koordinat capture diberi offset acak kecil
                        grbCoord = if (jitterEnabled) applyJitter(base) else base
                        grbPlaying = true
                    } else {
                        grbCoord = null
                        grbPlaying = false
                    }
                },
                onGjkToggle = {
                    if (!gjkPlaying) {
                        val base = cameraPositionState.position.target
                        gjkCoord = if (jitterEnabled) applyJitter(base) else base
                        gjkPlaying = true
                    } else {
                        gjkCoord = null
                        gjkPlaying = false
                    }
                },
                favActive = grbFavs.isNotEmpty() || gjkFavs.isNotEmpty(),
                onFavClick = { showFavDialog = true },
                jitterEnabled = jitterEnabled,
                onJitterToggle = { jitterEnabled = !jitterEnabled },
                locked = playLocked,
                onLockedChange = { playLocked = it },
                dragOffset = playDragOffset,
                onDragOffsetChange = { playDragOffset = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            )

            // ===== Panel utilitas icon-only (moveable + lock, posisi persisten) =====
            UtilityPanel(
                darkMode = darkMode,
                onAutoFocus = { autoFocus() },
                onToggleDark = { darkMode = !darkMode },
                onZoomIn = { zoomInMax() },
                onZoomOut = { zoomOut() },
                locked = utilLocked,
                onLockedChange = { utilLocked = it },
                dragOffset = utilDragOffset,
                onDragOffsetChange = { utilDragOffset = it },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 16.dp)
            )

            // ===== Dialog Favorite =====
            if (showFavDialog) {
                FavoriteDialog(
                    prefs = prefs,
                    initialTab = FavStore.lastTab(prefs),
                    grbFavs = grbFavs,
                    gjkFavs = gjkFavs,
                    currentPin = target,
                    onDismiss = { showFavDialog = false },
                    onAdd = { tab, name, coord -> addFav(tab, name, coord) },
                    onUpdate = { tab, id, name, lat, lng -> updateFav(tab, id, name, lat, lng) },
                    onDelete = { tab, id -> deleteFav(tab, id) },
                    onFlyTo = { coord ->
                        showFavDialog = false
                        flyTo(coord)
                    }
                )
            }
        }
    }
}

// =====================================================================
// Jitter: offset acak kecil pada koordinat (lat & lng masing-masing
// digeser acak dalam rentang +-JITTER_MAX_DEG ~= +-5 meter)
// =====================================================================

private fun applyJitter(coord: LatLng): LatLng = LatLng(
    coord.latitude + Random.nextDouble(-JITTER_MAX_DEG, JITTER_MAX_DEG),
    coord.longitude + Random.nextDouble(-JITTER_MAX_DEG, JITTER_MAX_DEG)
)

// =====================================================================
// State PERSISTEN — disimpan ke SharedPreferences di setiap perubahan,
// dimuat ulang saat aplikasi dibuka (termasuk setelah force stop).
// LocalContext.current dibaca DI LUAR remember {} (aturan Compose).
// =====================================================================

@Composable
private fun rememberPersistentBoolean(key: String, default: Boolean): MutableState<Boolean> {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val state = remember { mutableStateOf(prefs.getBoolean(key, default)) }
    SideEffect {
        prefs.edit().putBoolean(key, state.value).apply()
    }
    return state
}

// LatLng? — null disimpan dengan menghapus key (chip kosong / marker hilang)
@Composable
private fun rememberPersistentLatLng(key: String): MutableState<LatLng?> {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val state = remember {
        mutableStateOf<LatLng?>(
            if (prefs.contains("${key}_lat") && prefs.contains("${key}_lng")) {
                LatLng(
                    Double.fromBits(prefs.getLong("${key}_lat", 0L)),
                    Double.fromBits(prefs.getLong("${key}_lng", 0L))
                )
            } else {
                null
            }
        )
    }
    SideEffect {
        prefs.edit().apply {
            val v = state.value
            if (v == null) {
                remove("${key}_lat")
                remove("${key}_lng")
            } else {
                putLong("${key}_lat", v.latitude.toRawBits())
                putLong("${key}_lng", v.longitude.toRawBits())
            }
        }.apply()
    }
    return state
}

// Offset (posisi geser panel)
@Composable
private fun rememberPersistentOffset(key: String, default: Offset): MutableState<Offset> {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val state = remember {
        mutableStateOf(
            if (prefs.contains("${key}_x") && prefs.contains("${key}_y")) {
                Offset(
                    prefs.getFloat("${key}_x", 0f),
                    prefs.getFloat("${key}_y", 0f)
                )
            } else {
                default
            }
        )
    }
    SideEffect {
        prefs.edit()
            .putFloat("${key}_x", state.value.x)
            .putFloat("${key}_y", state.value.y)
            .apply()
    }
    return state
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
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onGrbChipClick: () -> Unit,
    onGjkChipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    onClick = { onExpandedChange(false) }
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
                .clickable { onExpandedChange(true) },
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
// Separator sempit — lebar seukuran tombol play (46dp)
// =====================================================================

@Composable
private fun PanelDivider() {
    HorizontalDivider(
        modifier = Modifier.width(46.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}

// =====================================================================
// Panel tombol — favorite MEMBUKA DIALOG (bukan toggle marker).
// Urutan: [▶GRB] [GRB] [sep] [GJK] [▶GJK] [sep] [lock] [sep] [⭐] [sep] [Jitter]
// ⭐ = icon BINTANG EMAS; latar emas lembut jika ada favorite tersimpan.
// =====================================================================

@Composable
private fun PlayControlPanel(
    grbPlaying: Boolean,
    gjkPlaying: Boolean,
    onGrbToggle: () -> Unit,
    onGjkToggle: () -> Unit,
    favActive: Boolean,
    onFavClick: () -> Unit,
    jitterEnabled: Boolean,
    onJitterToggle: () -> Unit,
    locked: Boolean,
    onLockedChange: (Boolean) -> Unit,
    dragOffset: Offset,
    onDragOffsetChange: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDragOffset by rememberUpdatedState(dragOffset)
    val currentLocked by rememberUpdatedState(locked)

    Surface(
        modifier = modifier
            .offset { IntOffset(currentDragOffset.x.roundToInt(), currentDragOffset.y.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (!currentLocked) {
                        onDragOffsetChange(currentDragOffset + dragAmount)
                    }
                }
            },
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
                activeColor = GRB_RED,
                contentDesc = if (grbPlaying) "Stop GRB" else "Play GRB",
                onClick = onGrbToggle
            )

            Spacer(Modifier.height(5.dp))

            // 2. Label GRB
            PlayLabel(text = "GRB", playing = grbPlaying, activeColor = GRB_RED)

            Spacer(Modifier.height(8.dp))

            // 3. Separator
            PanelDivider()

            Spacer(Modifier.height(8.dp))

            // 4. Label GJK
            PlayLabel(text = "GJK", playing = gjkPlaying, activeColor = GJK_BLUE)

            Spacer(Modifier.height(5.dp))

            // 5. Tombol play/stop GJK
            PlayCircleButton(
                playing = gjkPlaying,
                activeColor = GJK_BLUE,
                contentDesc = if (gjkPlaying) "Stop GJK" else "Play GJK",
                onClick = onGjkToggle
            )

            Spacer(Modifier.height(8.dp))

            // 6. Separator
            PanelDivider()

            Spacer(Modifier.height(8.dp))

            // 7. Tombol lock/unlock movable
            LockButton(locked = locked, onToggle = { onLockedChange(!locked) })

            Spacer(Modifier.height(8.dp))

            // 8. Separator
            PanelDivider()

            Spacer(Modifier.height(8.dp))

            // 9. Tombol Favorite — icon BINTANG EMAS, buka dialog menu favorite
            UtilityButton(
                iconRes = R.drawable.ic_star,
                contentDesc = "Buka menu favorite",
                active = favActive,
                activeColor = FAV_GOLD.copy(alpha = 0.25f),
                iconTint = FAV_GOLD,
                size = 46.dp,
                iconSize = 22.dp,
                onClick = onFavClick
            )

            Spacer(Modifier.height(8.dp))

            // 10. Separator
            PanelDivider()

            Spacer(Modifier.height(8.dp))

            // 11. Tombol Jitter
            UtilityButton(
                iconRes = R.drawable.ic_jitter,
                contentDesc = if (jitterEnabled) "Jitter aktif" else "Jitter nonaktif",
                active = jitterEnabled,
                size = 46.dp,
                iconSize = 22.dp,
                onClick = onJitterToggle
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
// Panel utilitas — ICON-ONLY, vertikal:
//   [Autofocus+Kompas] -> [Terang/Gelap] -> [lock] -> [+] -> [-]
// Movable (drag) dengan lock independen dari panel utama.
// =====================================================================

@Composable
private fun UtilityPanel(
    darkMode: Boolean,
    onAutoFocus: () -> Unit,
    onToggleDark: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    locked: Boolean,
    onLockedChange: (Boolean) -> Unit,
    dragOffset: Offset,
    onDragOffsetChange: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDragOffset by rememberUpdatedState(dragOffset)
    val currentLocked by rememberUpdatedState(locked)

    Surface(
        modifier = modifier
            .offset { IntOffset(currentDragOffset.x.roundToInt(), currentDragOffset.y.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (!currentLocked) {
                        onDragOffsetChange(currentDragOffset + dragAmount)
                    }
                }
            },
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
            // 1. Autofocus + Kompas (icon saja)
            UtilityButton(
                iconRes = R.drawable.ic_my_location,
                contentDesc = "Autofocus & Normalisasi Map",
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
            LockButton(locked = locked, onToggle = { onLockedChange(!locked) })

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

// Tombol bulat icon-only — ukuran & warna icon parameter
@Composable
private fun UtilityButton(
    iconRes: Int,
    contentDesc: String,
    active: Boolean,
    onClick: () -> Unit,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    iconTint: Color? = null
) {
    Surface(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (active) activeColor else MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDesc,
                tint = iconTint ?: if (active) Color.White
                else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(iconSize)
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
// Dialog Favorite:
//  - 2 tab (GRB/GJK), tab terakhir di-tap DISIMPAN -> dibuka lagi nanti
//  - Accordion section (bisa di-tap): "Dari Pin" ⇄ "Manual"
//      * Tap label Dari Pin  -> form pin tampil, form manual HIDE
//      * Tap label Manual    -> form manual tampil, form pin HIDE
//      * Section terakhir DI-TAP DISIMPAN -> tahan force stop
//  - Form "Dari Pin": nama manual, Latitude & Longitude OTOMATIS
//    terisi dari pin tengah (read-only) -> Tombol Simpan
//  - Form "Manual": nama + latitude + longitude manual -> Tombol Simpan
//  - Tap ✏ pada item -> form "Edit Favorite" terisi dari item
//    (nama, latitude, longitude) -> Tombol Update
//  - Tap 🗑 pada item -> dialog konfirmasi (Hapus/Batal)
//  - Tap baris = fly ke lokasi
// =====================================================================

@Composable
private fun FavoriteDialog(
    prefs: SharedPreferences,
    initialTab: FavTab,
    grbFavs: List<FavItem>,
    gjkFavs: List<FavItem>,
    currentPin: LatLng,
    onDismiss: () -> Unit,
    onAdd: (FavTab, String, LatLng) -> Unit,
    onUpdate: (FavTab, Long, String, Double, Double) -> Unit,
    onDelete: (FavTab, Long) -> Unit,
    onFlyTo: (LatLng) -> Unit
) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(initialTab) }

    // Section (form) aktif — dibuka sesuai yang terakhir di-tap (persisten)
    var section by remember { mutableStateOf(FavStore.lastSection(prefs)) }

    // Form "Dari Pin" — lat/lng otomatis dari pin tengah (read-only)
    var pinName by remember { mutableStateOf("") }

    // Form "Manual"
    var manualName by remember { mutableStateOf("") }
    var manualLat by remember { mutableStateOf("") }
    var manualLng by remember { mutableStateOf("") }

    // Form "Edit Favorite"
    var editingId by remember { mutableStateOf<Long?>(null) }
    var editingName by remember { mutableStateOf("") }
    var editingLat by remember { mutableStateOf("") }
    var editingLng by remember { mutableStateOf("") }

    var deleteTarget by remember { mutableStateOf<FavItem?>(null) }

    val list = if (tab == FavTab.GRB) grbFavs else gjkFavs

    fun cancelEdit() {
        editingId = null
        editingName = ""
        editingLat = ""
        editingLng = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Favorite", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // ===== Tab GRB / GJK =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FavTabChip(
                        label = "GRB",
                        active = tab == FavTab.GRB,
                        activeColor = GRB_RED,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            tab = FavTab.GRB
                            cancelEdit()
                            FavStore.saveLastTab(prefs, FavTab.GRB)
                        }
                    )
                    FavTabChip(
                        label = "GJK",
                        active = tab == FavTab.GJK,
                        activeColor = GJK_BLUE,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            tab = FavTab.GJK
                            cancelEdit()
                            FavStore.saveLastTab(prefs, FavTab.GJK)
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (editingId != null) {
                    // ================= Edit Favorite =================
                    Text(
                        text = "Edit Favorite",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = editingName,
                        onValueChange = { editingName = it },
                        label = { Text("Nama Favorite") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = editingLat,
                        onValueChange = { editingLat = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = editingLng,
                        onValueChange = { editingLng = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            val lat = editingLat.trim().toDoubleOrNull()
                            val lng = editingLng.trim().toDoubleOrNull()
                            if (editingName.isBlank() || lat == null || lng == null ||
                                lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0
                            ) {
                                Toast.makeText(
                                    context,
                                    "Data tidak valid. Periksa nama & koordinat.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                onUpdate(tab, editingId!!, editingName, lat, lng)
                                cancelEdit()
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Update") }
                } else {
                    // ================= Accordion: Dari Pin / Manual =================

                    // --- Header "Dari Pin" (tap = buka form pin, tutup manual) ---
                    SectionHeader(
                        title = "Dari Pin",
                        active = section == "PIN",
                        onClick = {
                            section = "PIN"
                            FavStore.saveLastSection(prefs, "PIN")
                        }
                    )

                    if (section == "PIN") {
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = pinName,
                            onValueChange = { pinName = it },
                            label = { Text("Nama Favorite") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        // Latitude OTOMATIS dari pin tengah (read-only)
                        OutlinedTextField(
                            value = String.format(Locale.US, "%.6f", currentPin.latitude),
                            onValueChange = {},
                            label = { Text("Latitude") },
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        // Longitude OTOMATIS dari pin tengah (read-only)
                        OutlinedTextField(
                            value = String.format(Locale.US, "%.6f", currentPin.longitude),
                            onValueChange = {},
                            label = { Text("Longitude") },
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(
                            onClick = {
                                onAdd(tab, pinName, currentPin)
                                pinName = ""
                            },
                            enabled = pinName.isNotBlank(),
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("Simpan") }
                    }

                    Spacer(Modifier.height(4.dp))

                    // --- Header "Manual" (tap = buka form manual, tutup pin) ---
                    SectionHeader(
                        title = "Manual",
                        active = section == "MANUAL",
                        onClick = {
                            section = "MANUAL"
                            FavStore.saveLastSection(prefs, "MANUAL")
                        }
                    )

                    if (section == "MANUAL") {
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = manualName,
                            onValueChange = { manualName = it },
                            label = { Text("Nama Favorite") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = manualLat,
                            onValueChange = { manualLat = it },
                            label = { Text("Latitude") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = manualLng,
                            onValueChange = { manualLng = it },
                            label = { Text("Longitude") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(
                            onClick = {
                                val lat = manualLat.trim().toDoubleOrNull()
                                val lng = manualLng.trim().toDoubleOrNull()
                                if (manualName.isBlank() || lat == null || lng == null ||
                                    lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0
                                ) {
                                    Toast.makeText(
                                        context,
                                        "Data tidak valid. Periksa nama & koordinat.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    onAdd(tab, manualName, LatLng(lat, lng))
                                    manualName = ""
                                    manualLat = ""
                                    manualLng = ""
                                }
                            },
                            enabled = manualName.isNotBlank() &&
                                manualLat.isNotBlank() && manualLng.isNotBlank(),
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("Simpan") }
                    }
                }

                HorizontalDivider()

                Spacer(Modifier.height(8.dp))

                // ===== Daftar Favorite =====
                Text(
                    text = "Daftar Favorite (${list.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))

                if (list.isEmpty()) {
                    Text(
                        text = "Belum ada favorite di tab ini",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        list.forEach { item ->
                            FavRow(
                                item = item,
                                onEdit = {
                                    editingId = item.id
                                    editingName = item.name
                                    editingLat = String.format(Locale.US, "%.6f", item.lat)
                                    editingLng = String.format(Locale.US, "%.6f", item.lng)
                                },
                                onDelete = { deleteTarget = item },
                                onClick = { onFlyTo(LatLng(item.lat, item.lng)) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )

    // ===== Dialog konfirmasi hapus =====
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus favorite?") },
            text = { Text("\"${target.name}\" akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(tab, target.id)
                    deleteTarget = null
                }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Batal") }
            }
        )
    }
}

// Header section yang bisa di-tap (Dari Pin / Manual)
// Aktif = warna primary + tanda minus; non-aktif = redup + tanda plus
@Composable
private fun SectionHeader(
    title: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = if (active) "−" else "+",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FavTabChip(
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (active) activeColor else MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 1.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun FavRow(
    item: FavItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatLatLng(LatLng(item.lat, item.lng)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = "Edit ${item.name}",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = "Hapus ${item.name}",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
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

// =====================================================================
// Sistem izin BERURUTAN + DOUBLE CHECK:
//   1. LOKASI (foreground) -> 2. LOKASI "Selalu izinkan" ->
//   3. NOTIFIKASI (Android 13+) -> 4. BATERAI "Tanpa pembatasan"
// =====================================================================

private enum class PermissionStep { LOCATION, BACKGROUND, NOTIFICATION, BATTERY, DONE }

@Composable
private fun rememberAppPermissions(): Boolean {
    val context = LocalContext.current

    var locationGranted by remember {
        mutableStateOf(
            isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

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
                    step = PermissionStep.LOCATION
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
