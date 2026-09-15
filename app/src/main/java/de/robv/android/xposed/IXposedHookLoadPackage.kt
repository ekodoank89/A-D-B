package de.robv.android.xposed

import de.robv.android.xposed.callbacks.XC_LoadPackage

/** STUB compile-only — versi asli disediakan framework LSPosed saat runtime. */
interface IXposedHookLoadPackage {
    fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam)
}
