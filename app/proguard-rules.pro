# RailCam ProGuard / R8 rules
#
# AndroidX (Room, CameraX, Compose, DataStore), Coil and kotlinx.coroutines all ship
# their own consumer R8 rules, so no blanket "-keep class library.** { *; }" rules are
# needed here. Those blanket keeps stopped R8 from shrinking and optimising the libraries
# that do the most work at runtime (CameraX, Compose runtime), making release builds
# bigger and slower.

# App models: enum names are persisted in DataStore / Room and parsed with valueOf().
-keep class com.rishavmishra010.railcam.data.db.** { *; }
-keep class com.rishavmishra010.railcam.domain.model.** { *; }
