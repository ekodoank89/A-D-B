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
private val FAV_GOLD = Color(0xFFFFB300)   // marker Favorite: EMAS

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
// Nama disanitasi (| dan ; diganti) agar tidak merusak encoding.
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

    // Ikon marker berbentuk pin (aman: fallback ke defaultMarker jika render gagal)
    val grbMarkerIcon = rememberPinMarkerIcon(GRB_RED, BitmapDescriptorFactory.HUE_RED)
    val gjkMarkerIcon = rememberPinMarkerIcon(GJK_BLUE, BitmapDescriptorFactory.HUE_BLUE)
    val favMarkerIcon = rememberPinMarkerIcon(FAV_GOLD, BitmapDescriptorFactory.HUE_YELLOW)

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

    // ===== Autofocus + KOMPAS =====
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

    // ===== Zoom IN: sekali tap langsung ke zoom MAKSIMAL =====
    fun zoomInMax() {
        scope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomTo(MAX_ZOOM)) }
    }

    // ===== Zoom OUT: mundur 2 level per tap =====
    fun zoomOut() {
        scope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomBy(-2f)) }
    }

    // ================================================================
    // CRUD Favorite — update state + simpan ke prefs (tahan force stop)
    // ================================================================

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

    fun renameFav(tab: FavTab, id: Long, newName: String) {
        fun patch(list: List<FavItem>) = list.map {
            if (it.id == id) it.copy(name = newName.trim()) else it
        }
        if (tab == FavTab.GRB) {
            grbFavs = patch(grbFavs)
            FavStore.saveList(prefs, FavTab.GRB, grbFavs)
        } else {
            gjkFavs = patch(gjkFavs)
            FavStore.saveList(prefs, FavTab.GJK, gjkFavs)
        }
    }

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

            // ===== Google Map + marker GRB/GJK + marker semua favorite (emas) =====
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
                        state = rememberMarkerState(key = "gjk_$coord", position = coord),
                        title = "GJK",
                        icon = gjkMarkerIcon,
                        anchor = Offset(0.5f, 1.0f)
                    )
                }
                // Marker emas untuk setiap favorite (kedua tab)
                (grbFavs + gjkFavs).forEach { fav ->
                    val pos = LatLng(fav.lat, fav.lng)
                    Marker(
                        state = rememberMarkerState(key = "fav_${fav.id}_$pos", position = pos),
                        title = fav.name,
                        icon = favMarkerIcon,
                        anchor = Offset(0.5f, 1.0f)
                    )
                }
            }

            // ===== Pin HIJAU tetap di tengah layar =====
            CenterPin(modifier = Modifier.align(Alignment.Center))

            // ===== Panel chip koordinat: PIN + GRB + GJK =====
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

            // ===== Panel utilitas icon-only =====
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
                    onRename = { tab, id, name -> renameFav(tab, id, name) },
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
// Panel tombol — favorite kini MEMBUKA DIALOG (bukan toggle marker).
// Urutan: [▶GRB] [GRB] [sep] [GJK] [▶GJK] [sep] [lock] [sep] [⭐] [sep] [Jitter]
// ⭐ menyala emas jika ada favorite di salah satu tab.
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

            // 9. Tombol Favorite — buka dialog menu favorite
            UtilityButton(
                iconRes = R.drawable.ic_star,
                contentDesc = "Buka menu favorite",
                active = favActive,
                activeColor = FAV_GOLD,
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

// =====================================================================
// Dialog Favorite:
//  - 2 tab (GRB/GJK), tab terakhir di-tap DISIMPAN -> dibuka lagi nanti
//  - Simpan dari Pin: nama saja, koordinat = pin tengah saat ini
//  - Input Manual: nama + koordinat (lat, lng)
//  - Daftar favorite: tap baris = fly ke lokasi, edit = rename inline,
//    hapus = dialog konfirmasi (Hapus/Batal)
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
    onRename: (FavTab, Long, String) -> Unit,
    onDelete: (FavTab, Long) -> Unit,
    onFlyTo: (LatLng) -> Unit
) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(initialTab) }
    var pinName by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var manualCoord by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var editingName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<FavItem?>(null) }

    val list = if (tab == FavTab.GRB) grbFavs else gjkFavs

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
                            FavStore.saveLastTab(prefs, FavTab.GJK)
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ===== Simpan dari Pin =====
                Text(
                    text = "Simpan dari Pin (posisi tengah sekarang)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatLatLng(currentPin),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = pinName,
                    onValueChange = { pinName = it },
                    label = { Text("Nama favorite") },
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
                ) { Text("Simpan dari Pin") }

                HorizontalDivider()

                Spacer(Modifier.height(8.dp))

                // ===== Input Manual =====
                Text(
                    text = "Input Manual",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = manualName,
                    onValueChange = { manualName = it },
                    label = { Text("Nama") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = manualCoord,
                    onValueChange = { manualCoord = it },
                    label = { Text("Koordinat (lat, lng)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = {
                        val parts = manualCoord.split(",")
                        val lat = parts.getOrNull(0)?.trim()?.toDoubleOrNull()
                        val lng = parts.getOrNull(1)?.trim()?.toDoubleOrNull()
                        if (lat == null || lng == null ||
                            lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0
                        ) {
                            Toast.makeText(
                                context,
                                "Koordinat tidak valid. Format: lat, lng",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            onAdd(tab, manualName, LatLng(lat, lng))
                            manualName = ""
                            manualCoord = ""
                        }
                    },
                    enabled = manualName.isNotBlank() && manualCoord.isNotBlank(),
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Tambah Manual") }

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
                                editing = editingId == item.id,
                                editName = editingName,
                                onEditNameChange = { editingName = it },
                                onStartEdit = {
                                    editingId = item.id
                                    editingName = item.name
                                },
                                onSaveEdit = {
                                    if (editingName.isNotBlank()) {
                                        onRename(tab, item.id, editingName)
                                    }
                                    editingId = null
                                },
                                onCancelEdit = { editingId = null },
                                onRequestDelete = { deleteTarget = item },
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
    editing: Boolean,
    editName: String,
    onEditNameChange: (String) -> Unit,
    onStartEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onRequestDelete: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (editing) {
            OutlinedTextField(
                value = editName,
                onValueChange = onEditNameChange,
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onSaveEdit, enabled = editName.isNotBlank()) {
                Text("Simpan")
            }
            TextButton(onClick = onCancelEdit) { Text("Batal") }
        } else {
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
            IconButton(onClick = onStartEdit) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = "Edit ${item.name}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onRequestDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = "Hapus ${item.name}",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// =====================================================================
// Panel tombol — favorite kini MEMBUKA DIALOG (bukan toggle marker).
// Urutan: [▶GRB] [GRB] [sep] [GJK] [▶GJK] [sep] [lock] [sep] [⭐] [sep] [Jitter]
// ⭐ menyala emas jika ada favorite di salah satu tab.
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

            // 9. Tombol Favorite — buka dialog menu favorite
            UtilityButton(
                iconRes = R.drawable.ic_star,
                contentDesc = "Buka menu favorite",
                active = favActive,
                activeColor = FAV_GOLD,
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

// =====================================================================
// Dialog Favorite:
//  - 2 tab (GRB/GJK), tab terakhir di-tap DISIMPAN -> dibuka lagi nanti
//  - Simpan dari Pin: nama saja, koordinat = pin tengah saat ini
//  - Input Manual: nama + koordinat (lat, lng)
//  - Daftar favorite: tap baris = fly ke lokasi, edit = rename inline,
//    hapus = dialog konfirmasi (Hapus/Batal)
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
    onRename: (FavTab, Long, String) -> Unit,
    onDelete: (FavTab, Long) -> Unit,
    onFlyTo: (LatLng) -> Unit
) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(initialTab) }
    var pinName by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var manualCoord by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var editingName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<FavItem?>(null) }

    val list = if (tab == FavTab.GRB) grbFavs else gjkFavs

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
                            FavStore.saveLastTab(prefs, FavTab.GJK)
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ===== Simpan dari Pin =====
                Text(
                    text = "Simpan dari Pin (posisi tengah sekarang)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatLatLng(currentPin),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = pinName,
                    onValueChange = { pinName = it },
                    label = { Text("Nama favorite") },
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
                ) { Text("Simpan dari Pin") }

                HorizontalDivider()

                Spacer(Modifier.height(8.dp))

                // ===== Input Manual =====
                Text(
                    text = "Input Manual",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = manualName,
                    onValueChange = { manualName = it },
                    label = { Text("Nama") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = manualCoord,
                    onValueChange = { manualCoord = it },
                    label = { Text("Koordinat (lat, lng)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = {
                        val parts = manualCoord.split(",")
                        val lat = parts.getOrNull(0)?.trim()?.toDoubleOrNull()
                        val lng = parts.getOrNull(1)?.trim()?.toDoubleOrNull()
                        if (lat == null || lng == null ||
                            lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0
                        ) {
                            Toast.makeText(
                                context,
                                "Koordinat tidak valid. Format: lat, lng",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            onAdd(tab, manualName, LatLng(lat, lng))
                            manualName = ""
                            manualCoord = ""
                        }
                    },
                    enabled = manualName.isNotBlank() && manualCoord.isNotBlank(),
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Tambah Manual") }

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
                                editing = editingId == item.id,
                                editName = editingName,
                                onEditNameChange = { editingName = it },
                                onStartEdit = {
                                    editingId = item.id
                                    editingName = item.name
                                },
                                onSaveEdit = {
                                    if (editingName.isNotBlank()) {
                                        onRename(tab, item.id, editingName)
                                    }
                                    editingId = null
                                },
                                onCancelEdit = { editingId = null },
                                onRequestDelete = { deleteTarget = item },
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
    editing: Boolean,
    editName: String,
    onEditNameChange: (String) -> Unit,
    onStartEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onRequestDelete: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (editing) {
            OutlinedTextField(
                value = editName,
                onValueChange = onEditNameChange,
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onSaveEdit, enabled = editName.isNotBlank()) {
                Text("Simpan")
            }
            TextButton(onClick = onCancelEdit) { Text("Batal") }
        } else {
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
            IconButton(onClick = onStartEdit) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = "Edit ${item.name}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onRequestDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = "Hapus ${item.name}",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
