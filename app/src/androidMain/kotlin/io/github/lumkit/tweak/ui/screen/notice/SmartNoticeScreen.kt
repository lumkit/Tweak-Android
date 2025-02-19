package io.github.lumkit.tweak.ui.screen.notice

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.ui.component.DevelopmentEmptyBox
import io.github.lumkit.tweak.ui.component.ScreenScaffold
import io.github.lumkit.tweak.ui.component.SharedTransitionText
import io.github.lumkit.tweak.ui.screen.ScreenRoute

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SmartNoticeScreen() {

    ScreenScaffold(
        sharedKey = ScreenRoute.SMART_NOTICE,
        title = {
            SharedTransitionText(
                text = stringResource(R.string.text_smart_notice),
            )
        }
    ) {
        DevelopmentEmptyBox()
    }
}