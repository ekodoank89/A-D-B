package awali.dengan.bismillah.ui.map

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale

// =====================================================================
// Dialog Jitter:
//  - 2 tab (GRB/GJK), konfigurasi TERPISAH per tab
//  - Konfigurasi di-LIFT ke parent (MapScreen): slider mengubah state
//    parent via callback -> jitter loop langsung memakai nilai baru
//    (chip koordinat jitter ter-update sesuai interval terkini)
//  - Set Default: GRB = 2 m / 8 dtk / 3 m; GJK = 3 m / 5 dtk / 4 m
//  - Slider Langkah per jendela : 0,5 - 8 m   (kelipatan 0,5)
//  - Slider Jendela (interval)  : 1 - 15 dtk  (kelipatan 1)
//  - Slider Radius maksimal     : 0,5 - 10 m  (kelipatan 0,5)
//  - Jitter otomatis aktif saat tombol GRB/GJK PLAY (di MapScreen)
// =====================================================================

@Composable
internal fun JitterDialog(
    initialTab: FavTab,
    grbCfg: JitterConfig,
    gjkCfg: JitterConfig,
    onGrbCfgChange: (JitterConfig) -> Unit,
    onGjkCfgChange: (JitterConfig) -> Unit,
    onTabChange: (FavTab) -> Unit,
    onDismiss: () -> Unit
) {
    var tab by remember { mutableStateOf(initialTab) }

    val cfg = if (tab == FavTab.GRB) grbCfg else gjkCfg
    val accent = if (tab == FavTab.GRB) GRB_RED else GJK_BLUE
    val defaultCfg = if (tab == FavTab.GRB) JITTER_DEFAULT_GRB else JITTER_DEFAULT_GJK

    fun updateCfg(newCfg: JitterConfig) {
        if (tab == FavTab.GRB) {
            onGrbCfgChange(newCfg)
        } else {
            onGjkCfgChange(newCfg)
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
                        onTabChange(FavTab.GRB)
                    }
                    JitterTabChip(
                        label = "GJK",
                        active = tab == FavTab.GJK,
                        activeColor = GJK_BLUE,
                        modifier = Modifier.weight(1f)
                    ) {
                        tab = FavTab.GJK
                        onTabChange(FavTab.GJK)
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
                    text = String.format(Locale.US, "%.1f m", cfg.stepM),
                    style = MaterialTheme.typography.bodySmall,
                    color = accent
                )
                Slider(
                    value = stepToSlider(cfg.stepM),
                    onValueChange = { v -> updateCfg(cfg.copy(stepM = sliderToStep(v))) },
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
                    onValueChange = { v -> updateCfg(cfg.copy(windowS = sliderToWindow(v))) },
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
                    text = String.format(Locale.US, "%.1f m", cfg.radiusM),
                    style = MaterialTheme.typography.bodySmall,
                    color = accent
                )
                Slider(
                    value = radiusToSlider(cfg.radiusM),
                    onValueChange = { v -> updateCfg(cfg.copy(radiusM = sliderToRadius(v))) },
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
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth()
        )
    }
}
