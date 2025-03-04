package io.github.lumkit.tweak

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.content.edit
import io.github.lumkit.tweak.data.CutoutRect
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.ui.local.json
import io.github.lumkit.tweak.ui.theme.AppTheme
import io.github.lumkit.tweak.ui.view.SmartNoticeWindow
import kotlinx.serialization.encodeToString

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            Main {
                AppTheme {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        App()
                    }
                }
            }
        }

        // 挖孔获取
        window.decorView.setOnApplyWindowInsetsListener { _, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val displayCutout = insets.displayCutout
                displayCutout?.apply {
                    val rectList = boundingRects.map {
                        CutoutRect(
                            left = it.left,
                            top = it.top,
                            right = it.right,
                            bottom = it.bottom
                        )
                    }.toList()

                    SmartNoticeWindow.updateCutout(rectList)

                    // 将挖孔保存到本地
                    val json = json.encodeToString(rectList)
                    TweakApplication.shared.edit(commit = true) {
                        putString(Const.SmartNotice.SMART_NOTICE_CUTOUT_RECT_LIST, json)
                    }
                }
            }
            insets
        }
    }
}