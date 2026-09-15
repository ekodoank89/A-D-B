package io.github.libxposed.api

abstract class XposedModule(
    base: XposedInterface,
    @Suppress("UNUSED_PARAMETER") param: XposedModuleInterface.ModuleLoadedParam
) : XposedModuleInterface {

    open fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {}
}
