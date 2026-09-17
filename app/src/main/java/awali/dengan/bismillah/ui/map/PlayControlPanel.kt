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
// Panel tombol utama — favorite MEMBUKA DIALOG (bukan toggle marker).
// Urutan: [▶GRB] [GRB] [sep] [GJK] [▶GJK] [sep] [lock] [sep] [⭐] [sep] [Jitter]
// ⭐ = icon BINTANG EMAS; latar emas lembut jika ada favorite tersimpan.
// Movable (drag) + lock, posisi persisten.
// =====================================================================

@Composable
internal fun PlayControlPanel(
    grbPlaying: Boolean,
    gjkPlaying: Boolean,
    onGrbToggle: () -> Unit,
    onGjkToggle: () -> Unit,
    favActive: Boolean,
    onFavClick: () -> Unit,
    jitterEnabled: Boolean,
    onJitterToggle: () -> Unit,
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
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Tombol play/stop GRB
            PlayCircleButton(
                playing = grbPlaying,
                activeColor = GRB_RED,
                contentDesc = if (grbPlaying) "Stop GRB" else "Play GRB",
                onClick = onGrbToggle
            )

            Spacer(Modifier.height(5.dp))

            // 2. Label GRB
            PlayLabel(text = "GRB", playing = grbPlaying, activeColor = GRB_RED)

            Spacer(Modifier.height(8.dp))

            // 3. Separator
            PanelDivider()

            Spacer(Modifier.height(8.dp))

            // 4. Label GJK
            PlayLabel(text = "GJK", playing = gjkPlaying, activeColor = GJK_BLUE)

            Spacer(Modifier.height(5.dp))

            // 5. Tombol play/stop GJK
            PlayCircleButton(
                playing = gjkPlaying,
                activeColor = GJK_BLUE,
                contentDesc = if (gjkPlaying) "Stop GJK" else "Play GJK",
                onClick = onGjkToggle
            )

            Spacer(Modifier.height(8.dp))

            // 6. Separator
            PanelDivider()

            Spacer(Modifier.height(8.dp))

            // 7. Tombol lock/unlock movable
            LockButton(locked = locked, onToggle = { onLockedChange(!locked) })

            Spacer(Modifier.height(8.dp))

            // 8. Separator
            PanelDivider()

            Spacer(Modifier.height(8.dp))

            // 9. Tombol Favorite — icon BINTANG EMAS, buka dialog menu favorite
            UtilityButton(
                iconRes = R.drawable.ic_star,
                contentDesc = "Buka menu favorite",
                active = favActive,
                activeColor = FAV_GOLD.copy(alpha = 0.25f),
                iconTint = FAV_GOLD,
                size = 46.dp,
                iconSize = 22.dp,
                onClick = onFavClick
            )

            Spacer(Modifier.height(8.dp))

            // 10. Separator
            PanelDivider()

            Spacer(Modifier.height(8.dp))

            // 11. Tombol Jitter
            UtilityButton(
                iconRes = R.drawable.ic_jitter,
                contentDesc = if (jitterEnabled) "Jitter aktif" else "Jitter nonaktif",
                active = jitterEnabled,
                size = 46.dp,
                iconSize = 22.dp,
                onClick = onJitterToggle
            )
        }
    }
}

// =====================================================================
// Separator sempit — lebar seukuran tombol play (46dp)
// =====================================================================

@Composable
internal fun PanelDivider() {
    HorizontalDivider(
        modifier = Modifier.width(46.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}

@Composable
private fun PlayCircleButton(
    playing: Boolean,
    activeColor: Color,
    contentDesc: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (playing) activeColor else MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(
                    if (playing) R.drawable.ic_stop else R.drawable.ic_play
                ),
                contentDescription = contentDesc,
                tint = if (playing) Color.White
                else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun PlayLabel(
    text: String,
    playing: Boolean,
    activeColor: Color
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = if (playing) activeColor else MaterialTheme.colorScheme.onSurface
    )
}
