package io.github.lumkit.tweak.ui.component

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import io.github.lumkit.tweak.LocalAnimateContentScope
import io.github.lumkit.tweak.LocalScreenNavigationController
import io.github.lumkit.tweak.LocalSharedTransitionScope
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.ui.view.WaterMarkView

@Immutable
enum class DevelopmentStage {
    Debug, Testing, Release
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    stage: DevelopmentStage = DevelopmentStage.Release,
    navHostController: NavHostController = LocalScreenNavigationController.current,
    sharedKey: String? = null,
    sharedTransitionScope: SharedTransitionScope = LocalSharedTransitionScope.current,
    animatedContentScope: AnimatedContentScope = LocalAnimateContentScope.current,
    navigation: (@Composable () -> Unit)? = {
        IconButton(
            onClick = {
                navHostController.popBackStack()
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nav_back),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .then(
                        if (sharedKey.isNullOrBlank()) {
                            Modifier
                        } else {
                            sharedTransitionScope.run {
                                Modifier.sharedBounds(
                                    sharedContentState = rememberSharedContentState("${sharedKey}-icon"),
                                    animatedVisibilityScope = animatedContentScope
                                )
                            }
                        }
                    )
            )
        }
    },
    title: @Composable String?.() -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    with(sharedTransitionScope) {
        Box {
            Scaffold(
                modifier = modifier
                    .then(
                        if (sharedKey.isNullOrBlank()) {
                            Modifier
                        } else {
                            Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState("${sharedKey}-box"),
                                animatedVisibilityScope = animatedContentScope,
                            )
                        }
                    ),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                topBar = {
                    TopAppBar(
                        modifier = Modifier.fillMaxWidth(),
                        navigationIcon = {
                            navigation?.invoke()
                        },
                        title = {
                            title(sharedKey)
                        },
                        actions = actions,
                    )
                },
                content = content
            )

            when (stage) {
                DevelopmentStage.Debug -> {
                    AndroidView(
                        modifier = Modifier.fillMaxSize()
                            .clip(RoundedCornerShape(1.dp))
                            .alpha(.38f),
                        factory = {
                            WaterMarkView(it)
                        },
                        update = {
                            it.setText(it.context.getString(R.string.text_function_is_debug))
                        }
                    )
                }
                DevelopmentStage.Testing -> {
                    AndroidView(
                        modifier = Modifier.fillMaxSize()
                            .clip(RoundedCornerShape(1.dp))
                            .alpha(.38f),
                        factory = {
                            WaterMarkView(it)
                        },
                        update = {
                            it.setText(it.context.getString(R.string.text_function_is_testing))
                        }
                    )
                }
                DevelopmentStage.Release -> Unit
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun String?.SharedTransitionText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current
) {
    val animatedContentScope = LocalAnimateContentScope.current
    with(LocalSharedTransitionScope.current) {
        Text(
            text,
            modifier.then(
                if (this@SharedTransitionText.isNullOrBlank()) {
                    Modifier
                } else {
                    Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState("${this@SharedTransitionText}-title"),
                        animatedVisibilityScope = animatedContentScope,
                    )
                }
            ),
            color,
            fontSize,
            fontStyle,
            fontWeight,
            fontFamily,
            letterSpacing,
            textDecoration,
            textAlign,
            lineHeight,
            overflow,
            softWrap,
            maxLines,
            minLines,
            onTextLayout,
            style
        )
    }
}

@Composable
fun NavBackStackEntry.asSharedKey(): String? = this.arguments?.getString("sharedKey")