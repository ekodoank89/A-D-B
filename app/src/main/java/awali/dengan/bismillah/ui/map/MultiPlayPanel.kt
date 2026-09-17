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
// CONTOH: Panel 4 tombol PLAY/STOP (label 1..4) dalam 1 kontainer.
//   - Movable (drag bebas) + lock/unlock
//   - Clamp saat jari dilepas & saat ukuran berubah -> panel tidak
//     pernah tertinggal di luar layar
//   - Rotate vertikal <-> horizontal (tombol 🔄)
//       * Saat panel TERKUNCI (🔒): rotate NONAKTIF (tidak bisa di-tap)
//       * Saat panel UNLOCK (🔓): rotate AKTIF kembali
//   - 🔒 dan 🔄 digabung dalam SATU KOLOM (ControlStack)
// Orientation-aware: isi panel sama, wadah Column/Row yang berganti.
// =====================================================================

// Warna aktif per tombol 1..4
private val MULTI_COLORS = listOf(
    Color(0xFFE53935), // 1 merah
    Color(0xFF1E88E5), // 2 biru
    Color(0xFF2E7D32), // 3 hijau
    Color(0xFFF57C00)  // 4 oranye
)

// Delta koreksi agar panel (di posisi panelPos) masuk ke dalam layar.
// Mengembalikan Offset.Zero jika sudah di dalam batas.
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
internal fun MultiPlayPanel(
    playing: List<Boolean>,          // 4 status play/stop (index 0..3)
    onToggle: (Int) -> Unit,         // tap tombol index i
    horizontal: Boolean,             // false = vertikal, true = horizontal
    onToggleOrientation: () -> Unit,
    locked: Boolean,
    onLockedChange: (Boolean) -> Unit,
    dragOffset: Offset,
    onDragOffsetChange: (Offset) -> Unit,
    screenSize: IntSize,             // ukuran layar px (dari MapScreen)
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
                    // Clamp saat jari dilepas -> panel masuk batas
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
                        // Mekanik drag asli yang terbukti bekerja
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
//   [▶1][1] sep [▶2][2] sep [▶3][3] sep [▶4][4] sep [kolom 🔒+🔄]
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

    // 🔒 + 🔄 dalam 1 kolom (rotate nonaktif saat terkunci)
    ControlStack(
        locked = locked,
        onLockedChange = onLockedChange,
        onToggleOrientation = onToggleOrientation
    )
}

// Satu item: tombol bulat play/stop + label angka di bawahnya
@Composable
private fun MultiItem(
    index: Int,
    playing: Boolean,
    onClick: () -> Unit
) {
    // Guard: jika index melebihi palet (harusnya tidak), pakai abu agar tidak crash
    val accent = MULTI_COLORS.getOrElse(index) { Color(0xFF757575) }
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
