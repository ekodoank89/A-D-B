package awali.dengan.bismillah.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import awali.dengan.bismillah.R
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

// =====================================================================
// Ikon marker pin-shape — AMAN dari FC:
//   1. MapsInitializer.initialize() (double-safety, sudah juga di MainActivity)
//   2. runCatching: jika render bitmap gagal ->
//   3. fallback ke defaultMarker bawaan Google
// sizeDp = ukuran pin hasil render (default 34dp; pakai 18dp untuk
// marker jitter kecil seperti icon di chip koordinat)
// =====================================================================

@Composable
internal fun rememberPinMarkerIcon(
    tint: Color,
    fallbackHue: Float,
    sizeDp: Int = 34
): BitmapDescriptor {
    val context = LocalContext.current
    return remember(tint, sizeDp) {
        runCatching {
            MapsInitializer.initialize(context.applicationContext)
            val density = context.resources.displayMetrics.density
            val sizePx = (sizeDp * density).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_pin)!!
            drawable.setTint(tint.toArgb())
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }.getOrElse {
            BitmapDescriptorFactory.defaultMarker(fallbackHue)
        }
    }
}
