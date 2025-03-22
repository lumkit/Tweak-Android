package io.github.lumkit.tweak.ui.screen.notice.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.view.View
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.databinding.SmartNoticeScreenUnlockedBinding
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.ui.component.DetailItem
import io.github.lumkit.tweak.ui.component.FolderItem
import io.github.lumkit.tweak.ui.view.SmartNoticeFactory

class VolumePlugin(
    override val factory: SmartNoticeFactory
) : SmartNoticeNotificationPlugin(
    factory,
    Const.SmartNotice.Observe.SMART_NOTICE_OBSERVE_VOLUME_CHANGED
) {
    @Composable
    override fun PreferenceContent(plugin: SmartNoticeNotificationPlugin) {
        VolumeChangedObserver(plugin)
    }

    private var onChangedTimes = -1
    private val volumeChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (SmartNoticeFactory.gameModeState) {
                return
            }
            when(intent?.action) {
                AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                    onChangedTimes ++
                    if (onChangedTimes < 2) {
                        return
                    }
                    onChangedTimes = 2
                    when (intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, -1)) {
                        // 勿扰
                        0 -> {
                            doNotDisturb()
                        }
                        // 静音
                        1 -> {
                            silentMode()
                        }
                        // 默认
                        2 -> {
                            normal()
                        }
                    }
                }
            }
        }
    }

    private fun register() {
        try {
            val filter = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
            factory.context.registerReceiver(volumeChangedReceiver, filter)
        }catch (_: Exception) {

        }
    }

    private fun unregister() {
        try {
            factory.context.unregisterReceiver(volumeChangedReceiver)
        }catch (_: Exception) {
        }
    }

    override fun onEnableChanged(enabled: Boolean) {
        super.onEnableChanged(enabled)
        if (enabled) {
            register()
        } else {
            unregister()
        }
    }

    init {
        loadEnabled()
        register()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregister()
    }

    private val binding = SmartNoticeScreenUnlockedBinding.bind(
        View.inflate(
            factory.context,
            R.layout.smart_notice_screen_unlocked,
            null
        )
    )

    private val purple = Color(0xFF7F73F8).toArgb()
    private val red = Color(0xFFF94629).toArgb()
    private val white = Color.White.toArgb()

    /**
     * 勿扰动画
     */
    private fun doNotDisturb() {
        binding.label.setText(R.string.text_do_not_disturn)
        binding.icon.setImageResource(R.drawable.ic_do_not_disturb)
        binding.icon.setColorFilter(purple)
        binding.root.setOnClickListener(factory::minimize)

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

    /**
     * 静音动画
     */
    private fun silentMode() {
        binding.label.setText(R.string.text_silent_mode)
        binding.icon.setImageResource(R.drawable.ic_silent_mode)
        binding.icon.setColorFilter(red)
        binding.root.setOnClickListener(factory::minimize)

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

    /**
     * 默认模式
     */
    private fun normal() {
        binding.label.setText(R.string.text_volume_normal_mode)
        binding.icon.setImageResource(R.drawable.ic_volume_normal_mode)
        binding.icon.setColorFilter(white)
        binding.root.setOnClickListener(factory::minimize)

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
private fun VolumeChangedObserver(plugin: SmartNoticeNotificationPlugin) {
    var dialogState by rememberSaveable { mutableStateOf(false) }

    DetailItem(
        onClick = {
            dialogState = true
        },
        title = {
            Text(
                text = stringResource(R.string.text_smart_notice_observe_volume_changed)
            )
        },
        subTitle = {
            Text(
                text = stringResource(R.string.text_smart_notice_observe_volume_changed_tips)
            )
        }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }

    VolumeChangedObserverDialog(dialogState, plugin) { dialogState = false }
}

@Composable
private fun VolumeChangedObserverDialog(
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
                    text = stringResource(R.string.text_smart_notice_observe_volume_changed)
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