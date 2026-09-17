package awali.dengan.bismillah.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import awali.dengan.bismillah.R
import com.google.android.gms.maps.model.LatLng

// =====================================================================
// Panel chip koordinat — 5 baris:
//   PIN         : koordinat pin tengah layar (live) — tap = collapse
//   GRB         : koordinat PUSAT GRB (terkunci saat PLAY)  — tap = fly
//   Jitter GRB  : koordinat jitter GRB (pin kecil, bergerak) — tap = fly
//   GJK         : koordinat PUSAT GJK (terkunci saat PLAY)  — tap = fly
//   Jitter GJK  : koordinat jitter GJK (pin kecil, bergerak) — tap = fly
// Panel wrap-content (lebar = chip terlebar, IntrinsicSize.Max)
// =====================================================================

@Composable
internal fun CoordinatePanel(
    pinCoord: LatLng,
    grbCoord: LatLng?,          // pusat GRB (anchor)
    grbJitterCoord: LatLng?,    // posisi jitter GRB (bergerak)
    grbPlaying: Boolean,
    gjkCoord: LatLng?,          // pusat GJK (anchor)
    gjkJitterCoord: LatLng?,    // posisi jitter GJK (bergerak)
    gjkPlaying: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onGrbChipClick: () -> Unit,
    onGrbJitterChipClick: () -> Unit,
    onGjkChipClick: () -> Unit,
    onGjkJitterChipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (expanded) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 2.dp,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ===== Chip PIN — tap = collapse =====
                ChipRow(
                    leading = {
                        Icon(
                            painter = painterResource(R.drawable.ic_pin),
                            contentDescription = null,
                            tint = PIN_GREEN,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    label = "PIN",
                    text = formatLatLng(pinCoord),
                    onClick = { onExpandedChange(false) }
                )

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // ===== Chip GRB — pusat (terkunci saat PLAY) =====
                ChipRow(
                    leading = {
                        Icon(
                            painter = painterResource(R.drawable.ic_pin),
                            contentDescription = null,
                            tint = if (grbPlaying) GRB_RED else GRB_RED.copy(alpha = 0.4f),
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    label = "GRB",
                    text = formatCoord(grbCoord),
                    enabled = grbCoord != null,
                    onClick = onGrbChipClick
                )

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // ===== Chip Jitter GRB — posisi bergerak =====
                ChipRow(
                    leading = {
                        Icon(
                            painter = painterResource(R.drawable.ic_jitter),
                            contentDescription = null,
                            tint = if (grbPlaying) GRB_RED else GRB_RED.copy(alpha = 0.4f),
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    label = "Jitter GRB",
                    text = formatCoord(grbJitterCoord),
                    enabled = grbJitterCoord != null,
                    onClick = onGrbJitterChipClick
                )

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // ===== Chip GJK — pusat (terkunci saat PLAY) =====
                ChipRow(
                    leading = {
                        Icon(
                            painter = painterResource(R.drawable.ic_pin),
                            contentDescription = null,
                            tint = if (gjkPlaying) GJK_BLUE else GJK_BLUE.copy(alpha = 0.4f),
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    label = "GJK",
                    text = formatCoord(gjkCoord),
                    enabled = gjkCoord != null,
                    onClick = onGjkChipClick
                )

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // ===== Chip Jitter GJK — posisi bergerak =====
                ChipRow(
                    leading = {
                        Icon(
                            painter = painterResource(R.drawable.ic_jitter),
                            contentDescription = null,
                            tint = if (gjkPlaying) GJK_BLUE else GJK_BLUE.copy(alpha = 0.4f),
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    label = "Jitter GJK",
                    text = formatCoord(gjkJitterCoord),
                    enabled = gjkJitterCoord != null,
                    onClick = onGjkJitterChipClick
                )
            }
        }
    } else {
        Surface(
            modifier = modifier
                .clip(CircleShape)
                .clickable { onExpandedChange(true) },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 2.dp,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_eye),
                contentDescription = "Tampilkan chip koordinat",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(8.dp)
                    .size(20.dp)
            )
        }
    }
}

@Composable
private fun ChipRow(
    leading: @Composable () -> Unit,
    label: String,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
