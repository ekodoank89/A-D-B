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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import awali.dengan.bismillah.R
import kotlin.math.roundToInt

// =====================================================================
// Panel tombol utama — favorite & jitter MEMBUKA DIALOG.
//   [▶GRB][GRB] sep [GJK][▶GJK] sep [kolom 🔒+🔄] sep [⭐] sep [🎲]
//   - Rotate vertikal <-> horizontal (tombol 🔄)
//       * Saat panel TERKUNCI (🔒): rotate NONAKTIF (redup, tidak bisa di-tap)
//       * Saat panel UNLOCK (🔓): rotate AKTIF kembali
//   - 🎲 menyala saat jitter aktif di salah satu tab (PLAY otomatis
//     atau manual)
//   - Movable (drag bebas) + lock — clamp saat jari dilepas & saat
//     ukuran layar/panel berubah (bukan saat drag)
// =====================================================================

// Delta koreksi agar panel (di posisi panelPos) masuk ke dalam layar.
private fun clampCorrection(
    panelPos: Offset,
    panelSize: IntSize,
    screenSize: IntSize,
    marginPx: Float
): Offset {
    if (screenSize.width <= 0 || screenSize.height <= 0 ||
        panelSize.width <= 0 || panelSize.height <= 0
    ) return Offset.Zero
    val minX = marginPx
    val maxX = (screenSize.width - panelSize.width - marginPx).toFloat()
    val minY = marginPx
    val maxY = (screenSize.height - panelSize.height - marginPx).toFloat()
    val targetX = panelPos.x.coerceIn(minX, maxX)
    val targetY = panelPos.y.coerceIn(minY, maxY)
    return Offset(targetX - panelPos.x, targetY - panelPos.y)
}

@Composable
internal fun PlayControlPanel(
    grbPlaying: Boolean,
    gjkPlaying: Boolean,
    onGrbToggle: () -> Unit,
    onGjkToggle: () -> Unit,
    favActive: Boolean,
    onFavClick: () -> Unit,
    jitterActive: Boolean,
    onJitterClick: () -> Unit,
    horizontal: Boolean,             // false = vertikal, true = horizontal
    onToggleOrientation: () -> Unit,
    locked: Boolean,
    onLockedChange: (Boolean) -> Unit,
    dragOffset: Offset,
    onDragOffsetChange: (Offset) -> Unit,
    screenSize: IntSize,
    modifier: Modifier = Modifier
) {
    val currentDragOffset by rememberUpdatedState(dragOffset)
    val currentLocked by rememberUpdatedState(locked)

    var panelSize by remember { mutableStateOf(IntSize.Zero) }
    var panelPos by remember { mutableStateOf(Offset.Zero) }

    val marginPx = with(LocalDensity.current) { 16.dp.toPx() }

    // Koreksi posisi SEKALI saat ukuran layar/panel berubah (start & rotate).
    LaunchedEffect(screenSize, panelSize) {
        if (screenSize.width == 0 || panelSize.width == 0) return@LaunchedEffect
        val delta = clampCorrection(panelPos, panelSize, screenSize, marginPx)
        if (delta != Offset.Zero) {
            onDragOffsetChange(currentDragOffset + delta)
        }
    }

    Surface(
        modifier = modifier
            .offset { IntOffset(currentDragOffset.x.roundToInt(), currentDragOffset.y.roundToInt()) }
            .onGloballyPositioned { panelPos = it.positionInWindow() }
            .onSizeChanged { panelSize = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (!currentLocked) {
                            val delta = clampCorrection(
                                panelPos, panelSize, screenSize, marginPx
                            )
                            if (delta != Offset.Zero) {
                                onDragOffsetChange(currentDragOffset + delta)
                            }
                        }
                    },
                    onDragCancel = {
                        if (!currentLocked) {
                            val delta = clampCorrection(
                                panelPos, panelSize, screenSize, marginPx
                            )
                            if (delta != Offset.Zero) {
                                onDragOffsetChange(currentDragOffset + delta)
                            }
                        }
                    }
                ) { change, dragAmount ->
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
                PlayPanelItems(
                    horizontal = true,
                    grbPlaying = grbPlaying,
                    gjkPlaying = gjkPlaying,
                    onGrbToggle = onGrbToggle,
                    onGjkToggle = onGjkToggle,
                    favActive = favActive,
                    onFavClick = onFavClick,
                    jitterActive = jitterActive,
                    onJitterClick = onJitterClick,
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
                PlayPanelItems(
                    horizontal = false,
                    grbPlaying = grbPlaying,
                    gjkPlaying = gjkPlaying,
                    onGrbToggle = onGrbToggle,
                    onGjkToggle = onGjkToggle,
                    favActive = favActive,
                    onFavClick = onFavClick,
                    jitterActive = jitterActive,
                    onJitterClick = onJitterClick,
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
//   [▶GRB][GRB] sep [GJK][▶GJK] sep [kolom 🔒+🔄] sep [⭐] sep [🎲]
// =====================================================================

@Composable
private fun PlayPanelItems(
    horizontal: Boolean,
    grbPlaying: Boolean,
    gjkPlaying: Boolean,
    onGrbToggle: () -> Unit,
    onGjkToggle: () -> Unit,
    favActive: Boolean,
    onFavClick: () -> Unit,
    jitterActive: Boolean,
    onJitterClick: () -> Unit,
    locked: Boolean,
    onLockedChange: (Boolean) -> Unit,
    onToggleOrientation: () -> Unit
) {
    // 1. GRB (tombol lalu label)
    PlayLabeledItem(
        horizontal = horizontal,
        labelFirst = false,
        button = {
            PlayCircleButton(
                playing = grbPlaying,
                activeColor = GRB_RED,
                contentDesc = if (grbPlaying) "Stop GRB" else "Play GRB",
                onClick = onGrbToggle
            )
        },
        label = { PlayLabel(text = "GRB", playing = grbPlaying, activeColor = GRB_RED) }
    )

    PanelGap(8, horizontal)
    PanelOrientDivider(horizontal)
    PanelGap(8, horizontal)

    // 2. GJK (label lalu tombol)
    PlayLabeledItem(
        horizontal = horizontal,
        labelFirst = true,
        button = {
            PlayCircleButton(
                playing = gjkPlaying,
                activeColor = GJK_BLUE,
                contentDesc = if (gjkPlaying) "Stop GJK" else "Play GJK",
                onClick = onGjkToggle
            )
        },
        label = { PlayLabel(text = "GJK", playing = gjkPlaying, activeColor = GJK_BLUE) }
    )

    PanelGap(8, horizontal)
    PanelOrientDivider(horizontal)
    PanelGap(8, horizontal)

    // 3. 🔒 + 🔄 dalam 1 kolom (rotate nonaktif saat terkunci)
    ControlStack(
        locked = locked,
        onLockedChange = onLockedChange,
        onToggleOrientation = onToggleOrientation
    )

    PanelGap(8
