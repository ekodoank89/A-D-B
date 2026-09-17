package awali.dengan.bismillah.ui.map

import android.content.SharedPreferences
import com.google.android.gms.maps.model.LatLng
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// =====================================================================
// Parameter jitter per tab (GRB/GJK) + persistensi + util konversi.
//   step     : langkah per jendela (meter), 0.5..8
//   interval : jendela (detik), 1..15
//   radius   : radius maksimal dari titik capture (meter), 0.5..10
// Default: GRB = 2m/8dtk/3m, GJK = 3m/5dtk/4m
// =====================================================================

internal data class JitterParams(
    val step: Float,
    val interval: Float,
    val radius: Float
)

internal val JITTER_DEFAULT_GRB = JitterParams(step = 2f, interval = 8f, radius = 3f)
internal val JITTER_DEFAULT_GJK = JitterParams(step = 3f, interval = 5f, radius = 4f)

internal object JitterStore {

    private const val KEY_PREFIX = "jitter_"

    private fun keyOf(tab: FavTab, field: String) =
        "${KEY_PREFIX}${if (tab == FavTab.GRB) "grb" else "gjk"}_$field"

    fun load(prefs: SharedPreferences, tab: FavTab): JitterParams {
        val def = if (tab == FavTab.GRB) JITTER_DEFAULT_GRB else JITTER_DEFAULT_GJK
        return JitterParams(
            step = prefs.getFloat(keyOf(tab, "step"), def.step),
            interval = prefs.getFloat(keyOf(tab, "interval"), def.interval),
            radius = prefs.getFloat(keyOf(tab, "radius"), def.radius)
        )
    }

    fun save(prefs: SharedPreferences, tab: FavTab, p: JitterParams) {
        prefs.edit()
            .putFloat(keyOf(tab, "step"), p.step)
            .putFloat(keyOf(tab, "interval"), p.interval)
            .putFloat(keyOf(tab, "radius"), p.radius)
            .apply()
    }

    fun setDefault(prefs: SharedPreferences, tab: FavTab) {
        save(prefs, tab, if (tab == FavTab.GRB) JITTER_DEFAULT_GRB else JITTER_DEFAULT_GJK)
    }

    // Flag aktif manual (tanpa play)
    fun isManual(prefs: SharedPreferences, tab: FavTab): Boolean =
        prefs.getBoolean(keyOf(tab, "manual"), false)

    fun setManual(prefs: SharedPreferences, tab: FavTab, on: Boolean) {
        prefs.edit().putBoolean(keyOf(tab, "manual"), on).apply()
    }
}

// =====================================================================
// Util konversi meter -> derajat & generator offset jitter
// =====================================================================

private const val METERS_PER_DEG_LAT = 111_320.0

private fun metersToLat(m: Double): Double = m / METERS_PER_DEG_LAT

private fun metersToLng(m: Double, atLat: Double): Double =
    m / (METERS_PER_DEG_LAT * cos(Math.toRadians(atLat)))

// Titik acak di dalam lingkaran radius (meter) dari center
internal fun randomInRadius(center: LatLng, radiusM: Float): LatLng {
    val r = radiusM * sqrt(Random.nextDouble()).toDouble()
    val theta = Random.nextDouble(0.0, 2.0 * PI)
    return LatLng(
        center.latitude + metersToLat(r * cos(theta)),
        center.longitude + metersToLng(r * sin(theta), center.latitude)
    )
}

// Gerak satu langkah (step meter, arah acak) dari current,
// di-clamp agar tidak melebihi radius dari titik pusat capture.
internal fun jitterStep(
    center: LatLng,
    current: LatLng,
    p: JitterParams
): LatLng {
    val theta = Random.nextDouble(0.0, 2.0 * PI)
    val dLat = metersToLat(p.step.toDouble() * cos(theta))
    val dLng = metersToLng(p.step.toDouble() * sin(theta), current.latitude)
    val newLat = current.latitude + dLat
    val newLng = current.longitude + dLng

    // Jarak baru dari pusat (aproksimasi planar dalam meter)
    val dLatM = (newLat - center.latitude) * METERS_PER_DEG_LAT
    val dLngM = (newLng - center.longitude) * METERS_PER_DEG_LAT *
        cos(Math.toRadians(center.latitude))
    val dist = sqrt(dLatM * dLatM + dLngM * dLngM)
    val maxR = p.radius.toDouble()
    if (dist <= maxR) return LatLng(newLat, newLng)

    // Clamp: proyeksikan ke lingkaran radius
    val scale = maxR / dist
    return LatLng(
        center.latitude + (newLat - center.latitude) * scale,
        center.longitude + (newLng - center.longitude) * scale
    )
}
