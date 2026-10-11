# Skyjumpers Release R8 / ProGuard Keep Rules

# Hilt & Dagger
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }
-keep class dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }
-keepclassmembers,allowobfuscation class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers enum * { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, ElementValuePairs
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}

# Google Tink AEAD
-keep class com.google.crypto.tink.** { *; }

# Razorpay Checkout SDK
-keep class com.razorpay.** { *; }
-dontwarn com.razorpay.**

# ZXing Core
-keep class com.google.zxing.** { *; }

# Domain Data Models
-keep class com.curiouskids.cashapp.feature.**.domain.** { *; }
