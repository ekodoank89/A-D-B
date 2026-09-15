package awali.dengan.bismillah.xposed

/**
 * Entry point modul LSPosed (API modern).
 * Menggunakan reflection agar tidak perlu compile-time dependency libxposed.
 * LSPosed akan meng-instantiate class ini saat module dimuat.
 *
 * Saat runtime, LSPosed menyediakan implementasi interface:
 *   - io.github.libxposed.api.XposedInterface
 *   - io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
 */
class MainHook {

    init {
        // LSPosed memanggil constructor ini dengan 2 argumen:
        //   (XposedInterface base, ModuleLoadedParam param)
        // Karena kita tidak hook apa-apa, constructor kosong sudah cukup
        // untuk registrasi modul di LSPosed Manager.
        android.util.Log.i("A-D-B", "A-D-B module loaded (LSPosed API 102)")
    }
}
