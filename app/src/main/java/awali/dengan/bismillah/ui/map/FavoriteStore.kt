package awali.dengan.bismillah.ui.map

import android.content.SharedPreferences

// =====================================================================
// Model & penyimpanan Favorite — list TERPISAH per tab (GRB/GJK)
// Encoding: "id|nama|lat|lng", antar item dipisah ";"
// Juga menyimpan: tab terakhir + section (Dari Pin/Manual) terakhir.
// =====================================================================

internal data class FavItem(
    val id: Long,
    val name: String,
    val lat: Double,
    val lng: Double
)

internal enum class FavTab { GRB, GJK }

internal object FavStore {

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
