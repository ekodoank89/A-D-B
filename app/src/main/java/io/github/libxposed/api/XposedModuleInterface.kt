package io.github.libxposed.api

interface XposedModuleInterface {
    interface ModuleLoadedParam {
        val isSystemServer: Boolean
        val processName: String
        val packageName: String
    }

    interface PackageLoadedParam {
        val isFirstPackage: Boolean
        val packageName: String
        val classLoader: ClassLoader
    }
}
