package io.github.lumkit.tweak.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GroupDetail(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
    ) {
        ProvideTextStyle(
            value = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.primary
            ),
        ) {
            Row {
                Spacer(modifier = Modifier.width(16.dp))
                title()
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
fun DetailItem(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
    subTitle: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.alpha(alpha = if (enabled) 1f else .45f)
            .clickable(enabled = enabled, indication = null, interactionSource = null, onClick = {})
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        icon?.let {
            Surface(
                modifier = Modifier.size(24.dp),
                color = Color.Transparent
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurface
                ) {
                    it()
                }
            }
        }
        Box(
            modifier = Modifier.weight(1f)
        ) {
            Column {
                ProvideTextStyle(
                    value = MaterialTheme.typography.titleMedium,
                ) {
                    Text("")
                }
                ProvideTextStyle(
                    value = MaterialTheme.typography.labelMedium,
                ) {
                    Text("")
                }
            }
            Column(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.titleMedium,
                ) {
                    title()
                }
                subTitle?.apply {
                    ProvideTextStyle(
                        value = MaterialTheme.typography.labelMedium,
                    ) {
                        this()
                    }
                }
            }
        }
        actions?.let {
            Spacer(modifier = Modifier.width(16.dp))
            it(this)
        }
    }
}

@Composable
fun DetailItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
    subTitle: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .alpha(alpha = if (enabled) 1f else .45f)
            .clickable(onClick = onClick, enabled = enabled)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        icon?.let {
            Surface(
                modifier = Modifier.size(24.dp),
                color = Color.Transparent
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurface
                ) {
                    it()
                }
            }
        }
        Box(
            modifier = Modifier.weight(1f)
        ) {
            Column {
                ProvideTextStyle(
                    value = MaterialTheme.typography.titleMedium,
                ) {
                    Text("")
                }
                ProvideTextStyle(
                    value = MaterialTheme.typography.labelMedium,
                ) {
                    Text("")
                }
            }
            Column(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.titleMedium,
                ) {
                    title()
                }
                subTitle?.apply {
                    ProvideTextStyle(
                        value = MaterialTheme.typography.labelMedium
                            .copy(color = MaterialTheme.colorScheme.outline),
                    ) {
                        this()
                    }
                }
            }
        }
        actions?.invoke(this)
    }
}