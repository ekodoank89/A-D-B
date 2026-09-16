package de.robv.android.xposed

/**
 * STUB — signature identik dengan framework asli (@JvmStatic = invokestatic).
 * Saat runtime, class asli dari LSPosed (boot classpath) menimpa stub ini
 * (parent-first delegation), sehingga stub ter-bundle tidak berbahaya.
 */
object XposedBridge {
    @JvmStatic
    fun log(text: String) {
        // no-op (stub)
    }
}
