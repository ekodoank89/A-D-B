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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import awali.dengan.bismillah.R
import kotlin.math.roundToInt

// =====================================================================
// Panel utilitas — ICON-ONLY:
//   [🎯 Autofocus+Kompas] [☀️ Terang/Gelap] [kolom 🔒+🔄] [＋] [－]
//   - Rotate vertikal <-> horizontal (tombol 🔄)
//       * Saat panel TERKUNCI (🔒): rotate NONAKTIF (redup, tidak bisa di-tap)
//       * Saat panel UNLOCK (🔓): rotate AKTIF kembali
//   - Movable (drag bebas) + lock — clamp saat jari dilepas & saat
//     ukuran layar/panel berubah (bukan saat drag)
// Orientation-aware: isi panel sama, wadah Column/Row yang berganti.
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
internal fun UtilityPanel(
    darkMode: Boolean,
    onAutoFocus: () -> Unit,
    onToggleDark: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
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
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UtilityPanelItems(
                    horizontal = true,
                    darkMode = darkMode,
                    onAutoFocus = onAutoFocus,
                    onToggleDark = onToggleDark,
                    onZoomIn = onZoomIn,
                    onZoomOut = onZoomOut,
                    locked = locked,
                    onLockedChange = onLockedChange,
                    onToggleOrientation = onToggleOrientation
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UtilityPanelItems(
                    horizontal = false,
                    darkMode = darkMode,
                    onAutoFocus = onAutoFocus,
                    onToggleDark = onToggleDark,
                    onZoomIn = onZoomIn,
                    onZoomOut = onZoomOut,
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
//   [🎯] [☀️] [kolom 🔒+🔄] [＋] [－]
// =====================================================================

@Composable
private fun UtilityPanelItems(
    horizontal: Boolean,
    darkMode: Boolean,
    onAutoFocus: () -> Unit,
    onToggleDark: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    locked: Boolean,
    onLockedChange: (Boolean) -> Unit,
    onToggleOrientation: () -> Unit
) {
    // 1. Autofocus + Kompas
    UtilityButton(
        iconRes = R.drawable.ic_my_location,
        contentDesc = "Autofocus & Normalisasi Map",
        active = false,
        onClick = onAutoFocus
    )

    PanelGap(8, horizontal)

    // 2. Terang/Gelap
    UtilityButton(
        iconRes = R.drawable.ic_brightness,
        contentDesc = "Terang/Gelap",
        active = darkMode,
        onClick = onToggleDark
    )

    PanelGap(8, horizontal)

    // 3. 🔒 + 🔄 dalam 1 kolom (rotate nonaktif saat terkunci)
    ControlStack(
        locked = locked,
        onLockedChange = onLockedChange,
        onToggleOrientation = onToggleOrientation
    )

    PanelGap(8, horizontal)

    // 4. Zoom In (tap = langsung zoom maksimal)
    UtilityButton(
        iconRes = R.drawable.ic_plus,
        contentDesc = "Zoom In",
        active = false,
        onClick = onZoomIn
    )

    PanelGap(8, horizontal)

    // 5. Zoom Out (mundur 2 level)
    UtilityButton(
        iconRes = R.drawable.ic_minus,
        contentDesc = "Zoom Out",
        active = false,
        onClick = onZoomOut
    )
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

// Tombol bulat icon-only — ukuran & warna icon parameter
@Composable
internal fun UtilityButton(
    iconRes: Int,
    contentDesc: String,
    active: Boolean,
    onClick: () -> Unit,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    iconTint: Color? = null
) {
    Surface(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (active) activeColor else MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDesc,
                tint = iconTint ?: if (active) Color.White
                else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
internal fun LockButton(locked: Boolean, onToggle: () -> Unit) {
    IconButton(
        onClick = onToggle,
        modifier = Modifier.size(28.dp)
    ) {
        Icon(
            painter = painterResource(
                if (locked) R.drawable.ic_lock else R.drawable.ic_unlock
            ),
            contentDescription = if (locked) "Buka kunci" else "Kunci posisi",
            tint = if (locked) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}
