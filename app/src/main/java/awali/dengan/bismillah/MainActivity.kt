package awali.dengan.bismillah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import awali.dengan.bismillah.ui.map.MapScreen
import awali.dengan.bismillah.ui.theme.ADBTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ADBTheme {
                MapScreen()
            }
        }
    }
}
