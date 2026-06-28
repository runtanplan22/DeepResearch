# Proguard rules for DeepResearch app
# Keep Retrofit interfaces
-keep,allowobfuscation interface com.deepresearch.app.data.api.*ApiService

# Keep data classes used by Gson
-keep class com.deepresearch.app.data.api.** { *; }
-keep class com.deepresearch.app.data.model.** { *; }

# Keep Room entities
-keep class com.deepresearch.app.data.local.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
