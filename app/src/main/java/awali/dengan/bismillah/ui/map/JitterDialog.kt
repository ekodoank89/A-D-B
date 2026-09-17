package awali.dengan.bismillah.ui.map

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// =====================================================================
// Dialog Jitter:
//  - 2 tab (GRB/GJK), konfigurasi TERPISAH per tab (persisten)
//  - Set Default: GRB = 2 m / 8 dtk / 3 m; GJK = 3 m / 5 dtk / 4 m
//  - Slider Langkah per jendela : 0,5 - 8 m   (kelipatan 0,5)
//  - Slider Jendela (interval)  : 1 - 15 dtk  (kelipatan 1)
//  - Slider Radius maksimal     : 0,5 - 10 m  (kelipatan 0,5)
//  - Jitter otomatis aktif saat tombol GRB/GJK PLAY (di MapScreen)
// =====================================================================

private const val KEY_JITTER_GRB = "jitter_cfg_grb"
private const val KEY_JITTER_GJK = "jitter_cfg_gjk"
private const val KEY_JITTER_LAST_TAB = "jitter_last_tab"

// Simpan config sebagai "step|window|radius"
internal fun loadJitterConfig(prefs: SharedPreferences, tab: FavTab): JitterConfig {
    val key = if (tab == FavTab.GRB) KEY_JITTER_GRB else KEY_JITTER_GJK
    val raw = prefs.getString(key, null) ?: return if (tab == FavTab.GRB) {
        JITTER_DEFAULT_GRB
    } else {
        JITTER_DEFAULT_GJK
    }
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
    val key = if (tab == FavTab.GRB) KEY_JITTER_GRB else KEY_JITTER_GJK
    prefs.edit().putString(key, "${cfg.stepM}|${cfg.windowS}|${cfg.radiusM}").apply()
}

internal fun loadJitterLastTab(prefs: SharedPreferences): FavTab =
    if (prefs.getString(KEY_JITTER_LAST_TAB, "GRB") == "GJK") FavTab.GJK else FavTab.GRB

internal fun saveJitterLastTab(prefs: SharedPreferences, tab: FavTab) {
    prefs.edit().putString(KEY_JITTER_LAST_TAB, if (tab == FavTab.GJK) "GJK" else "GRB").apply()
}

@Composable
internal fun JitterDialog(
    prefs: SharedPreferences,
    initialTab: FavTab,
    onDismiss: () -> Unit
) {
    var tab by remember { mutableStateOf(initialTab) }
    var grbCfg by remember { mutableStateOf(loadJitterConfig(prefs, FavTab.GRB)) }
    var gjkCfg by remember { mutableStateOf(loadJitterConfig(prefs, FavTab.GJK)) }

    val cfg = if (tab == FavTab.GRB) grbCfg else gjkCfg
    val accent = if (tab == FavTab.GRB) GRB_RED else GJK_BLUE
    val defaultCfg = if (tab == FavTab.GRB) JITTER_DEFAULT_GRB else JITTER_DEFAULT_GJK

    fun updateCfg(newCfg: JitterConfig) {
        if (tab == FavTab.GRB) {
            grbCfg = newCfg
            saveJitterConfig(prefs, FavTab.GRB, newCfg)
        } else {
            gjkCfg = newCfg
            saveJitterConfig(prefs, FavTab.GJK, newCfg)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Jitter", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // ===== Tab GRB / GJK =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    JitterTabChip(
                        label = "GRB",
                        active = tab == FavTab.GRB,
                        activeColor = GRB_RED,
                        modifier = Modifier.weight(1f)
                    ) {
                        tab = FavTab.GRB
                        saveJitterLastTab(prefs, FavTab.GRB)
                    }
                    JitterTabChip(
                        label = "GJK",
                        active = tab == FavTab.GJK,
                        activeColor = GJK_BLUE,
                        modifier = Modifier.weight(1f)
                    ) {
                        tab = FavTab.GJK
                        saveJitterLastTab(prefs, FavTab.GJK)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ===== Set Default =====
                Button(
                    onClick = { updateCfg(defaultCfg) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) { Text("Set Default") }

                Spacer(Modifier.height(12.dp))

                // ===== Slider: Langkah per jendela (0,5 - 8 m, kelipatan 0,5) =====
                Text(
                    text = "Langkah per jendela",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(java.util.Locale.US, "%.1f m", cfg.stepM),
                    style = MaterialTheme.typography.bodySmall,
                    color = accent
                )
                Slider(
                    value = stepToSlider(cfg.stepM),
                    onValueChange = { v ->
                        updateCfg(cfg.copy(stepM = sliderToStep(v)))
                    },
                    valueRange = 0f..1f
                )

                Spacer(Modifier.height(4.dp))

                // ===== Slider: Jendela (interval) (1 - 15 dtk, kelipatan 1) =====
                Text(
                    text = "Jendela (interval)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${cfg.windowS} dtk",
                    style = MaterialTheme.typography.bodySmall,
                    color = accent
                )
                Slider(
                    value = windowToSlider(cfg.windowS),
                    onValueChange = { v ->
                        updateCfg(cfg.copy(windowS = sliderToWindow(v)))
                    },
                    valueRange = 0f..1f
                )

                Spacer(Modifier.height(4.dp))

                // ===== Slider: Radius maksimal (0,5 - 10 m, kelipatan 0,5) =====
                Text(
                    text = "Radius maksimal",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(java.util.Locale.US, "%.1f m", cfg.radiusM),
                    style = MaterialTheme.typography.bodySmall,
                    color = accent
                )
                Slider(
                    value = radiusToSlider(cfg.radiusM),
                    onValueChange = { v ->
                        updateCfg(cfg.copy(radiusM = sliderToRadius(v)))
                    },
                    valueRange = 0f..1f
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Tutup",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun JitterTabChip(
    label: String,
    active: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
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
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
