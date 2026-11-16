-allowaccessmodification

-keep class com.hadedo.novaiptv.** { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }
-dontwarn org.videolan.libvlc.**
-keep class org.videolan.libvlc.** { *; }
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn org.json.**
-keep class org.json.** { *; }
