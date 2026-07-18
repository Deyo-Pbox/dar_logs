# Keep Room entities and DAOs
-keep class com.example.darlogs.data.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**
-keepattributes *Annotation*

# Keep Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Keep Bcrypt
-keep class at.favre.lib.crypto.bcrypt.** { *; }
-keep class at.favre.lib.bytes.** { *; }

# Keep WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Keep biometric
-keep class androidx.biometric.** { *; }

# Kotlin serialization / coroutines
-keepattributes InnerClasses
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep ViewBinding
-keep class * implements androidx.viewbinding.ViewBinding { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Keep JSON serialization/deserialization
-keepclassmembers class * {
    @org.json.JSONObject <init>(...);
}
