package io.github.lumkit.tweak.ui.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider

class TopAlignmentOffsetPositionProvider(
    val alignment: Alignment = Alignment.Center,
    val offset: IntOffset
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {

        val popupX = when (alignment) {
            Alignment.Start -> anchorBounds.left
            Alignment.CenterHorizontally -> anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
            Alignment.End -> anchorBounds.right - popupContentSize.width
            else -> anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        }.coerceIn(0, windowSize.width - popupContentSize.width)

        val spaceAbove = anchorBounds.top
        val spaceBelow = windowSize.height - anchorBounds.bottom

        val popupY = if (spaceAbove >= popupContentSize.height) {
            anchorBounds.top - popupContentSize.height + offset.y
        } else (if (spaceBelow >= popupContentSize.height) {
            anchorBounds.bottom + offset.y
        } else {
            if (spaceAbove >= spaceBelow) {
                anchorBounds.top - popupContentSize.height + offset.y
            } else {
                anchorBounds.bottom + offset.y
            }
        }).coerceIn(0, windowSize.height - popupContentSize.height)

        return IntOffset(popupX, popupY)
    }
}

class BottomAlignmentOffsetPositionProvider(
    val alignment: Alignment = Alignment.Center,
    val offset: IntOffset
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {

        val popupX = when (alignment) {
            Alignment.Start -> anchorBounds.left
            Alignment.CenterHorizontally -> anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
            Alignment.End -> anchorBounds.right - popupContentSize.width
            else -> anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        }.coerceIn(0, windowSize.width - popupContentSize.width)

        val spaceBelow = windowSize.height - anchorBounds.bottom
        val spaceAbove = anchorBounds.top

        val popupY = if (spaceBelow >= popupContentSize.height) {
            anchorBounds.bottom + offset.y
        } else (if (spaceAbove >= popupContentSize.height) {
            anchorBounds.top - popupContentSize.height + offset.y
        } else {
            if (spaceBelow >= spaceAbove) {
                anchorBounds.bottom + offset.y
            } else {
                anchorBounds.top - popupContentSize.height + offset.y
            }
        }).coerceIn(0, windowSize.height - popupContentSize.height)
        return IntOffset(popupX, popupY)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlainTooltipBox(
    modifier: Modifier = Modifier,
    positionProvider: PopupPositionProvider = remember {
        TopAlignmentOffsetPositionProvider(offset = IntOffset.Zero)
    },
    tooltip: @Composable () -> Unit,
    state: TooltipState = rememberTooltipState(),
    focusable: Boolean = true,
    enableUserInput: Boolean = true,
    content: @Composable () -> Unit,
) {

    TooltipBox(
        modifier = modifier,
        positionProvider = positionProvider,
        tooltip = {
            PlainTooltip {
                tooltip()
            }
        },
        state = state,
        focusable = focusable,
        enableUserInput = enableUserInput,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichTooltipBox(
    modifier: Modifier = Modifier,
    density: Density = LocalDensity.current,
    positionProvider: PopupPositionProvider = remember {
        TopAlignmentOffsetPositionProvider(offset = IntOffset(x = 0, y = - with(density) { 8.dp.roundToPx() }))
    },
    tooltip: @Composable () -> Unit,
    state: TooltipState = rememberTooltipState(),
    focusable: Boolean = true,
    enableUserInput: Boolean = true,
    content: @Composable () -> Unit,
) {

    TooltipBox(
        modifier = modifier,
        positionProvider = positionProvider,
        tooltip = {
            RichTooltip {
                tooltip()
            }
        },
        state = state,
        focusable = focusable,
        enableUserInput = enableUserInput,
        content = content,
    )
}