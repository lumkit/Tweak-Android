
-keep class io.github.lumkit.tweak.data.** { *; }
-dontwarn io.github.lumkit.tweak.data.**

-keep class io.github.lumkit.tweak.ui.local.** { *; }
-dontwarn io.github.lumkit.tweak.ui.local.**

-keep class io.github.lumkit.tweak.util.Aes
-dontwarn io.github.lumkit.tweak.util.Aes

-keep class java.util.concurrent.** { *; }
-dontwarn java.util.concurrent.**

-keep class android.support.v8.renderscript.** { *; }
-keep class androidx.renderscript.** { *; }

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# Uncomment for DexGuard only
#-keepresourcexmlelements manifest/application/meta-data@value=GlideModule