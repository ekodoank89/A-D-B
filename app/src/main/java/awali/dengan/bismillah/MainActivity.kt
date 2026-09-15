package awali.dengan.bismillah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import awali.dengan.bismillah.ui.map.MapScreen
import awali.dengan.bismillah.ui.theme.ADBTheme
import com.google.android.gms.maps.MapsInitializer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // FIX FC: inisialisasi Maps SDK lebih awal, SEBELUM composition Compose,
        // agar BitmapDescriptorFactory aman dipakai untuk custom marker icon.
        try {
            MapsInitializer.initialize(applicationContext)
        } catch (_: Exception) {
            // Play services belum tersedia — biarkan map handle sendiri nanti
        }

        setContent {
            ADBTheme {
                MapScreen()
            }
        }
    }
}
