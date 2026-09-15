package awali.dengan.bismillah.xposed

import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * Entry point modul LSPosed API modern (102).
 * Constructor 2 argumen sesuai kontrak framework — aman di-load
 * ke proses ter-scope tanpa error.
 */
class MainHook(
    base: XposedInterface,
    param: ModuleLoadedParam
) : XposedModule(base, param) {

    init {
        Log.i(TAG, "A-D-B module loaded (modern API 102)")
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        super.onPackageLoaded(param)
        when (param.packageName) {
            "com.gojek.partner", "com.grabtaxi.driver2" ->
                Log.i(TAG, "Target loaded: ${param.packageName}")
        }
    }

    companion object {
        private const val TAG = "A-D-B"
    }
}
