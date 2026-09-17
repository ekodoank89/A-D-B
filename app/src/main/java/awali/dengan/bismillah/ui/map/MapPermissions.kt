package awali.dengan.bismillah.ui.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

// =====================================================================
// Sistem izin BERURUTAN + DOUBLE CHECK:
//   1. LOKASI (foreground) -> 2. LOKASI "Selalu izinkan" ->
//   3. NOTIFIKASI (Android 13+) -> 4. BATERAI "Tanpa pembatasan"
// Jika ada izin hilang saat kembali ke aplikasi (ON_RESUME),
// urutan dijalankan ulang.
// =====================================================================

private enum class PermissionStep { LOCATION, BACKGROUND, NOTIFICATION, BATTERY, DONE }

@Composable
internal fun rememberAppPermissions(): Boolean {
    val context = LocalContext.current

    var locationGranted by remember {
        mutableStateOf(
            isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    var step by remember { mutableStateOf(PermissionStep.LOCATION) }

    val lifecycleOwner = LocalLifecycleOwner.current

    // ===== Double check: setiap kembali ke aplikasi, verifikasi ulang =====
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                locationGranted =
                    isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)

                val anyMissing = !locationGranted ||
                    !isGranted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !isGranted(context, Manifest.permission.POST_NOTIFICATIONS)) ||
                    !isIgnoringBatteryOptimizations(context)

                if (anyMissing && step == PermissionStep.DONE) {
                    step = PermissionStep.LOCATION
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 1) Launcher LOKASI foreground
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        locationGranted =
            (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
            (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        step = PermissionStep.BACKGROUND
    }

    // 2) Launcher LOKASI "Selalu izinkan" (background)
    val bgLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        step = PermissionStep.NOTIFICATION
    }

    // 3) Launcher NOTIFIKASI
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        step = PermissionStep.BATTERY
    }

    // 4) Launcher BATERAI (dialog "Tanpa pembatasan")
    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        step = PermissionStep.DONE
    }

    LaunchedEffect(step) {
        when (step) {
            PermissionStep.LOCATION -> {
                if (locationGranted) {
                    step = PermissionStep.BACKGROUND
                } else {
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }

            PermissionStep.BACKGROUND -> {
                if (!locationGranted ||
                    isGranted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                ) {
                    step = PermissionStep.NOTIFICATION
                } else {
                    bgLocationLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    )
                }
            }

            PermissionStep.NOTIFICATION -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    isGranted(context, Manifest.permission.POST_NOTIFICATIONS)
                ) {
                    step = PermissionStep.BATTERY
                } else {
                    notifLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                }
            }

            PermissionStep.BATTERY -> {
                if (isIgnoringBatteryOptimizations(context)) {
                    step = PermissionStep.DONE
                } else {
                    batteryLauncher.launch(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                }
            }

            PermissionStep.DONE -> { /* Semua izin selesai diverifikasi */ }
        }
    }

    return locationGranted
}

private fun isGranted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun isIgnoringBatteryOptimizations(context: Context): Boolean =
    runCatching {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isIgnoringBatteryOptimizations(context.packageName)
    }.getOrDefault(false)
