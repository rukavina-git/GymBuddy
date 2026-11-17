# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# Generated API client (build/generated/openapi): models are
# (de)serialised by Moshi's reflective KotlinJsonAdapterFactory, which
# needs their constructors, fields and Kotlin metadata intact.
-keep class com.rukavina.gymbuddy.data.remote.generated.models.** { *; }
-keep class kotlin.Metadata { *; }
# Crashlytics titles and groups non-fatals by exception class name;
# keep our own sync exceptions readable there.
-keepnames class com.rukavina.gymbuddy.data.sync.DiscardedOnLogoutException
-keepnames class com.rukavina.gymbuddy.data.sync.SyncException

# Active workout draft: stored as JSON via Moshi's reflective
# KotlinJsonAdapterFactory, so names must survive R8 or stored drafts
# break between releases (plan 4 step 03).
-keep class com.rukavina.gymbuddy.domain.model.ActiveWorkoutDraft { *; }
-keep class com.rukavina.gymbuddy.domain.model.ActiveWorkoutDraft$* { *; }
-keep enum com.rukavina.gymbuddy.domain.model.ExerciseTrackingType { *; }

# Data export (Plan 6 step 07, ADR 0015): ExportV1 is written by Moshi's
# reflective adapter, and its field names and enum names are the v1 file
# format. R8 must not rename or strip them.
-keep class com.rukavina.gymbuddy.domain.export.** { *; }
-keep enum com.rukavina.gymbuddy.domain.model.** { *; }

# Open-source licences page (Plan 9 step 01): the build-time report is
# read with Moshi's reflective KotlinJsonAdapterFactory.
-keep class com.rukavina.gymbuddy.ui.you.licences.Report { *; }
-keep class com.rukavina.gymbuddy.ui.you.licences.ReportLibrary { *; }
-keep class com.rukavina.gymbuddy.ui.you.licences.ReportLicence { *; }
