package io.github.libxposed.api

import java.lang.reflect.Executable

/**
 * STUB compile-only — libxposed API (modern).
 * Class asli disediakan framework LSPosed saat runtime (class injection),
 * sehingga stub TIDAK boleh ikut di-packaging ke APK (wajib compileOnly).
 */

interface XposedInterface {
    fun log(message: String)
    fun log(throwable: Throwable)
    fun hook(executable: Executable, hooker: Class<out Hooker>): Hooker
}

interface Hooker

interface BeforeHookCallback {
    fun getExecutable(): Executable
    fun getThisObject(): Any?
    fun getArgs(): Array<Any?>
    fun setArg(index: Int, value: Any?)
    fun returnAndSkip(returnValue: Any?)
    fun throwAndSkip(throwable: Throwable)
}

interface AfterHookCallback {
    fun getExecutable(): Executable
    fun getThisObject(): Any?
    fun getArgs(): Array<Any?>
    fun getResult(): Any?
    fun setResult(result: Any?)
    fun getThrowable(): Throwable?
    fun setThrowable(throwable: Throwable?)
}

interface XposedModuleInterface {
    interface ModuleLoadedParam {
        fun isSystemServer(): Boolean
        fun processName(): String
        fun packageName(): String?
    }

    interface PackageLoadedParam {
        fun isFirstPackage(): Boolean
        fun packageName(): String
        fun processName(): String
        fun classLoader(): ClassLoader
    }
}

abstract class XposedModule(
    base: XposedInterface,
    @Suppress("UNUSED_PARAMETER") param: XposedModuleInterface.ModuleLoadedParam
) : XposedModuleInterface {

    private val impl: XposedInterface = base

    fun log(message: String) = impl.log(message)
    fun log(throwable: Throwable) = impl.log(throwable)
    fun hook(executable: Executable, hooker: Class<out Hooker>): Hooker =
        impl.hook(executable, hooker)

    open fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {}
}
