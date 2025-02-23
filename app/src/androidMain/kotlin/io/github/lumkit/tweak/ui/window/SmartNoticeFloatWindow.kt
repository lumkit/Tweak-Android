package io.github.lumkit.tweak.ui.window

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.lumkit.tweak.services.LocalSmartNoticeView
import io.github.lumkit.tweak.services.LocalSmartNoticeWindowManager
import io.github.lumkit.tweak.services.LocalSmartNoticeWindowParams
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SmartNoticeFloatWindow() {

    val density = LocalDensity.current
    val view = LocalSmartNoticeView.current
    val windowManager = LocalSmartNoticeWindowManager.current
    val layoutParams = LocalSmartNoticeWindowParams.current
    val coroutine = rememberCoroutineScope()

    var show by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier,
        contentAlignment = Alignment.TopStart
    ) {
        Surface(
            modifier = Modifier
                .wrapContentSize(),
            shape = RoundedCornerShape(28.dp),
            color = Color.Black,
            onClick = {
                show = !show
                coroutine.launch {
                    if (show) {
                        with(density) {
                            layoutParams.width = 400.dp.roundToPx()
                            layoutParams.height = 250.dp.roundToPx()
                        }
                        windowManager.updateViewLayout(view, layoutParams)
                    } else {
                        delay(700)
                        with(density) {
                            layoutParams.width = 100.dp.roundToPx()
                            layoutParams.height = 32.dp.roundToPx()
                        }
                        windowManager.updateViewLayout(view, layoutParams)
                    }
                }
            }
        ) {
            Box(
                modifier = Modifier.animateContentSize(
                    finishedListener = { init, target ->
                        println("init = [${init}], target = [${target}]")
                    }
                )
            ) {
                if (show) {
                    Box(
                        modifier = Modifier.size(400.dp, 250.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier.size(100.dp, 32.dp)
                    )
                }
            }
        }
    }
}