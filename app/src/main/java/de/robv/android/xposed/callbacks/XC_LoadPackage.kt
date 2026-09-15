package de.robv.android.xposed.callbacks

/** STUB compile-only — versi asli disediakan framework LSPosed saat runtime. */
class XC_LoadPackage {

    class LoadPackageParam {
        var packageName: String = ""
        var processName: String = ""
        var classLoader: ClassLoader? = null
        var isFirstApplication: Boolean = false
    }
}
