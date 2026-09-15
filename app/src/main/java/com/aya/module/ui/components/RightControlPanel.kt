package com.aya.module.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RightControlPanel(
    onFullscreenClick: () -> Unit,
    onLockClick: () -> Unit,
    onZoomInClick: () -> Unit,
    onZoomOutClick: () -> Unit,
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
                icon = Icons.Default.CropFree,
                label = null,
                onClick = onFullscreenClick
            )
            IconButtonWithLabel(
                icon = Icons.Default.Lock,
                label = null,
                onClick = onLockClick
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 10.dp),
                color = Color.LightGray.copy(alpha = 0.6f)
            )

            IconButtonWithLabel(
                icon = Icons.Default.ZoomIn,
                label = null,
                onClick = onZoomInClick
            )
            IconButtonWithLabel(
                icon = Icons.Default.ZoomOut,
                label = null,
                onClick = onZoomOutClick
            )
        }
    }
}
