package io.github.lumkit.tweak.ui.screen.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.ui.component.BottomAlignmentOffsetPositionProvider
import io.github.lumkit.tweak.ui.component.PlainTooltipBox
import io.github.lumkit.tweak.ui.screen.main.page.FunctionPage
import io.github.lumkit.tweak.ui.screen.main.page.MinePage
import io.github.lumkit.tweak.ui.screen.main.page.OverviewPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Immutable
data class NavItem(
    val label: String,
    val unselected: Painter,
    val selected: Painter,
)

@Composable
fun MainScreen() {
    val coroutineScope = rememberCoroutineScope()

    val functionLabel = stringResource(R.string.nav_function)
    val overviewLabel = stringResource(R.string.nav_overview)
    val mineLabel = stringResource(R.string.nav_mine)

    val functionPainterUnselected = painterResource(R.drawable.ic_function_unselected)
    val overviewPainterUnselected = painterResource(R.drawable.ic_overview_unselected)
    val minePainterUnselected = painterResource(R.drawable.ic_min_unselected)

    val functionPainterSelected = painterResource(R.drawable.ic_function_selected)
    val overviewPainterSelected = painterResource(R.drawable.ic_overview_selected)
    val minePainterSelected = painterResource(R.drawable.ic_min_selected)

    val navItems = remember {
        listOf(
            NavItem(
                label = functionLabel,
                unselected = functionPainterUnselected,
                selected = functionPainterSelected
            ),
            NavItem(
                label = overviewLabel,
                unselected = overviewPainterUnselected,
                selected = overviewPainterSelected
            ),
            NavItem(
                label = mineLabel,
                unselected = minePainterUnselected,
                selected = minePainterSelected
            ),
        )
    }

    val pagerState = rememberPagerState(initialPage = 1) { navItems.size }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            ToolBar(coroutineScope, pagerState, navItems)
        }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            beyondViewportPageCount = navItems.size - 1
        ) { position ->
            when (position) {
                0 -> FunctionPage()
                1 -> OverviewPage()
                2 -> MinePage()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolBar(coroutineScope: CoroutineScope, pagerState: PagerState, navItems: List<NavItem>) {
    TopAppBar(
        modifier = Modifier.fillMaxWidth(),
        title = {
            NavBar(coroutineScope = coroutineScope, pagerState = pagerState, navItems = navItems)
        },
        actions = {

        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavBar(
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope,
    pagerState: PagerState,
    navItems: List<NavItem>
) {
    val hapticFeedback = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEachIndexed { index, navItem ->
            val selected = index == pagerState.currentPage

            val indicatorBoxColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = .8f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )

            val indicatorColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )

            PlainTooltipBox(
                positionProvider = BottomAlignmentOffsetPositionProvider(
                    alignment = Alignment.Center,
                    IntOffset(0, with(LocalDensity.current) { 4.dp.roundToPx() }),
                ),
                tooltip = {
                    Text(navItem.label)
                }
            ) {
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(
                            indication = null,
                            interactionSource = null,
                        ) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                    color = indicatorBoxColor,
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = if (selected) {
                                navItem.selected
                            } else {
                                navItem.unselected
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = indicatorColor
                        )
                        AnimatedVisibility(
                            visible = selected
                        ) {
                            Text(
                                text = navItem.label,
                                color = indicatorColor,
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}