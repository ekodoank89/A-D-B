package awali.dengan.bismillah.ui.map

import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import awali.dengan.bismillah.R
import com.google.android.gms.maps.CameraUpdateFactory
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

// =====================================================================
// Orchestrator: memegang state persisten + wiring antar panel.
// Implementasi UI tiap panel ada di file terpisah:
//   MapConstants.kt, MapPrefs.kt, FavoriteStore.kt, MapPermissions.kt,
//   MarkerIcons.kt, CoordinatePanel.kt, PlayControlPanel.kt,
//   UtilityPanel.kt, FavoriteDialog.kt, MultiPlayPanel.kt
// Semua panel moveable menerima screenSize untuk clamp posisi drag
// agar tidak pernah keluar dari tampilan layar.
// Panel utama & utilitas juga bisa di-rotate (vertikal <-> horizontal),
// state orientasinya persisten. Rotate nonaktif saat panel terkunci.
// =====================================================================

// Key persistensi posisi kamera
private const val KEY_CAM_LAT = "cam_lat"
private const val KEY_CAM_LNG = "cam_lng"
private const val KEY_CAM_ZOOM = "cam_zoom"

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

    // ===== Orientasi panel (rotate) — PERSISTEN =====
    var playHorizontal by rememberPersistentBoolean("play_horizontal", false)
    var utilHorizontal by rememberPersistentBoolean("util_horizontal", false)

    // ===== First launch: pin otomatis ke titik biru (sekali saja) =====
    var firstLaunchDone by rememberPersistentBoolean("first_launch_done", false)

    // ===== Favorite =====
    var showFavDialog by remember { mutableStateOf(false) }
    var grbFavs by remember { mutableStateOf(FavStore.loadList(prefs, FavTab.GRB)) }
    var gjkFavs by remember { mutableStateOf(FavStore.loadList(prefs, FavTab.GJK)) }

    // ===== Contoh: panel 4 tombol play/stop — PERSISTEN =====
    var multiPlaying by remember {
        // Parsing aman: maksimal 4 nilai + pad false jika kurang
        // -> selalu List<Boolean> ukuran 4 (data lama tidak bikin crash)
        val parsed = (prefs.getString("multi_playing", "0,0,0,0") ?: "0,0,0,0")
            .split(",").take(4).map { it == "1" }
        mutableStateOf(List(4) { i -> parsed.getOrNull(i) ?: false })
    }
    var multiHorizontal by rememberPersistentBoolean("multi_horizontal", false)
    var multiLocked by rememberPersistentBoolean("multi_locked", false)
    var multiDrag by rememberPersistentOffset("multi_drag", Offset.Zero)

    // Simpan status play panel contoh tiap berubah (tahan force stop)
    SideEffect {
        prefs.edit()
            .putString("multi_playing", multiPlaying.joinToString(",") { if (it) "1" else "0" })
            .apply()
    }

    // ===== Ukuran layar (px) — dipakai panel moveable untuk clamp =====
    var screenSize by remember { mutableStateOf(IntSize.Zero) }

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
        // Ukuran layar diukur dari Box root — dipakai semua panel moveable
        Box(
            modifier = modifier
                .fillMaxSize()
                .onSizeChanged { screenSize = it }
        ) {

            // ===== Google Map full width + marker GRB/GJK =====
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

            // ===== Panel tombol utama (rotate + favorite membuka dialog) =====
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
                horizontal = playHorizontal,
                onToggleOrientation = { playHorizontal = !playHorizontal },
                locked = playLocked,
                onLockedChange = { playLocked = it },
                dragOffset = playDragOffset,
                onDragOffsetChange = { playDragOffset = it },
                screenSize = screenSize,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            )

            // ===== Panel utilitas icon-only (rotate + moveable + lock) =====
            UtilityPanel(
                darkMode = darkMode,
                onAutoFocus = { autoFocus() },
                onToggleDark = { darkMode = !darkMode },
                onZoomIn = { zoomInMax() },
                onZoomOut = { zoomOut() },
                horizontal = utilHorizontal,
                onToggleOrientation = { utilHorizontal = !utilHorizontal },
                locked = utilLocked,
                onLockedChange = { utilLocked = it },
                dragOffset = utilDragOffset,
                onDragOffsetChange = { utilDragOffset = it },
                screenSize = screenSize,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 16.dp)
            )

            // ===== Contoh: panel 4 tombol play/stop (rotate + moveable + lock) =====
            MultiPlayPanel(
                playing = multiPlaying,
                onToggle = { i ->
                    multiPlaying = multiPlaying.mapIndexed { idx, v -> if (idx == i) !v else v }
                },
                horizontal = multiHorizontal,
                onToggleOrientation = { multiHorizontal = !multiHorizontal },
                locked = multiLocked,
                onLockedChange = { multiLocked = it },
                dragOffset = multiDrag,
                onDragOffsetChange = { multiDrag = it },
                screenSize = screenSize,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, bottom = 16.dp)
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
