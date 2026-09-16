package awali.dengan.bismillah.xposed

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Entry point modul LEGACY (assets/xposed_init).
 * Target uji: LocTest (awali.dengan.bismillah.loctest) — aplikasi sendiri.
 *
 * Uji: Location.getLatitude/getLongitude pada proses LocTest dikembalikan
 * menjadi nilai uji. Jika LocTest menampilkan -6.123456 / 106.654321,
 * hook BEKERJA. Chip MOCK/ASLI tidak disentuh (isMock tidak di-hook).
 */
class MainHook : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "A-D-B"
        private const val TARGET_APP = "awali.dengan.bismillah.loctest"
        private const val TEST_LAT = -6.123456
        private const val TEST_LNG = 106.654321
    }

        override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Log TANPA syarat — diagnostik apakah modul ter-load ke proses ini
        XposedBridge.log("A-D-B: handleLoadPackage -> ${lpparam.packageName}")

        if (lpparam.packageName != TARGET_APP) return

        XposedBridge.log("A-D-B: LocTest terdeteksi, memasang hook...")
        try {
            val cl = lpparam.classLoader
            // ... dst (sisanya TIDAK berubah)
                ?: run {
                    XposedBridge.log("A-D-B: classLoader null")
                    return
                }

            XposedHelpers.findAndHookMethod(
                "android.location.Location",
                cl,
                "getLatitude",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: XC_MethodHook.MethodHookParam) {
                        param.result = TEST_LAT
                    }
                }
            )

            XposedHelpers.findAndHookMethod(
                "android.location.Location",
                cl,
                "getLongitude",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: XC_MethodHook.MethodHookParam) {
                        param.result = TEST_LNG
                    }
                }
            )

            XposedBridge.log("A-D-B: hook Location.getLatitude/getLongitude terpasang")
        } catch (t: Throwable) {
            XposedBridge.log("A-D-B: gagal memasang hook — $t")
            Log.e(TAG, "Gagal memasang hook", t)
        }
    }
}
