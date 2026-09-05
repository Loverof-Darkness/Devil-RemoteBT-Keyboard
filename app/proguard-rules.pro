# Project-specific R8 rules.
# Keep Android HID callbacks and Bluetooth profile classes reachable through
# framework registration/callback dispatch.
-keep class com.loverofdarkness.remotebtkeyboard.bluetooth.** { *; }

-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
