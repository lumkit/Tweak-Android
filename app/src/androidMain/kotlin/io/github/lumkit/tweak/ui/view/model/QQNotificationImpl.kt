package io.github.lumkit.tweak.ui.view.model

import android.app.Notification
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.common.util.getDiveSize
import io.github.lumkit.tweak.common.util.getStatusBarHeight
import io.github.lumkit.tweak.ui.token.SmartNoticeCapsuleDefault
import io.github.lumkit.tweak.ui.view.SmartNoticeWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.min

class QQNotificationImpl(
    override val statusBarNotification: StatusBarNotification?,
    override val view: SmartNoticeWindow,
    override val scope: SmartNoticeWindow.SmartNoticeWindowScope,
    override val density: Density,
    private val expandedState: MutableStateFlow<Boolean>
) : NotificationImpl(
    statusBarNotification, view, scope, density
) {
    init {
        println(statusBarNotification)
    }

    override fun componentSize(): DpSize {
        val rect = scope.cutoutRect ?: Rect(0, 0, 0, 0)
        return with(density) {
            if (expandedState.value) {
                val diveSize = context.getDiveSize()
                DpSize(
                    width = min(
                        diveSize.width,
                        diveSize.height
                    ).toDp() - 28.dp * 2f,
                    height = SmartNoticeCapsuleDefault.Notification.Height
                )
            } else {
                val cutoutWidth = (rect.right - rect.left).toDp() + 12.dp * 2f
                DpSize(
                    width = cutoutWidth + 100.dp * 2,
                    height = 32.dp
                )
            }
        }
    }

    @Composable
    override fun Content() {
        val icon: Icon? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras?.getParcelable(
                Notification.EXTRA_LARGE_ICON,
                Icon::class.java
            )
        } else {
            extras?.getParcelable(
                Notification.EXTRA_LARGE_ICON
            )
        }

        val expanded by expandedState.collectAsStateWithLifecycle()

        if (expanded) {
            ExpandedContent()
        } else {
            with(density) {
                var cutoutWidth by remember { mutableStateOf(0.dp) }

                LaunchedEffect(Unit) {
                    val rect = scope.cutoutRect ?: Rect(0, 0, 0, 0)
                    cutoutWidth = (rect.right - rect.left).toDp() + 12.dp * 2f
                }

                ProvideTextStyle(
                    MaterialTheme.typography.bodyMedium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                view.minimize(false)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(start = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        // TODO 跳转到聊天页面
                                    }
                            ) {
                                AsyncImage(
                                    model = icon?.loadDrawable(context),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = extras?.getString(Notification.EXTRA_TITLE)
                                    ?: stringResource(R.string.text_notification_no_title),
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(modifier = Modifier.width(cutoutWidth))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = info?.appName
                                    ?: stringResource(R.string.text_notification_no_title),
                                softWrap = false,
                                overflow = TextOverflow.StartEllipsis,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        expandedState.value = true
                                        view.toast(
                                            componentSize = {
                                                val diveSize = context.getDiveSize()
                                                DpSize(
                                                    width = with(density) {
                                                        min(
                                                            diveSize.width,
                                                            diveSize.height
                                                        ).toDp() - 28.dp * 2f
                                                    },
                                                    height = SmartNoticeCapsuleDefault.Notification.Height
                                                )
                                            },
                                            offsetY = getStatusBarHeight().toFloat()
                                        ) { _, _ ->
                                            ExpandedContent()
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = info?.icon,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ExpandedContent() {
        val icon: Icon? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras?.getParcelable(
                Notification.EXTRA_LARGE_ICON,
                Icon::class.java
            )
        } else {
            extras?.getParcelable(
                Notification.EXTRA_LARGE_ICON
            )
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clickable {
                    expandedState.value = false
                    view.minimize(false)
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                modifier = Modifier.size(56.dp),
                model = icon?.loadDrawable(context),
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = extras?.getString(Notification.EXTRA_TITLE)
                        ?: stringResource(R.string.text_notification_no_title),
                    style = MaterialTheme.typography.bodyLarge,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    color = Color.White
                )
                Text(
                    text = extras?.getString(Notification.EXTRA_TEXT)
                        ?: stringResource(R.string.text_notification_no_content),
                    style = MaterialTheme.typography.bodyMedium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    color = Color.White.copy(alpha = .8f)
                )
            }

            Icon(
                painter = painterResource(R.drawable.ic_right),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.White.copy(alpha = .8f)
            )
        }
    }
}