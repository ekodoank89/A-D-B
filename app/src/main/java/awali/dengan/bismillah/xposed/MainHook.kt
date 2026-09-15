package awali.dengan.bismillah.xposed

/**
 * Entry point modul LSPosed — format LEGACY (assets/xposed_init).
 * Terbukti bekerja di LSPosed 2.1.1 (7790): badge LEGACY + rekomendasi
 * scope terbaca dari meta-data xposedscope di manifest.
 */
class MainHook {
    init {
        android.util.Log.i("A-D-B", "A-D-B module loaded (legacy)")
    }
}
