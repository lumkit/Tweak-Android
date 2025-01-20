package io.github.lumkit.tweak.ui.screen.notice

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.ui.component.ScreenScaffold

@Composable
fun SmartNoticeScreen() {

    ScreenScaffold(
        title = {
            Text(
                text = stringResource(R.string.text_smart_notice)
            )
        }
    ) {

    }
}