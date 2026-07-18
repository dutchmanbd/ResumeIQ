# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# --- Gson ---
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
-keep class * implements java.io.Serializable { *; }
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# --- Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keep class kotlinx.coroutines.** { *; }

# --- Hilt / Dagger ---
-keep,allowobfuscation,allowshrinking interface dagger.hilt.internal.GeneratedEntryPoint
-keep,allowobfuscation,allowshrinking @dagger.hilt.internal.GeneratedEntryPoint class *
-keep class dagger.** { *; }
-keep class hilt_aggregated_deps.** { *; }

# --- Room ---
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.** { *; }

# --- App Models ---
-keep class com.dutchman.resumeiq.domain.models.** { *; }
-keep class com.dutchman.resumeiq.data.local.entity.** { *; }
-keep class com.dutchman.resumeiq.data.remote.dto.** { *; }

# --- Firebase ---
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# --- ML Kit / MediaPipe ---
-keep class com.google.mlkit.** { *; }
-keep class com.google.mediapipe.** { *; }

# --- Coil ---
-keep class coil.** { *; }

# --- Compose ---
-keep class androidx.compose.** { *; }

# --- WorkManager ---
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(...);
}
-keep class com.dutchman.resumeiq.data.worker.** { *; }

# --- Accompanist ---
-keep class com.google.accompanist.** { *; }

# --- CameraX ---
-keep class androidx.camera.** { *; }

# --- Material / AndroidX General ---
-keep class com.google.android.material.** { *; }
-keep class androidx.** { *; }

# --- LiteRT ---
-keep class com.google.mediapipe.tasks.** { *; }
-keep class org.tensorflow.lite.** { *; }

# --- R8 Missing Classes Fixes ---
-dontwarn com.google.mediapipe.proto.CalculatorProfileProto$CalculatorProfile
-dontwarn com.google.mediapipe.proto.GraphTemplateProto$CalculatorGraphTemplate
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn org.apache.log.Hierarchy
-dontwarn org.apache.log.Logger
-dontwarn org.apache.log4j.Level
-dontwarn org.apache.log4j.Logger
-dontwarn org.apache.log4j.Priority
-dontwarn org.ietf.jgss.GSSCredential
