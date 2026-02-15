# ============================================================
# Gallery Jarvis - ProGuard / R8 Rules
# ============================================================

# --- TensorFlow Lite ---
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-keepclassmembers class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# --- SQLDelight ---
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# --- Coil (이미지 로딩) ---
-dontwarn coil.**

# --- Kotlin Serialization ---
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# --- Compose ---
-dontwarn androidx.compose.**

# --- 앱 데이터 모델 (리플렉션 사용 가능성) ---
-keep class com.hunknownn.galleryjarvis.** { *; }

# --- 일반 Android ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
