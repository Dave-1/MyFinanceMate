# ============================================================
# MyFinanceMate ProGuard / R8 Rules
# ============================================================

# --- Keep ALL app code (safest approach for Hilt/Room) ---
-keep class com.myfinancemate.** { *; }
-keepclassmembers class com.myfinancemate.** { *; }

# --- Keep ALL generated Hilt/Dagger code ---
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-keep class **_HiltModules* { *; }
-keep class **_HiltComponents* { *; }
-keep class **_GeneratedInjector { *; }
-keep class **_ContextKey { *; }
-keep class **_Binding { *; }
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <fields>;
    @javax.inject.* <fields>;
    @javax.inject.* <init>(...);
}

# --- Keep ALL Room code ---
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.TypeConverter class * { *; }
-keep class **_Impl { *; }
-keep class **_Dao { *; }
-dontwarn androidx.room.paging.**

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keepclassmembers class kotlin.coroutines.** { *; }

# --- AndroidX / Compose ---
-keep class androidx.** { *; }
-keepclassmembers class androidx.** { *; }
-dontwarn androidx.compose.**

# --- WorkManager ---
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# --- Navigation ---
-keepnames class * extends android.os.Parcelable
-keepnames class * extends java.io.Serializable

# --- SimpleFramework ---
-dontwarn org.simpleframework.**
-keep class org.simpleframework.** { *; }

# --- General ---
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions
-renamesourcefileattribute SourceFile
