package awali.dengan.bismillah.xposed

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Entry point modul LEGACY (assets/xposed_init).
 * Kode ini HANYA berjalan di proses aplikasi ter-scope
 * (com.gojek.partner / com.grabtaxi.driver2).
 */
class MainHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Log ke LSPosed (Log -> tab Modul)
        XposedBridge.log("A-D-B: loaded in ${lpparam.packageName}")
        // Cadangan: logcat (tag: A-D-B)
        Log.i("A-D-B", "loaded in ${lpparam.packageName}")

        when (lpparam.packageName) {
            "com.gojek.partner", "com.grabtaxi.driver2" -> {
                XposedBridge.log("A-D-B: target ter-scope aktif")
                Log.i("A-D-B", "target ter-scope aktif")
            }
        }
    }
}
