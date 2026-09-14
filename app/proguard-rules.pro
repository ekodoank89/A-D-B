# LSPosed / Xposed API Preservation
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**

# Keep Hook Entry Point
-keep class com.aya.module.hook.MainHook { *; }
