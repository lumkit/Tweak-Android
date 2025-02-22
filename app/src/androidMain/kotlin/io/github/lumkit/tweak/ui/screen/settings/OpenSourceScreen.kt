package io.github.lumkit.tweak.ui.screen.settings

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.common.util.startBrowser
import io.github.lumkit.tweak.ui.component.DetailItem
import io.github.lumkit.tweak.ui.component.PlainTooltipBox
import io.github.lumkit.tweak.ui.component.ScreenScaffold
import io.github.lumkit.tweak.ui.component.SharedTransitionText
import io.github.lumkit.tweak.ui.screen.ScreenRoute

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun OpenSourceScreen(
    viewModel: OpenSourceViewModel = viewModel { OpenSourceViewModel() }
) {

    val context = LocalContext.current
    val licenseState by viewModel.licenseState.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = {
            SharedTransitionText(
                text = stringResource(R.string.text_open_sources)
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.padding(it)
                .fillMaxSize(),
        ) {
            items(licenseState) {
                PlainTooltipBox(
                    tooltip = {
                        Text(
                            text = it.tip
                        )
                    }
                ) {
                    DetailItem(
                        onClick = {
                            context.startBrowser(it.url)
                        },
                        title = {
                            Text(
                                text = it.title,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        subTitle = {
                            Text(
                                text = buildString {
                                    append(stringResource(R.string.text_author))
                                    append(": ")
                                    append(it.author)
                                }
                            )
                        },
                        actions = {
                            Icon(
                                painter = painterResource(R.drawable.ic_right),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}