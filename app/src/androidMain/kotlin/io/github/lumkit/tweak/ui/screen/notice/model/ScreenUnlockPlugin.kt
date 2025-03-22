package io.github.lumkit.tweak.ui.screen.notice.model

import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.databinding.SmartNoticeScreenUnlockedBinding
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.ui.component.DetailItem
import io.github.lumkit.tweak.ui.component.FolderItem
import io.github.lumkit.tweak.ui.view.SmartNoticeFactory

class ScreenUnlockPlugin(
    override val factory: SmartNoticeFactory
) : SmartNoticeNotificationPlugin(
    factory,
    Const.SmartNotice.Observe.SMART_NOTICE_OBSERVE_SCREEN_UNLOCKED
) {
    @Composable
    override fun PreferenceContent(plugin: SmartNoticeNotificationPlugin) {
        ScreenUnLockedObserver(plugin)
    }

    override fun onEnableChanged(enabled: Boolean) {
        super.onEnableChanged(enabled)
        if (enabled) {
            register()
        } else {
            unregister()
        }
    }

    private fun unregister() {
        try {
            factory.context.unregisterReceiver(screenStateReceiver)
        } catch (_: Exception) {
        }
    }

    private fun register() {
        try {
            val screenFilter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            factory.context.registerReceiver(screenStateReceiver, screenFilter)
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregister()
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_USER_PRESENT, Intent.ACTION_USER_UNLOCKED -> {
                    if (SmartNoticeFactory.gameModeState) {
                        return
                    }
                    unlocked()
                }

                Intent.ACTION_SCREEN_OFF -> {
                    factory.expanded = false
                    binding.icon.scaleX = 0f
                    binding.icon.scaleY = 0f
                }
            }
        }
    }

    init {
        loadEnabled()
        register()
    }

    private val binding = SmartNoticeScreenUnlockedBinding.bind(
        View.inflate(
            factory.context,
            R.layout.smart_notice_screen_unlocked,
            null
        )
    )

    fun unlocked() {
        binding.label.setText(R.string.text_unlocked)
        binding.icon.setImageResource(R.drawable.ic_unlock)
        binding.icon.setColorFilter("#FFFFFFFF".toColorInt())
        binding.root.setOnClickListener(factory::minimize)

        ObjectAnimator.ofFloat(
            binding.icon,
            "scaleX",
            binding.root.scaleX,
            1f
        ).apply {
            duration = SmartNoticeFactory.globalSmartNoticeData.value.duration
            interpolator = OvershootInterpolator()
            start()
        }

        ObjectAnimator.ofFloat(
            binding.icon,
            "scaleY",
            binding.root.scaleY,
            1f
        ).apply {
            duration = SmartNoticeFactory.globalSmartNoticeData.value.duration
            interpolator = OvershootInterpolator()
            start()
        }

        factory.toast(
            binding.root,
            contentSize = { _, _, _, _ ->
                DpSize(
                    220.dp,
                    32.dp
                )
            },
        )
    }
}

@Composable
private fun ScreenUnLockedObserver(plugin: SmartNoticeNotificationPlugin) {
    var dialogState by rememberSaveable { mutableStateOf(false) }

    DetailItem(
        onClick = {
            dialogState = true
        },
        title = {
            Text(
                text = stringResource(R.string.text_smart_notice_observe_screen_unlocked)
            )
        },
        subTitle = {
            Text(
                text = stringResource(R.string.text_smart_notice_observe_screen_unlocked_tips)
            )
        }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }

    ScreenUnlockedObserverDialog(dialogState, plugin) { dialogState = false }
}

@Composable
private fun ScreenUnlockedObserverDialog(
    visible: Boolean,
    plugin: SmartNoticeNotificationPlugin,
    onDismissRequest: () -> Unit,
) {
    if (visible) {
        AlertDialog(
            modifier = Modifier.padding(vertical = 28.dp),
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    text = stringResource(R.string.text_smart_notice_observe_screen_unlocked)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    SwitchRow(plugin)
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismissRequest
                ) {
                    Text(
                        text = stringResource(R.string.text_confirm)
                    )
                }
            },
            dismissButton = {
                FilledTonalButton(
                    onClick = onDismissRequest
                ) {
                    Text(
                        text = stringResource(R.string.text_cross)
                    )
                }
            }
        )
    }
}

@Composable
private fun SwitchRow(plugin: SmartNoticeNotificationPlugin) {
    val enabled by plugin.enableState.collectAsStateWithLifecycle()
    FolderItem(
        title = {
            Text(
                text = stringResource(R.string.text_notice_observe_plugin)
            )
        }
    ) {
        Switch(
            checked = enabled,
            onCheckedChange = {
                plugin.setEnabled(it)
                plugin.saveEnabled(plugin.sharedKey)
            }
        )
    }
}