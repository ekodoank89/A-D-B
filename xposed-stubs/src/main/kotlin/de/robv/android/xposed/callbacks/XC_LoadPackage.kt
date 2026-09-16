package de.robv.android.xposed.callbacks

/**
 * STUB compile-only. Field asli = public field Java (bukan getter),
 * maka @JvmField wajib agar akses Kotlin compile ke field langsung.
 */
class XC_LoadPackage {

    class LoadPackageParam {
        @JvmField var packageName: String = ""
        @JvmField var processName: String = ""
        @JvmField var classLoader: ClassLoader? = null
        @JvmField var isFirstApplication: Boolean = false
    }
}
