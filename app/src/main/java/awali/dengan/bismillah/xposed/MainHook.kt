package awali.dengan.bismillah.xposed

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Entry point modul LSPosed — format LEGACY klasik (assets/xposed_init).
 * Implement IXposedHookLoadPackage seperti modul legacy pada umumnya (spt AYA).
 * Stub de.robv.android.xposed.* hanya untuk compile — saat runtime
 * framework LSPosed menyediakan class aslinya (menimpa stub).
 */
class MainHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedBridge.log("A-D-B loaded in ${lpparam.packageName}")
        when (lpparam.packageName) {
            "com.gojek.partner", "com.grabtaxi.driver2" ->
                XposedBridge.log("A-D-B: target ter-scope aktif")
        }
    }
}
