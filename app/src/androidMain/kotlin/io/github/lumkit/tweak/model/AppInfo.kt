package io.github.lumkit.tweak.model

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
import io.github.lumkit.tweak.data.AppType

@Immutable
data class AppInfo(
    var appName: String = "",
    var packageName: String = "",
    var notFound: Boolean = false,
    var selected: Boolean = false,
    var icon: Drawable? = null,
    var stateTags: CharSequence = "",
    var path: CharSequence = "",
    var dir: CharSequence = "",
    var enabled: Boolean = false,
    var suspended: Boolean = false,
    var updated: Boolean = false,
    var versionName: String = "",
    var versionCode: Int = 0,
    var appType: AppType = AppType.UNKNOWN,
    var sceneConfigInfo: SceneConfigInfo,
    var desc: CharSequence,
    var targetSdkVersion: Int,
    var minSdkVersion: Int,
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
