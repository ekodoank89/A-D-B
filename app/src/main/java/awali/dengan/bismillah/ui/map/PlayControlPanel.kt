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
    jitterActive: Boolean,           // status tampilan tombol jitter
    onJitterClick: () -> Unit,       // buka dialog Jitter
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

    PanelGap(8, horizontal)
    PanelOrientDivider(horizontal)
    PanelGap(8, horizontal)

    // 4. Favorite — icon BINTANG EMAS
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

    PanelGap(8, horizontal)
    PanelOrientDivider(horizontal)
    PanelGap(8, horizontal)

    // 5. Jitter — buka dialog pengaturan jitter (GRB/GJK)
    UtilityButton(
        iconRes = R.drawable.ic_jitter,
        contentDesc = "Pengaturan jitter",
        active = jitterActive,
        size = 46.dp,
        iconSize = 22.dp,
        onClick = onJitterClick
    )
}

// Item tombol + label: vertikal = tumpuk, horizontal = berdampingan
// labelFirst = true -> label di atas/samping kiri (urutan GJK)
@Composable
private fun PlayLabeledItem(
    horizontal: Boolean,
    labelFirst: Boolean,
    button: @Composable () -> Unit,
    label: @Composable () -> Unit
) {
    if (horizontal) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (labelFirst) {
                label()
                Spacer(Modifier.width(5.dp))
                button()
            } else {
                button()
                Spacer(Modifier.width(5.dp))
                label()
            }
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (labelFirst) {
                label()
                Spacer(Modifier.height(5.dp))
                button()
            } else {
                button()
                Spacer(Modifier.height(5.dp))
                label()
            }
        }
    }
}

// Spacer orientation-aware: height saat vertikal, width saat horizontal
@Composable
private fun PanelGap(dps: Int, horizontal: Boolean) {
    if (horizontal) {
        Spacer(Modifier.width(dps.dp))
    } else {
        Spacer(Modifier.height(dps.dp))
    }
}

// Separator orientation-aware:
//   vertikal   -> garis mendatar 46dp
//   horizontal -> garis tegak tinggi 46dp
@Composable
private fun PanelOrientDivider(horizontal: Boolean) {
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

// Kontrol panel dalam 1 kolom: lock/unlock di atas, rotate di bawah.
// Saat panel TERKUNCI, tombol rotate dinonaktifkan.
@Composable
private fun ControlStack(
    locked: Boolean,
    onLockedChange: (Boolean) -> Unit,
    onToggleOrientation: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LockButton(locked = locked, onToggle = { onLockedChange(!locked) })
        Spacer(Modifier.height(4.dp))
        OrientationButton(
            enabled = !locked, // rotate hanya bisa di-tap saat unlock
            onClick = onToggleOrientation
        )
    }
}

// Tombol rotate orientasi — nonaktif (redup) saat panel terkunci
@Composable
private fun OrientationButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(28.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_swap),
            contentDescription = if (enabled) "Ubah orientasi panel"
            else "Buka kunci untuk mengubah orientasi",
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(16.dp)
        )
    }
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
