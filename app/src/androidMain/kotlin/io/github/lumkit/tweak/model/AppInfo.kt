package io.github.lumkit.tweak.model

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
import io.github.lumkit.tweak.data.AppType

@Immutable
data class AppInfo(
    val appName: String = "",
    val packageName: String = "",
    val notFound: Boolean = false,
    val selected: Boolean = false,
    val icon: Drawable? = null,
    val stateTags: CharSequence = "",
    val path: CharSequence = "",
    val dir: CharSequence = "",
    val enabled: Boolean = false,
    val suspended: Boolean = false,
    val updated: Boolean = false,
    val versionName: String = "",
    val versionCode: Int = 0,
    val appType: AppType = AppType.UNKNOWN,
    val sceneConfigInfo: SceneConfigInfo,
    val desc: CharSequence,
    val targetSdkVersion: Int,
    val minSdkVersion: Int,
) {
    companion object {
        fun getItem(): AppInfo {
            return AppInfo(
                sceneConfigInfo = SceneConfigInfo(""),
                desc = "",
                targetSdkVersion = 1,
                minSdkVersion = 1,
            )
        }
    }
}
