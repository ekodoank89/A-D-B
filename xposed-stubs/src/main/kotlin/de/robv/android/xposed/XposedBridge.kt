package de.robv.android.xposed

/** STUB compile-only — versi asli disediakan framework LSPosed saat runtime. */
object XposedBridge {
    fun log(text: String) {
        android.util.Log.i("XposedBridge", text)
    }
}
