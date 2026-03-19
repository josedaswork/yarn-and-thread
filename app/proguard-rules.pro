# Keep Room entities
-keep class com.yarnandthread.app.model.** { *; }
-keep class com.yarnandthread.app.database.** { *; }

# Keep Gson for JSON serialization
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
