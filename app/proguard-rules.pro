# App-specific R8 rules (full mode, see gradle.properties). R8 reports no missing classes for this app.
# Most libraries ship their own rules, which end up in app/build/outputs/mapping/release/configuration.txt. The rules
# here state what the app relies on at run time and can't be seen in a build, so that a library update can't drop one
# unnoticed. docs/ARCHITECTURE.md §3.1 lists what to check on a device.

# Readable stack traces after retrace.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Navigation routes. Navigation finds the serializer through KClass.serializer(), which looks for the members below by
# name with reflection; a route that loses them ends the app with "Serializer for class ... is not found".
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,EnclosingMethod,Signature
-keep @kotlinx.serialization.Serializable class net.drehtuer.nextgallery.** { *; }
-keep class net.drehtuer.nextgallery.**$$serializer { *; }
-if @kotlinx.serialization.Serializable class net.drehtuer.nextgallery.**
-keep class net.drehtuer.nextgallery.<1>$Companion { kotlinx.serialization.KSerializer serializer(...); }

# Room creates <Database>_Impl by name from the database class.
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# WorkManager stores the workers' class names in its database, so renaming one would orphan queued work. The Hilt
# worker factory also finds them by name.
-keep class * extends androidx.work.ListenableWorker

# Tink reads its protobuf messages through their field names (https://github.com/tink-crypto/tink/issues/361).
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite { <fields>; }

# osmdroid: ArchiveFileFactory creates these with Class.newInstance(), and has no rules of its own.
-keep class * implements org.osmdroid.tileprovider.modules.IArchiveFile { <init>(); }

# OkHttp: its own rules cover it, and its optional TLS providers are not used. The public suffix list is an asset and
# PlatformInitializer a startup initializer, both found by name; the merged manifest and the APK contain them.
