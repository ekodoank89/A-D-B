package awali.dengan.bismillah.ui.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import java.util.Locale
import kotlin.random.Random

// =====================================================================
// Konstanta, warna, style map, dan util format/jitter — dibagikan
// lintas file UI (internal = hanya terlihat di dalam modul app).
// =====================================================================

internal val DEFAULT_CENTER = LatLng(-6.2088, 106.8456) // Monas, Jakarta
internal const val DEFAULT_ZOOM = 17f
internal const val MAX_ZOOM = 21f // zoomTo() otomatis clamp ke max map
internal val PIN_SIZE = 40.dp

// ===== Warna =====
internal val PIN_GREEN = Color(0xFF2E7D32)  // pin tengah: HIJAU
internal val GRB_RED = Color(0xFFE53935)    // marker GRB: MERAH
internal val GJK_BLUE = Color(0xFF1E88E5)   // marker GJK: BIRU
internal val FAV_GOLD = Color(0xFFFFB300)   // aksen favorite: EMAS

// ===== Jitter: offset acak maksimal (derajat) ~= +-5 meter =====
private const val JITTER_MAX_DEG = 0.00005

// ===== Style gelap untuk Google Map =====
internal val DARK_MAP_STYLE = """
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
// Util format
// =====================================================================

internal fun formatLatLng(latLng: LatLng): String =
    String.format(Locale.US, "%.6f, %.6f", latLng.latitude, latLng.longitude)

internal fun formatCoord(latLng: LatLng?): String =
    latLng?.let { formatLatLng(it) } ?: "--.------, --.------"

// =====================================================================
// Jitter: offset acak kecil pada koordinat (lat & lng masing-masing
// digeser acak dalam rentang +-JITTER_MAX_DEG ~= +-5 meter)
// =====================================================================

internal fun applyJitter(coord: LatLng): LatLng = LatLng(
    coord.latitude + Random.nextDouble(-JITTER_MAX_DEG, JITTER_MAX_DEG),
    coord.longitude + Random.nextDouble(-JITTER_MAX_DEG, JITTER_MAX_DEG)
)
