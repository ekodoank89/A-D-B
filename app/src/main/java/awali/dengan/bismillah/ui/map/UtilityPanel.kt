package awali.dengan.bismillah.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
// Panel utilitas — ICON-ONLY, vertikal:
//   [Autofocus+Kompas] -> [Terang/Gelap] -> [lock] -> [+] -> [-]
// Movable (drag) + lock — DRAG DI-CLAMP agar panel tidak keluar layar.
// =====================================================================

// Hitung delta drag yang sudah di-clamp agar panel tetap dalam layar.
private fun clampedDragDelta(
    panelPos: Offset,
    drag: Offset,
    panelSize: IntSize,
    screenSize: IntSize,
    marginPx: Float
): Offset {
    if (screenSize.width <= 0 || screenSize.height <= 0 ||
        panelSize.width <= 0 || panelSize.height <= 0
    ) return drag
    val minX = marginPx
    val maxX = (screenSize.width - panelSize.width - marginPx).toFloat()
    val minY = marginPx
    val maxY = (screenSize.height - panelSize.height - marginPx).toFloat()
    val targetX = (panelPos.x + drag.x).coerceIn(minX, maxX)
    val targetY = (panelPos.y + drag.y).coerceIn(minY, maxY)
    return Offset(targetX - panelPos.x, targetY - panelPos.y)
}

@Composable
internal fun UtilityPanel(
    darkMode: Boolean,
    onAutoFocus: () -> Unit,
    onToggleDark: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    locked: Boolean,
    onLockedChange: (Boolean) -> Unit,
    dragOffset: Offset,
    onDragOffsetChange: (Offset) -> Unit,
    screenSize: IntSize,
    modifier: Modifier = Modifier
) {
    val currentDragOffset by rememberUpdatedState(dragOffset)
    val currentLocked by rememberUpdatedState(locked)

    // Ukuran & posisi panel sendiri (diukur saat layout)
    var panelSize by remember { mutableStateOf(IntSize.Zero) }
    var panelPos by remember { mutableStateOf(Offset.Zero) }

    val marginPx = with(LocalDensity.current) { 16.dp.toPx() }

    // Safety net: koreksi posisi bila panel di luar batas
    LaunchedEffect(panelPos, panelSize, screenSize) {
        val delta = clampedDragDelta(panelPos, Offset.Zero, panelSize, screenSize, marginPx)
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
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (!currentLocked) {
                        onDragOffsetChange(
                            clampedDragDelta(
                                panelPos, dragAmount, panelSize, screenSize, marginPx
                            )
                        )
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Autofocus + Kompas (icon saja)
            UtilityButton(
                iconRes = R.drawable.ic_my_location,
                contentDesc = "Autofocus & Normalisasi Map",
                active = false,
                onClick = onAutoFocus
            )

            Spacer(Modifier.height(8.dp))

            // 2. Terang/Gelap (icon saja)
            UtilityButton(
                iconRes = R.drawable.ic_brightness,
                contentDesc = "Terang/Gelap",
                active = darkMode,
                onClick = onToggleDark
            )

            Spacer(Modifier.height(8.dp))

            // 3. Lock/unlock movable panel ini
            LockButton(locked = locked, onToggle = { onLockedChange(!locked) })

            Spacer(Modifier.height(8.dp))

            // 4. Zoom In (icon +, tap = langsung zoom maksimal)
            UtilityButton(
                iconRes = R.drawable.ic_plus,
                contentDesc = "Zoom In",
                active = false,
                onClick = onZoomIn
            )

            Spacer(Modifier.height(8.dp))

            // 5. Zoom Out (icon -, mundur 2 level)
            UtilityButton(
                iconRes = R.drawable.ic_minus,
                contentDesc = "Zoom Out",
                active = false,
                onClick = onZoomOut
            )
        }
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
