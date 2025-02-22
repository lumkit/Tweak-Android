package io.github.lumkit.tweak.ui.screen.notice

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.ui.component.ScreenScaffold
import io.github.lumkit.tweak.ui.component.SharedTransitionText

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SmartNoticeScreen() {



    ScreenScaffold(
        title = {
            SharedTransitionText(
                text = stringResource(R.string.text_smart_notice),
            )
        }
    ) {

    }
}