package awali.dengan.bismillah.loctest

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LocTestScreen()
                }
            }
        }
    }
}

// Deteksi mock: API 31+ = isMock, di bawahnya isFromMockProvider
private fun Location.mockStatus(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isMock
    else @Suppress("DEPRECATION") isFromMockProvider

private val TIME_FMT = SimpleDateFormat("HH:mm:ss", Locale.US)

@Composable
fun LocTestScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // ===== Izin lokasi =====
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
            (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
    }

    LaunchedEffect(Unit) {
        if (!granted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // ===== State tampilan =====
    var lat by remember { mutableStateOf<Double?>(null) }
    var lng by remember { mutableStateOf<Double?>(null) }
    var mock by remember { mutableStateOf<Boolean?>(null) }
    var provider by remember { mutableStateOf("--") }
    var fixTime by remember { mutableStateOf("--") }
    var accuracy by remember { mutableStateOf<Float?>(null) }
    var lastKnownInfo by remember { mutableStateOf("Belum diambil") }

    // ===== Listener lokasi live — TITIK UJI HOOK: onLocationChanged =====
    val listener = remember {
        object : LocationListener {
            override fun onLocationChanged(location: Location) {
                lat = location.latitude
                lng = location.longitude
                mock = location.mockStatus()
                provider = location.provider ?: "--"
                fixTime = TIME_FMT.format(Date(location.time))
                accuracy = location.accuracy
            }
        }
    }

    DisposableEffect(granted) {
        var lm: LocationManager? = null
        if (granted) {
            lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            try {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1000L, 0.5f, listener, Looper.getMainLooper()
                )
            } catch (_: IllegalArgumentException) { /* provider tidak ada */ }
            try {
                lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 1000L, 0.5f, listener, Looper.getMainLooper()
                )
            } catch (_: IllegalArgumentException) { /* provider tidak ada */ }
        }
        onDispose { lm?.removeUpdates(listener) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "LocTest — Target Uji Hook",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Aplikasi sendiri untuk latihan modul LSPosed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        // ===== Kartu koordinat live =====
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "LATITUDE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = lat?.let { String.format(Locale.US, "%.6f", it) } ?: "--",
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "LONGITUDE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = lng?.let { String.format(Locale.US, "%.6f", it) } ?: "--",
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                // ===== Chip status MOCK / ASLI =====
                val m = mock
                val (label, bg) = when (m) {
                    true -> "MOCK LOCATION" to Color(0xFFC62828)
                    false -> "LOKASI ASLI" to Color(0xFF2E7D32)
                    null -> "MENUNGGU FIX..." to MaterialTheme.colorScheme.surfaceVariant
                }
                val fg = if (m == null) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                Text(
                    text = label,
                    color = fg,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(bg)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )

                Spacer(Modifier.height(12.dp))
                InfoRow("Provider", provider)
                InfoRow("Waktu fix", fixTime)
                InfoRow("Akurasi", accuracy?.let { "~${it.toInt()} m" } ?: "--")
            }
        }

        Spacer(Modifier.height(16.dp))

        // ===== Uji getLastKnownLocation — TITIK UJI HOOK klasik =====
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Uji getLastKnownLocation()",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val lm =
                            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                        val loc: Location? = try {
                            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        } catch (_: SecurityException) {
                            null
                        }
                        lastKnownInfo = if (loc != null) {
                            String.format(
                                Locale.US,
                                "%.6f, %.6f | mock=%s",
                                loc.latitude, loc.longitude, loc.mockStatus().toString()
                            )
                        } else {
                            "null (belum ada fix)"
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) { Text("Ambil LastKnownLocation") }
                Spacer(Modifier.height(6.dp))
                Text(
                    lastKnownInfo,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ===== Panduan titik uji hook =====
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Titik uji hook yang tersedia:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Bullet("LocationListener.onLocationChanged() — kartu koordinat live")
                Bullet("LocationManager.getLastKnownLocation() — tombol uji di atas")
                Bullet("Location.isMock / isFromMockProvider — chip MOCK/ASLI")
                Spacer(Modifier.height(6.dp))
                Text(
                    "Cara uji: Developer Options → Select mock location app → jalankan app mock → " +
                        "bandingkan tampilan LocTest (koordinat & status MOCK).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text("•  ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
