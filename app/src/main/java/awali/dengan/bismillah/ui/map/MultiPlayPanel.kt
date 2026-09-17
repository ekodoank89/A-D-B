package awali.dengan.bismillah.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import awali.dengan.bismillah.R
import kotlin.math.roundToInt

// =====================================================================
// CONTOH: Panel 5 tombol PLAY/STOP (label 1..5) dalam 1 kontainer.
//   - Movable (drag) + lock/unlock (LockButton dari UtilityPanel.kt)
//   - Rotate vertikal <-> horizontal (tombol 🔄 di ujung panel)
//   - Setiap tombol punya warna aktif sendiri saat playing
// Orientation-aware: isi panel sama, wadah Column/Row yang berganti.
// =====================================================================

// Warna aktif per tombol 1..5
private val MULTI_COLORS = listOf(
    Color(0xFFE53935), // 1 merah
    Color(0xFF1E88E5), // 2 biru
    Color(0xFF2E7D32), // 3 hijau
    Color(0xFFF57C00), // 4 oranye
    Color(0xFF8E24AA)  // 5 ungu
)

@Composable
internal fun MultiPlayPanel(
    playing: List<Boolean>,          // 5 status play/stop (index 0..4)
    onToggle: (Int) -> Unit,         // tap tombol index i
    horizontal: Boolean,             // false = vertikal, true = horizontal
    onToggleOrientation: () -> Unit,
    locked: Boolean,
    onLockedChange: (Boolean) -> Unit,
    dragOffset: Offset,
    onDragOffsetChange: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDragOffset by rememberUpdatedState(dragOffset)
    val currentLocked by rememberUpdatedState(locked)

    Surface(
        modifier = modifier
            .offset { IntOffset(currentDragOffset.x.roundToInt(), currentDragOffset.y.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (!currentLocked) {
                        onDragOffsetChange(currentDragOffset + dragAmount)
                    }
                }
            },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(
            1.dp,
            if (locked) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        if (horizontal) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MultiPanelContent(
                    horizontal = true,
                    playing = playing,
                    onToggle = onToggle,
                    locked = locked,
                    onLockedChange = onLockedChange,
                    onToggleOrientation = onToggleOrientation
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MultiPanelContent(
                    horizontal = false,
                    playing = playing,
                    onToggle = onToggle,
                    locked = locked,
                    onLockedChange = onLockedChange,
                    onToggleOrientation = onToggleOrientation
                )
            }
        }
    }
}

// =====================================================================
// Isi panel — ditulis SEKALI, dipakai Column (vertikal) & Row (horizontal):
//   [▶1][1] sep [▶2][2] sep [▶3][3] sep [▶4][4] sep [▶5][5] sep [🔒] sep [🔄]
// =====================================================================

@Composable
private fun MultiPanelContent(
    horizontal: Boolean,
    playing: List<Boolean>,
    onToggle: (Int) -> Unit,
    locked: Boolean,
    onLockedChange: (Boolean) -> Unit,
    onToggleOrientation: () -> Unit
) {
    playing.forEachIndexed { index, isPlaying ->
        if (index > 0) {
            MultiGap(8, horizontal)
            MultiDivider(horizontal)
            MultiGap(8, horizontal)
        }
        MultiItem(
            index = index,
            playing = isPlaying,
            onClick = { onToggle(index) }
        )
    }

    MultiGap(8, horizontal)
    MultiDivider(horizontal)
    MultiGap(8, horizontal)

    // Lock/unlock movable
    LockButton(locked = locked, onToggle = { onLockedChange(!locked) })

    MultiGap(8, horizontal)
    MultiDivider(horizontal)
    MultiGap(8, horizontal)

    // Rotate vertikal <-> horizontal
    OrientationButton(onClick = onToggleOrientation)
}

// Satu item: tombol bulat play/stop + label angka di bawahnya
@Composable
private fun MultiItem(
    index: Int,
    playing: Boolean,
    onClick: () -> Unit
) {
    val accent = MULTI_COLORS[index]
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            shape = CircleShape,
            color = if (playing) accent else MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(
                        if (playing) R.drawable.ic_stop else R.drawable.ic_play
                    ),
                    contentDescription = if (playing) "Stop ${index + 1}" else "Play ${index + 1}",
                    tint = if (playing) Color.White
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (playing) accent else MaterialTheme.colorScheme.onSurface
        )
    }
}

// Separator orientation-aware:
//   vertikal   -> garis mendatar 46dp
//   horizontal -> garis tegak tinggi 46dp
@Composable
private fun MultiDivider(horizontal: Boolean) {
    if (horizontal) {
        VerticalDivider(
            modifier = Modifier.height(46.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    } else {
        HorizontalDivider(
            modifier = Modifier.width(46.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    }
}

// Spacer orientation-aware: height saat vertikal, width saat horizontal
@Composable
private fun MultiGap(dps: Int, horizontal: Boolean) {
    if (horizontal) {
        Spacer(Modifier.width(dps.dp))
    } else {
        Spacer(Modifier.height(dps.dp))
    }
}

// Tombol rotate orientasi
@Composable
private fun OrientationButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_swap),
            contentDescription = "Ubah orientasi panel",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}
