package de.robv.android.xposed

import de.robv.android.xposed.callbacks.XC_LoadPackage

/** STUB compile-only — entry point modul legacy. */
interface IXposedHookLoadPackage {
    fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam)
}
