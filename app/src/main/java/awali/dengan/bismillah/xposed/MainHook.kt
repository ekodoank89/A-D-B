package awali.dengan.bismillah.xposed

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * Entry point modul LSPosed (API modern).
 * Placeholder — tambahkan logika hook di sini sesuai kebutuhan.
 */
class MainHook(
    base: XposedInterface,
    param: ModuleLoadedParam
) : XposedModule(base, param) {

    init {
        log("A-D-B module loaded")
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        super.onPackageLoaded(param)
        log("A-D-B: onPackageLoaded -> ${param.packageName}")
    }
}
