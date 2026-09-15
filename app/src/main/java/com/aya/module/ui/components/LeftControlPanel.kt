package com.aya.module.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LeftControlPanel(
    isLocked: Boolean,
    onPointAClick: () -> Unit,
    onPointBClick: () -> Unit,
    onLockClick: () -> Unit,
    onFavClick: () -> Unit,
    onJitterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.width(60.dp),
        shape = RoundedCornerShape(30.dp),
        color = Color(0xFFEBF1F5).copy(alpha = 0.9f),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButtonWithLabel(
                icon = Icons.Default.PlayArrow,
                label = "A",
                onClick = onPointAClick
            )
            IconButtonWithLabel(
                icon = Icons.Default.PlayArrow,
                label = "B",
                onClick = onPointBClick
            )
            IconButtonWithLabel(
                icon = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                label = null,
                onClick = onLockClick
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 10.dp),
                color = Color.LightGray.copy(alpha = 0.6f)
            )

            IconButtonWithLabel(
                icon = Icons.Default.Star,
                label = "Fav",
                onClick = onFavClick
            )
            IconButtonWithLabel(
                icon = Icons.Default.Shuffle,
                label = "Jitter",
                onClick = onJitterClick
            )
        }
    }
}

@Composable
fun IconButtonWithLabel(
    icon: ImageVector,
    label: String?,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFD6E4F0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label ?: "Button",
                tint = Color(0xFF1E293B),
                modifier = Modifier.size(20.dp)
            )
        }
        if (label != null) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF475569),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
