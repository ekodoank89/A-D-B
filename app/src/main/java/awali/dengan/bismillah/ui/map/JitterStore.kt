package awali.dengan.bismillah.ui.map

import android.content.SharedPreferences
import com.google.android.gms.maps.model.LatLng
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// =====================================================================
// Sumber kebenaran jitter: tipe config, default, batas slider,
// konverter slider, persistensi per tab (GRB/GJK), dan kalkulasi
// posisi berikutnya (jitter berkelanjutan).
// =====================================================================

// Tipe config jitter:
//   stepM   = langkah per jendela (meter)
//   windowS = jendela / interval (detik)
//   radiusM = radius maksimal dari titik awal (meter)
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

// ===== Konverter nilai <-> posisi slider (0..1) =====

internal fun stepToSlider(v: Float): Float =
    (v - JITTER_STEP_MIN) / (JITTER_STEP_MAX - JITTER_STEP_MIN)

internal fun sliderToStep(v: Float): Float =
    ((JITTER_STEP_MIN + v * (JITTER_STEP_MAX - JITTER_STEP_MIN)) * 2).roundToInt() / 2f

internal fun windowToSlider(v: Int): Float =
    (v - JITTER_WINDOW_MIN).toFloat() / (JITTER_WINDOW_MAX - JITTER_WINDOW_MIN)

internal fun sliderToWindow(v: Float): Int =
    (JITTER_WINDOW_MIN + v * (JITTER_WINDOW_MAX - JITTER_WINDOW_MIN)).roundToInt()

internal fun radiusToSlider(v: Float): Float =
    (v - JITTER_RADIUS_MIN) / (JITTER_RADIUS_MAX - JITTER_RADIUS_MIN)

internal fun sliderToRadius(v: Float): Float =
    ((JITTER_RADIUS_MIN + v * (JITTER_RADIUS_MAX - JITTER_RADIUS_MIN)) * 2).roundToInt() / 2f

// ===== Persistensi per tab =====

private const val KEY_JITTER_GRB = "jitter_cfg_grb"
private const val KEY_JITTER_GJK = "jitter_cfg_gjk"
private const val KEY_JITTER_LAST_TAB = "jitter_last_tab"

private fun keyOf(tab: FavTab) = if (tab == FavTab.GRB) KEY_JITTER_GRB else KEY_JITTER_GJK

// Simpan config sebagai "step|window|radius"
internal fun loadJitterConfig(prefs: SharedPreferences, tab: FavTab): JitterConfig {
    val raw = prefs.getString(keyOf(tab), null)
        ?: return if (tab == FavTab.GRB) JITTER_DEFAULT_GRB else JITTER_DEFAULT_GJK
    return runCatching {
        val p = raw.split("|")
        JitterConfig(
            stepM = p[0].toFloatOrNull() ?: 2f,
            windowS = p[1].toIntOrNull() ?: 8,
            radiusM = p[2].toFloatOrNull() ?: 3f
        )
    }.getOrDefault(if (tab == FavTab.GRB) JITTER_DEFAULT_GRB else JITTER_DEFAULT_GJK)
}

internal fun saveJitterConfig(prefs: SharedPreferences, tab: FavTab, cfg: JitterConfig) {
    prefs.edit()
        .putString(keyOf(tab), "${cfg.stepM}|${cfg.windowS}|${cfg.radiusM}")
        .apply()
}

internal fun loadJitterLastTab(prefs: SharedPreferences): FavTab =
    if (prefs.getString(KEY_JITTER_LAST_TAB, "GRB") == "GJK") FavTab.GJK else FavTab.GRB

internal fun saveJitterLastTab(prefs: SharedPreferences, tab: FavTab) {
    prefs.edit()
        .putString(KEY_JITTER_LAST_TAB, if (tab == FavTab.GJK) "GJK" else "GRB")
        .apply()
}

// =====================================================================
// Jitter berkelanjutan: hitung posisi berikutnya.
//   - Arah acak; jarak = stepM (langkah per jendela)
//   - center = TITIK AWAL (anchor) saat PLAY — radius diukur dari sini
//   - Jika hasil keluar radius, arah dibalik (memantul ke dalam)
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

    // Jarak kandidat dari titik awal (meter)
    val dLatM = (nextLat - center.latitude) * metersPerDegLat
    val dLngM = (nextLng - center.longitude) * metersPerDegLng
    val distM = sqrt(dLatM * dLatM + dLngM * dLngM)

    return if (distM <= cfg.radiusM) {
        LatLng(nextLat, nextLng)
    } else {
        // Pantul ke arah pusat: bergerak sejauh stepM dari posisi saat ini
        val toCenterLat = (center.latitude - current.latitude) * metersPerDegLat
        val toCenterLng = (center.longitude - current.longitude) * metersPerDegLng
        val toCenterDist = sqrt(
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
