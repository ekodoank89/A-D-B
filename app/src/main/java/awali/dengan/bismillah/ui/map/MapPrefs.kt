package awali.dengan.bismillah.ui.map

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.LatLng

// =====================================================================
// State PERSISTEN — disimpan ke SharedPreferences di setiap perubahan
// (SideEffect), dimuat ulang saat aplikasi dibuka (termasuk setelah
// force stop). LocalContext.current dibaca DI LUAR remember {}.
// =====================================================================

internal const val PREFS_NAME = "adb_persistent_state"

@Composable
internal fun rememberPersistentBoolean(key: String, default: Boolean): MutableState<Boolean> {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val state = remember { mutableStateOf(prefs.getBoolean(key, default)) }
    SideEffect {
        prefs.edit().putBoolean(key, state.value).apply()
    }
    return state
}

// LatLng? — null disimpan dengan menghapus key (chip kosong / marker hilang)
@Composable
internal fun rememberPersistentLatLng(key: String): MutableState<LatLng?> {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val state = remember {
        mutableStateOf<LatLng?>(
            if (prefs.contains("${key}_lat") && prefs.contains("${key}_lng")) {
                LatLng(
                    Double.fromBits(prefs.getLong("${key}_lat", 0L)),
                    Double.fromBits(prefs.getLong("${key}_lng", 0L))
                )
            } else {
                null
            }
        )
    }
    SideEffect {
        prefs.edit().apply {
            val v = state.value
            if (v == null) {
                remove("${key}_lat")
                remove("${key}_lng")
            } else {
                putLong("${key}_lat", v.latitude.toRawBits())
                putLong("${key}_lng", v.longitude.toRawBits())
            }
        }.apply()
    }
    return state
}

// Offset (posisi geser panel)
@Composable
internal fun rememberPersistentOffset(key: String, default: Offset): MutableState<Offset> {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val state = remember {
        mutableStateOf(
            if (prefs.contains("${key}_x") && prefs.contains("${key}_y")) {
                Offset(
                    prefs.getFloat("${key}_x", 0f),
                    prefs.getFloat("${key}_y", 0f)
                )
            } else {
                default
            }
        )
    }
    SideEffect {
        prefs.edit()
            .putFloat("${key}_x", state.value.x)
            .putFloat("${key}_y", state.value.y)
            .apply()
    }
    return state
}
