# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Bluetooth classes
-keep class android.bluetooth.** { *; }

# Keep WebView classes
-keep class android.webkit.** { *; }

# Keep JavaScript interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Bluetooth Serial
-keep class androidx.** { *; }
-dontwarn androidx.**
