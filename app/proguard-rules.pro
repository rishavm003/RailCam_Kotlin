# RailCam ProGuard rules

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# --- Kotlin / coroutines ---
-keepattributes *Annotation*
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# --- App models (Room entities & domain) ---
-keep class com.rishavmishra010.railcam.data.db.** { *; }
-keep class com.rishavmishra010.railcam.domain.model.** { *; }

# --- CameraX ---
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# --- Coil ---
-keep class coil.** { *; }
-dontwarn coil.**

# --- Compose (runtime stability) ---
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# --- DataStore ---
-keep class androidx.datastore.*.** { *; }
