package awali.dengan.bismillah.ui.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// =====================================================================
// Konstanta, warna, style map, tipe jitter, dan util format — dibagikan
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
// Tipe jitter — konfigurasi per tab (GRB/GJK)
// stepM: langkah per jendela (meter) | windowS: jendela/interval (detik)
// radiusM: radius maksimal dari titik awal (meter)
// =====================================================================

internal data class JitterConfig(
    val stepM: Float,
    val windowS: Int,
    val radiusM: Float
)

// Default sesuai permintaan
internal val JITTER_DEFAULT_GRB = JitterConfig(stepM = 2f, windowS = 8, radiusM = 3f)
internal val JITTER_DEFAULT_GJK = JitterConfig(stepM = 3f, windowS = 5, radiusM = 4f)

// Batas slider
internal const val JITTER_STEP_MIN = 0.5f
internal const val JITTER_STEP_MAX = 8f
internal const val JITTER_WINDOW_MIN = 1
internal const val JITTER_WINDOW_MAX = 15
internal const val JITTER_RADIUS_MIN = 0.5f
internal const val JITTER_RADIUS_MAX = 10f

// Util slider
internal fun stepToSlider(v: Float): Float = (v - JITTER_STEP_MIN) / (JITTER_STEP_MAX - JITTER_STEP_MIN)
internal fun sliderToStep(v: Float): Float =
    ((JITTER_STEP_MIN + v * (JITTER_STEP_MAX - JITTER_STEP_MIN)) * 2).roundToInt() / 2f

internal fun windowToSlider(v: Int): Float = (v - JITTER_WINDOW_MIN).toFloat() / (JITTER_WINDOW_MAX - JITTER_WINDOW_MIN)
internal fun sliderToWindow(v: Float): Int =
    (JITTER_WINDOW_MIN + v * (JITTER_WINDOW_MAX - JITTER_WINDOW_MIN)).roundToInt()

internal fun radiusToSlider(v: Float): Float =
    (v - JITTER_RADIUS_MIN) / (JITTER_RADIUS_MAX - JITTER_RADIUS_MIN)
internal fun sliderToRadius(v: Float): Float =
    ((JITTER_RADIUS_MIN + v * (JITTER_RADIUS_MAX - JITTER_RADIUS_MIN)) * 2).roundToInt() / 2f

// =====================================================================
// Jitter berkelanjutan: hitung posisi berikutnya.
//   - Arah acak; jarak = stepM (langkah per jendela)
//   - Jika hasil keluar radius radiusM dari pusat, arah dibalik
//     (memantul ke dalam) agar tetap dekat titik awal
// =====================================================================

internal fun nextJitterPosition(current: LatLng, center: LatLng, cfg: JitterConfig): LatLng {
    // Konversi meter ke derajat (aproksimasi di lintang saat ini)
    val metersPerDegLat = 111_320.0
    val metersPerDegLng = 111_320.0 * cos(Math.toRadians(center.latitude))

    val angle = Random.nextFloat() * (2.0 * Math.PI).toFloat()
    val dxM = sin(angle) * cfg.stepM
    val dyM = cos(angle) * cfg.stepM

    val nextLat = current.latitude + (dyM / metersPerDegLat)
    val nextLng = current.longitude + (dxM / metersPerDegLng)

    // Jarak dari pusat (meter)
    val dLatM = (nextLat - center.latitude) * metersPerDegLat
    val dLngM = (nextLng - center.longitude) * metersPerDegLng
    val distM = kotlin.math.sqrt(dLatM * dLatM + dLngM * dLngM)

    return if (distM <= cfg.radiusM) {
        LatLng(nextLat, nextLng)
    } else {
        // Pantul ke arah pusat: posisi berikutnya = bergerak sejauh stepM
        // dari posisi saat ini menuju pusat
        val toCenterLat = (center.latitude - current.latitude) * metersPerDegLat
        val toCenterLng = (center.longitude - current.longitude) * metersPerDegLng
        val toCenterDist = kotlin.math.sqrt(
            toCenterLat * toCenterLat + toCenterLng * toCenterLng
        ).coerceAtLeast(0.001)
        val unitLat = toCenterLat / toCenterDist
        val unitLng = toCenterLng / toCenterDist
        LatLng(
            current.latitude + (unitLat * cfg.stepM) / metersPerDegLat,
            current.longitude + (unitLng * cfg.stepM) / metersPerDegLng
        )
    }
}

// =====================================================================
// Util format
// =====================================================================

internal fun formatLatLng(latLng: LatLng): String =
    String.format(Locale.US, "%.6f, %.6f", latLng.latitude, latLng.longitude)

internal fun formatCoord(latLng: LatLng?): String =
    latLng?.let { formatLatLng(it) } ?: "--.------, --.------"
