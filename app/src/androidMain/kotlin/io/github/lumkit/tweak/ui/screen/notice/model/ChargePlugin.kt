package io.github.lumkit.tweak.ui.screen.notice.model

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.data.SmartNoticeRunningState
import io.github.lumkit.tweak.databinding.SmartNoticeChargeBinding
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.ui.component.DetailItem
import io.github.lumkit.tweak.ui.component.FolderItem
import io.github.lumkit.tweak.ui.view.SmartNoticeFactory
import kotlin.math.min

class ChargePlugin(
    override val factory: SmartNoticeFactory
) : SmartNoticeNotificationPlugin(
    factory,
    Const.SmartNotice.Observe.SMART_NOTICE_OBSERVE_CHARGE
) {
    private val chargingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (SmartNoticeFactory.gameModeState) {
                return
            }
            if (SmartNoticeFactory.runningState == SmartNoticeRunningState.ONLINE) {
                when (intent.action) {
                    Intent.ACTION_POWER_CONNECTED -> {
                        powerConnected()
                    }

                    Intent.ACTION_POWER_DISCONNECTED -> {
                        powerDisconnected()
                    }
                }
            }
        }
    }

    private var batteryPercentageState by mutableFloatStateOf(0f)
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // 获取电池电量
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

            // 计算电池百分比
            batteryPercentageState = (level.toFloat() / scale.toFloat()) * 100


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

    private fun register() {
        try {
            val filter = IntentFilter(Intent.ACTION_POWER_CONNECTED)
            filter.addAction(Intent.ACTION_POWER_DISCONNECTED)
            factory.context.registerReceiver(chargingReceiver, filter)
        } catch (_: Exception) {
        }
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            factory.context.registerReceiver(batteryReceiver, filter)
        } catch (_: Exception) {
        }
    }

    private fun unregister() {
        try {
            factory.context.unregisterReceiver(chargingReceiver)
        } catch (_: Exception) {
        }
        try {
            factory.context.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {
        }
    }

    private var binding: SmartNoticeChargeBinding = SmartNoticeChargeBinding.bind(
        View.inflate(
            factory.context,
            R.layout.smart_notice_charge,
            null
        )
    )

    init {
        loadEnabled()
        register()
    }

    @Composable
    override fun PreferenceContent(plugin: SmartNoticeNotificationPlugin) {
        BatteryObserver(plugin)
    }

    @SuppressLint("DefaultLocale")
    private fun powerConnected() {
        binding.label.setText(R.string.text_power_connect)
        binding.level.text = String.format("%d%s", batteryPercentageState.toInt(), "%")
        binding.level.setTextColor("#FF09FE75".toColorInt())
        binding.icon.setImageResource(R.drawable.ic_power_connect)
        binding.icon.setColorFilter("#FF09FE75".toColorInt())
        binding.root.setOnClickListener(factory::minimize)
        factory.toast(
            binding.root,
            contentSize = { _, density, size, _ ->
                with(density) {
                    DpSize(
                        min(size.width, size.height).toDp() - 28.dp * 2f,
                        32.dp
                    )
                }
            },
        )
    }

    @SuppressLint("DefaultLocale")
    private fun powerDisconnected() {
        binding.label.setText(R.string.text_power_disconnect)
        binding.level.text = String.format("%d%s", batteryPercentageState.toInt(), "%")
        binding.level.setTextColor("#FFFFFFFF".toColorInt())
        binding.icon.setImageResource(R.drawable.ic_power_disconnect)
        binding.icon.setColorFilter("#FFFFFFFF".toColorInt())
        binding.root.setOnClickListener(factory::minimize)
        factory.toast(
            binding.root,
            contentSize = { _, density, size, _ ->
                with(density) {
                    DpSize(
                        min(size.width, size.height).toDp() - 28.dp * 2f,
                        32.dp
                    )
                }
            },
        )
    }

    @SuppressLint("DefaultLocale")
    private fun lowPower() {
        binding.label.setText(R.string.text_low_power)
        binding.level.text = String.format("%d%s", batteryPercentageState.toInt(), "%")
        binding.level.setTextColor(Color(0xFFF94629).toArgb())
        binding.icon.setImageResource(R.drawable.ic_power_disconnect)
        binding.icon.setColorFilter(Color(0xFFF94629).toArgb())
        binding.root.setOnClickListener(factory::minimize)
        factory.toast(
            binding.root,
            contentSize = { _, density, size, _ ->
                with(density) {
                    DpSize(
                        size.width.toDp() - 28.dp * 2f,
                        32.dp
                    )
                }
            },
        )
    }

    override fun display(state: Any) {
        if (SmartNoticeFactory.gameModeState) {
            return
        }
        if (state as Boolean) {
            powerConnected()
        } else {
            powerDisconnected()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregister()
    }
}

@Composable
private fun BatteryObserver(plugin: SmartNoticeNotificationPlugin) {
    var dialogState by rememberSaveable { mutableStateOf(false) }

    DetailItem(
        onClick = {
            dialogState = true
        },
        title = {
            Text(
                text = stringResource(R.string.text_smart_notice_observe_charge)
            )
        },
        subTitle = {
            Text(
                text = stringResource(R.string.text_smart_notice_observe_charge_tips)
            )
        }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }

    BatteryObserverDialog(dialogState, plugin) { dialogState = false }
}

@Composable
private fun BatteryObserverDialog(
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
                    text = stringResource(R.string.text_smart_notice_observe_charge)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    SwitchRow(plugin)
                    TestRows()
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

@Composable
private fun TestRows() {
    val context = LocalContext.current
    FolderItem(
        padding = PaddingValues(vertical = 16.dp),
        onClick = {
            SmartNoticeFactory.imitateCharge(context, true)
        },
        title = {
            Text(
                text = stringResource(R.string.text_imitate_power_connected)
            )
        },
        subtitle = {
            Text(
                text = stringResource(R.string.text_show_imitate_animation)
            )
        }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }

    FolderItem(
        padding = PaddingValues(vertical = 16.dp),
        onClick = {
            SmartNoticeFactory.imitateCharge(context, false)
        },
        title = {
            Text(
                text = stringResource(R.string.text_imitate_power_disconnected)
            )
        },
        subtitle = {
            Text(
                text = stringResource(R.string.text_show_imitate_animation)
            )
        }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }
}