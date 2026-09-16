package awali.dengan.bismillah.xposed

import android.util.Log
import io.github.libxposed.api.AfterHookCallback
import io.github.libxposed.api.Hooker
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.XposedHooker

/**
 * Entry point modul MODERN (META-INF/xposed/java_init.list).
 * Target uji: LocTest (awali.dengan.bismillah.loctest) — aplikasi sendiri.
 *
 * Uji: Location.getLatitude/getLongitude pada proses LocTest dikembalikan
 * menjadi nilai uji. Jika LocTest menampilkan -6.123456 / 106.654321,
 * hook BEKERJA. Chip MOCK/ASLI tidak disentuh (isMock tidak di-hook).
 */
class MainHook(
    base: XposedInterface,
    param: ModuleLoadedParam
) : XposedModule(base, param) {

    companion object {
        private const val TAG = "A-D-B"
        private const val TARGET_APP = "awali.dengan.bismillah.loctest"
        private const val TEST_LAT = -6.123456
        private const val TEST_LNG = 106.654321
    }

    private fun slog(msg: String) {
        Log.i(TAG, msg)
        runCatching { log(msg) } // LSPosed module log; gagal -> diam saja
    }

    init {
        slog("Modul modern dimuat: process=${param.processName()}")
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        super.onPackageLoaded(param)
        if (param.packageName() != TARGET_APP) return

        slog("LocTest terdeteksi, memasang hook...")
        try {
            val locationClass =
                param.classLoader().loadClass("android.location.Location")

            val lat = locationClass.getMethod("getLatitude")
            val lng = locationClass.getMethod("getLongitude")

            runCatching { hook(lat, LatHooker::class.java) }
                .onFailure { Log.e(TAG, "Hook getLatitude gagal", it) }
            runCatching { hook(lng, LngHooker::class.java) }
                .onFailure { Log.e(TAG, "Hook getLongitude gagal", it) }

            slog("Hook Location.getLatitude/getLongitude terpasang")
        } catch (t: Throwable) {
            Log.e(TAG, "Gagal memasang hook", t)
        }
    }

    @XposedHooker
    class LatHooker : Hooker {
        companion object {
            @JvmStatic
            @AfterInvocation
            fun after(callback: AfterHookCallback) {
                callback.setResult(TEST_LAT)
            }
        }
    }

    @XposedHooker
    class LngHooker : Hooker {
        companion object {
            @JvmStatic
            @AfterInvocation
            fun after(callback: AfterHookCallback) {
                callback.setResult(TEST_LNG)
            }
        }
    }
}
