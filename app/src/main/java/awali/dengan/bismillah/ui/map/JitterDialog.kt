package awali.dengan.bismillah.ui.map

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// =====================================================================
// Dialog Jitter:
//  - 2 tab (GRB / GJK), parameter terpisah & persisten per tab
//  - Tombol Set Default (di bawah tab):
//      GRB = 2 m / 8 dtk / 3 m,  GJK = 3 m / 5 dtk / 4 m
//  - Slider (semua berkelipatan):
//      Langkah per jendela : 0.5 .. 8 m   (x 0.5)
//      Jendela (interval)  : 1 .. 15 dtk  (x 1)
//      Radius maksimal     : 0.5 .. 10 m  (x 0.5)
//  - Status efektif: otomatis via PLAY / manual / tidak aktif
//  - Tombol AKTIF MANUAL di bagian BAWAH dialog:
//      aktifkan jitter tanpa play (berlaku juga saat simpan
//      favorite "Dari Pin")
// =====================================================================

@Composable
internal fun JitterDialog(
    prefs: SharedPreferences,
    grbPlaying: Boolean,
    gjkPlaying: Boolean,
    grbParams: JitterParams,
    gjkParams: JitterParams,
    grbManual: Boolean,
    gjkManual: Boolean,
    onDismiss: () -> Unit,
    onParamsChange: (FavTab, JitterParams) -> Unit,
    onSetDefault: (FavTab) -> Unit,
    onManualChange: (FavTab, Boolean) -> Unit
) {
    var tab by remember { mutableStateOf(FavTab.GRB) }

    val params = if (tab == FavTab.GRB) grbParams else gjkParams
    val manual = if (tab == FavTab.GRB) grbManual else gjkManual
    val playing = if (tab == FavTab.GRB) grbPlaying else gjkPlaying
    val tabName = if (tab == FavTab.GRB) "GRB" else "GJK"
    val accent = if (tab == FavTab.GRB) GRB_RED else GJK_BLUE

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
                    FavTabChip(
                        label = "GRB",
                        active = tab == FavTab.GRB,
                        activeColor = GRB_RED,
                        modifier = Modifier.weight(1f),
                        onClick = { tab = FavTab.GRB }
                    )
                    FavTabChip(
                        label = "GJK",
                        active = tab == FavTab.GJK,
                        activeColor = GJK_BLUE,
                        modifier = Modifier.weight(1f),
                        onClick = { tab = FavTab.GJK }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ===== Tombol Set Default (di bawah tab) =====
                OutlinedButton(
                    onClick = { onSetDefault(tab) },
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Set Default") }

                Spacer(Modifier.height(4.dp))

                // ===== Slider: Langkah per jendela (0.5..8, kelipatan 0.5) =====
                Text(
                    "Langkah per jendela: ${formatMeter(params.step)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = params.step,
                    onValueChange = { raw ->
                        val snapped = (raw * 2).roundToInt() / 2f
                        onParamsChange(tab, params.copy(step = snapped))
                    },
                    valueRange = 0.5f..8f,
                    steps = 14
                )

                // ===== Slider: Jendela interval (1..15, kelipatan 1) =====
                Text(
                    "Jendela (interval): ${params.interval.toInt()} dtk",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = params.interval,
                    onValueChange = { raw ->
                        onParamsChange(tab, params.copy(interval = raw.roundToInt().toFloat()))
                    },
                    valueRange = 1f..15f,
                    steps = 13
                )

                // ===== Slider: Radius maksimal (0.5..10, kelipatan 0.5) =====
                Text(
                    "Radius maksimal: ${formatMeter(params.radius)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = params.radius,
                    onValueChange = { raw ->
                        val snapped = (raw * 2).roundToInt() / 2f
                        onParamsChange(tab, params.copy(radius = snapped))
                    },
                    valueRange = 0.5f..10f,
                    steps = 18
                )

                // ===== Status efektif =====
                val activeNow = playing || manual
                Text(
                    text = when {
                        playing -> "Jitter $tabName aktif (otomatis: PLAY)"
                        manual -> "Jitter $tabName aktif (manual)"
                        else -> "Jitter $tabName tidak aktif"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (activeNow) accent
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))

                // ===== Tombol AKTIF MANUAL (bagian bawah) =====
                Button(
                    onClick = { onManualChange(tab, !manual) },
                    enabled = !playing, // saat PLAY, jitter sudah aktif otomatis
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (manual) BTN_CANCEL_GRAY else BTN_SAVE_GREEN,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when {
                            playing -> "Aktif otomatis (PLAY)"
                            manual -> "Aktif Manual: NONAKTIFKAN"
                            else -> "Aktif Manual: AKTIFKAN"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
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

private fun formatMeter(m: Float): String =
    String.format(java.util.Locale.US, "%.1f m", m)
