package io.github.lumkit.tweak.ui.screen.vabup

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.common.shell.io.impl.RootFile
import io.github.lumkit.tweak.common.shell.module.UpdateEngineClient
import io.github.lumkit.tweak.common.shell.provide.ReusableShells
import io.github.lumkit.tweak.common.status.TweakException
import io.github.lumkit.tweak.common.util.RealFilePathUtil
import io.github.lumkit.tweak.common.util.deviceModeName
import io.github.lumkit.tweak.data.UpdateEngineStatus
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.ui.component.PlainTooltipBox
import io.github.lumkit.tweak.ui.component.ScreenScaffold
import io.github.lumkit.tweak.ui.component.SharedTransitionText
import io.github.lumkit.tweak.ui.local.LocalStorageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalSharedTransitionApi::class)
@SuppressLint("DefaultLocale")
@Composable
fun VabUpdaterScreen(
    viewModel: VabUpdateViewModel = viewModel { VabUpdateViewModel() },
) {

    val storageStore = LocalStorageStore.current
    val context = LocalContext.current

    var show by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            if (!storageStore.getBoolean(Const.APP_AUTO_START_SERVICE) && !show) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.text_please_open_app_auto_start, Toast.LENGTH_SHORT)
                        .show()
                }
                show = true
            }
        }
    }

    ScreenScaffold(
        title = {
            SharedTransitionText(
                text = stringResource(R.string.text_vab_updater),
            )
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(it)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoLayout()
            InputCard(
                context = context,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun InfoLayout() {
    var support by remember { mutableStateOf(false) }
    var slot by remember { mutableStateOf("N/A") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            support = UpdateEngineClient.support()
            slot = ReusableShells.execSync("getprop ro.boot.slot_suffix").replace("_", "").uppercase()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.primary
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_phone),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.text_device_info),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Text(
                text = buildAnnotatedString {
                    // 品牌
                    append(stringResource(R.string.text_device_brand))
                    append(": ${Build.BRAND}\n")

                    // 型号
                    append(stringResource(R.string.text_device_mode))
                    append(": ${deviceModeName()}\n")

                    // 系统版本
                    append(stringResource(R.string.text_android_version))
                    append(": Android ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})\n")

                    // OTA支持
                    append(stringResource(R.string.text_ota_support))
                    if (support) {
                        append(": ${stringResource(R.string.text_support)}\n")
                    } else {
                        withStyle(
                            style = SpanStyle(color = MaterialTheme.colorScheme.error)
                        ) {
                            append(": ${stringResource(R.string.text_not_support)}\n")
                        }
                    }

                    // 插槽
                    append(stringResource(R.string.text_alive_slot))
                    append(": $slot")

                    append("\n\n")
                    append(stringResource(R.string.text_ota_update_tips))
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.animateContentSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("DefaultLocale")
@Composable
private fun InputCard(
    context: Context, viewModel: VabUpdateViewModel
) {
    val updateEngineStatus by UpdateEngineClient.updateEngineStatus.collectAsStateWithLifecycle()
    var statusText by remember { mutableStateOf("") }
    val path by viewModel.romPathState.collectAsStateWithLifecycle()
    val taskEnabled by viewModel.installEnabled.collectAsStateWithLifecycle()
    val startButtonTextState by viewModel.startButtonTextState.collectAsStateWithLifecycle()
    var rowFileExists by rememberSaveable { mutableStateOf(false) }

    var support by remember { mutableStateOf(false) }

    BackHandler(!taskEnabled) {
        Toast.makeText(context, R.string.text_vab_updater_task_is_running, Toast.LENGTH_SHORT)
            .show()
    }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val filePath = RealFilePathUtil.getPath(context, it)
                    ?: throw TweakException("文件选择错误，请手动输入！")
                viewModel.setPath(
                    TextFieldValue(
                        text = filePath, selection = TextRange(filePath.length)
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        support = UpdateEngineClient.support()
    }

    LaunchedEffect(updateEngineStatus) {
        val status = updateEngineStatus.first
        statusText = when (status) {
            UpdateEngineStatus.UPDATE_STATUS_IDLE -> context.getString(R.string.text_status_idle)
            UpdateEngineStatus.UPDATE_STATUS_CHECKING_FOR_UPDATE -> context.getString(R.string.text_checking_for_update)
            UpdateEngineStatus.UPDATE_STATUS_UPDATE_AVAILABLE -> context.getString(R.string.text_update_available)
            UpdateEngineStatus.UPDATE_STATUS_DOWNLOADING -> context.getString(R.string.text_status_downloading)
            UpdateEngineStatus.UPDATE_STATUS_VERIFYING -> context.getString(R.string.text_status_verifying)
            UpdateEngineStatus.UPDATE_STATUS_FINALIZING -> context.getString(R.string.text_status_finishing)
            UpdateEngineStatus.UPDATE_STATUS_UPDATED_NEED_REBOOT -> context.getString(R.string.text_status_need_reboot)
            UpdateEngineStatus.UPDATE_STATUS_REPORTING_ERROR_EVENT -> context.getString(R.string.text_status_error_event)
            UpdateEngineStatus.UPDATE_STATUS_ATTEMPTING_ROLLBACK -> context.getString(R.string.text_status_attempting_rollback)
            UpdateEngineStatus.UPDATE_STATUS_DISABLED -> context.getString(R.string.text_status_disabled)
            UpdateEngineStatus.ERROR -> context.getString(R.string.text_unknown_error)
        }

        if (
            status == UpdateEngineStatus.UPDATE_STATUS_IDLE
            || status == UpdateEngineStatus.UPDATE_STATUS_UPDATED_NEED_REBOOT
            || status == UpdateEngineStatus.UPDATE_STATUS_REPORTING_ERROR_EVENT
            || status == UpdateEngineStatus.ERROR
        ) {
            viewModel.setStartButtonTextState(context.getString(R.string.text_start_vab_update))
        }
    }

    LaunchedEffect(path) {
        rowFileExists = RootFile(path.text).exists()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = if (updateEngineStatus.first == UpdateEngineStatus.ERROR || updateEngineStatus.first == UpdateEngineStatus.UPDATE_STATUS_REPORTING_ERROR_EVENT) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )

            Text(
                text = String.format("%.2f%s", updateEngineStatus.second * 100f, "%"),
                style = MaterialTheme.typography.labelSmall,
                color = if (updateEngineStatus.first == UpdateEngineStatus.ERROR || updateEngineStatus.first == UpdateEngineStatus.UPDATE_STATUS_REPORTING_ERROR_EVENT) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
        }
        LinearProgressIndicator(
            progress = { updateEngineStatus.second },
            modifier = Modifier.fillMaxWidth(),
            drawStopIndicator = {},
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = path,
            onValueChange = viewModel::setPath,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            singleLine = true,
            label = {
                Text(text = stringResource(R.string.text_rom_path))
            },
            readOnly = !taskEnabled,
        )
        TextButton(
            onClick = {
                pickFileLauncher.launch("application/zip")
            }) {
            Text(
                text = stringResource(R.string.text_choose_file)
            )
        }
    }

    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            viewModel.install()
        },
        enabled = taskEnabled && updateEngineStatus.first == UpdateEngineStatus.UPDATE_STATUS_IDLE && rowFileExists && support
    ) {
        Text(text = startButtonTextState)
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        maxItemsInEachRow = 2,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.weight(1f)
        ) {
            PlainTooltipBox(
                tooltip = {
                    Text(
                        text = stringResource(R.string.text_ota_merge_tips)
                    )
                }) {
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        viewModel.merge()
                        Toast.makeText(context, R.string.text_success_block, Toast.LENGTH_SHORT)
                            .show()
                    },
                    enabled = updateEngineStatus.first == UpdateEngineStatus.UPDATE_STATUS_IDLE && taskEnabled
                ) {
                    Text(
                        text = stringResource(R.string.text_ota_merge)
                    )
                }
            }
        }

        Box(
            modifier = Modifier.weight(1f)
        ) {
            PlainTooltipBox(
                tooltip = {
                    Text(
                        text = stringResource(R.string.text_ota_cancel_tips)
                    )
                }) {
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        viewModel.cancel()
                        Toast.makeText(context, R.string.text_success_block, Toast.LENGTH_SHORT)
                            .show()
                    },
                    enabled = updateEngineStatus.first == UpdateEngineStatus.UPDATE_STATUS_DOWNLOADING && taskEnabled
                ) {
                    Text(
                        text = stringResource(R.string.text_ota_cancel)
                    )
                }
            }
        }

        Box(
            modifier = Modifier.weight(1f)
        ) {
            PlainTooltipBox(
                tooltip = {
                    Text(
                        text = stringResource(R.string.text_ota_reset_status_tips)
                    )
                }) {
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        viewModel.resetStatus()
                        Toast.makeText(context, R.string.text_success_block, Toast.LENGTH_SHORT)
                            .show()
                    },
                    enabled = updateEngineStatus.first != UpdateEngineStatus.UPDATE_STATUS_DOWNLOADING && taskEnabled
                ) {
                    Text(
                        text = stringResource(R.string.text_ota_reset_status)
                    )
                }
            }
        }
    }
}