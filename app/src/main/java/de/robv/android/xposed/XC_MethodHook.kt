package de.robv.android.xposed

import java.lang.reflect.Method

/**
 * STUB — mirror XC_MethodHook + XposedHelpers framework asli.
 * findAndHookMethod return Method (persis asli) agar descriptor cocok.
 */
object XposedHelpers {
    @JvmStatic
    fun findAndHookMethod(
        className: String,
        classLoader: ClassLoader,
        methodName: String,
        vararg parameterTypesAndCallback: Any?
    ): Method = throw UnsupportedOperationException("compile-only stub")
}

abstract class XC_MethodHook {

    protected open fun beforeHookedMethod(param: MethodHookParam) {}

    protected open fun afterHookedMethod(param: MethodHookParam) {}

    open class MethodHookParam {
        @JvmField var thisObject: Any? = null
        @JvmField var args: Array<Any?> = arrayOfNulls(0)
        @JvmField var method: Method? = null
        @JvmField var result: Any? = null
        @JvmField var throwable: Throwable? = null
    }
}
