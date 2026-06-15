# Add project specific ProGuard rules here.
# Keep Room entities and DAOs
-keep class com.callscheduler.data.model.** { *; }
-keep class com.callscheduler.data.repository.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
