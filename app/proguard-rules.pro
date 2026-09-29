# ============================================================================
# General ProGuard / R8 Optimization & Debugging Rules
# ============================================================================
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Preserve line numbers and source file attributes for Crashlytics / Stack traces
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# ============================================================================
# Application Specific Keep Rules
# ============================================================================
# Keep models / data classes (for Firebase, Gson, JSON serialization, etc.)
-keep class com.iconchanger.rollingicons.model.** { *; }
-keep class com.iconchanger.rollingicons.utils.RemoteConfigs { *; }
-keep class com.iconchanger.rollingicons.utils.LogEvent { *; }

# Keep View Binding & Custom Views
-keep class com.iconchanger.rollingicons.widget.** { *; }
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ============================================================================
# libGDX & Box2D Rules (Crucial for JNI / Native C++ integration)
# ============================================================================
-keep class com.badlogic.gdx.backends.android.** { *; }
-keep class com.badlogic.gdx.** { *; }
-keep class com.badlogic.gdx.physics.box2d.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================================================
# Kotlin & Coroutines
# ============================================================================
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlinx.coroutines.android.AndroidDispatcherFactory {
    public <init>();
}
-keepclassmembers class kotlinx.coroutines.CoroutineExceptionHandler {
    public <init>();
}

# ============================================================================
# Coil (Image Loading)
# ============================================================================
-keep class coil.** { *; }
-dontwarn coil.**

# ============================================================================
# Lottie Animations
# ============================================================================
-keep class com.airbnb.lottie.** { *; }

# ============================================================================
# Firebase & Google Play Services
# ============================================================================
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ============================================================================
# Ads & Mediation SDKs (Google Mobile Ads, Meta, AppLovin, InMobi, Pangle, etc.)
# ============================================================================

# Google Mobile Ads
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# CSC Ads library
-keep class com.app.library.** { *; }
-keep class com.app.** { *; }

# Facebook / Meta Audience Network
-keep class com.facebook.ads.** { *; }
-dontwarn com.facebook.ads.**

# AppLovin
-keep class com.applovin.** { *; }
-dontwarn com.applovin.**

# InMobi
-keep class com.inmobi.** { *; }
-dontwarn com.inmobi.**

# Pangle (Bytedance)
-keep class com.bytedance.sdk.openadsdk.** { *; }
-dontwarn com.bytedance.sdk.openadsdk.**

# Mintegral
-keep class com.mbridge.msdk.** { *; }
-dontwarn com.mbridge.msdk.**

# Unity Ads
-keep class com.unity3d.ads.** { *; }
-keep class com.unity3d.services.** { *; }
-dontwarn com.unity3d.ads.**

# Liftoff / Vungle
-keep class com.vungle.warren.** { *; }
-dontwarn com.vungle.warren.**
