# ============================================================
# MyFinanceMate ProGuard / R8 Rules
# ============================================================

# --- General ---
-dontwarn org.simpleframework.**
-keep class org.simpleframework.** { *; }
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlin.coroutines.safe.** { *; }

# --- Kotlin Serialization (if used) ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# --- Room Database ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.TypeConverter class *
-dontwarn androidx.room.paging.**

# Keep Room entity classes (they are accessed via reflection by Room compiler)
-keep class com.myfinancemate.data.local.entity.** { *; }
-keep class com.myfinancemate.data.local.dao.** { *; }
-keep class com.myfinancemate.data.local.typeconverter.** { *; }
-keep class com.myfinancemate.data.local.AppDatabase { *; }

# Keep Room's generated implementation classes
-keep class **_Impl { *; }
-keep class **_Dao { *; }

# --- Hilt / Dagger ---
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$ViewWithFragmentInContextBindingContext { *; }

# Keep Hilt generated components
-keep class **_HiltModules* { *; }
-keep class **_HiltComponents* { *; }
-keep class **_GeneratedInjector { *; }
-keep class **_MembersInjector { *; }
-keep class **_Factory { *; }
-keep class **_ContextKey { *; }

# Keep classes that Hilt injects into
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# --- AndroidX Compose ---
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# --- Biometric ---
-keep class androidx.biometric.** { *; }

# --- WorkManager ---
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.myfinancemate.worker.** { *; }

# --- Navigation Compose ---
-keepnames class * extends android.os.Parcelable
-keepnames class * extends java.io.Serializable

# --- Prevent R8 from stripping interface names needed for serialization ---
-keepnames interface * { *; }

# --- Data classes (used as migration args, Room entities, etc.) ---
-keep class com.myfinancemate.data.model.** { *; }
-keep class com.myfinancemate.data.repository.** { *; }

# --- General Android ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
