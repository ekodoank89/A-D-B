package de.robv.android.xposed.callbacks

/**
 * STUB — field asli = public field Java, maka @JvmField wajib
 * agar akses Kotlin compile ke field langsung (bukan getter).
 */
class XC_LoadPackage {

    class LoadPackageParam {
        @JvmField var packageName: String = ""
        @JvmField var processName: String = ""
        @JvmField var classLoader: ClassLoader? = null
        @JvmField var isFirstApplication: Boolean = false
    }
}
