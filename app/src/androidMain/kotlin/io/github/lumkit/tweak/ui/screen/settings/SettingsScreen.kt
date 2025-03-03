package io.github.lumkit.tweak.ui.screen.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import io.github.lumkit.tweak.LocalAnimateContentScope
import io.github.lumkit.tweak.LocalScreenNavigationController
import io.github.lumkit.tweak.LocalSharedTransitionScope
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.common.shell.provide.ReusableShells
import io.github.lumkit.tweak.common.util.format
import io.github.lumkit.tweak.common.util.getVersionCode
import io.github.lumkit.tweak.common.util.getVersionName
import io.github.lumkit.tweak.common.util.restartApp
import io.github.lumkit.tweak.common.util.startBrowser
import io.github.lumkit.tweak.data.DarkModeState
import io.github.lumkit.tweak.data.RuntimeStatus
import io.github.lumkit.tweak.data.asStringId
import io.github.lumkit.tweak.data.watch
import io.github.lumkit.tweak.model.Config
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.ui.component.DetailItem
import io.github.lumkit.tweak.ui.component.FolderItem
import io.github.lumkit.tweak.ui.component.GroupDetail
import io.github.lumkit.tweak.ui.component.LoadingDialog
import io.github.lumkit.tweak.ui.component.MaterialSlider
import io.github.lumkit.tweak.ui.component.PlainTooltipBox
import io.github.lumkit.tweak.ui.component.ScreenScaffold
import io.github.lumkit.tweak.ui.component.SharedTransitionText
import io.github.lumkit.tweak.ui.local.CustomColorScheme
import io.github.lumkit.tweak.ui.local.LocalStorageStore
import io.github.lumkit.tweak.ui.local.LocalThemeStore
import io.github.lumkit.tweak.ui.local.Material3
import io.github.lumkit.tweak.ui.local.Scheme
import io.github.lumkit.tweak.ui.local.Schemes
import io.github.lumkit.tweak.ui.local.StorageStore
import io.github.lumkit.tweak.ui.local.ThemeStore
import io.github.lumkit.tweak.ui.local.json
import io.github.lumkit.tweak.ui.local.toColor
import io.github.lumkit.tweak.ui.local.toHex
import io.github.lumkit.tweak.ui.screen.ScreenRoute
import io.mhssn.colorpicker.ColorPicker
import io.mhssn.colorpicker.ColorPickerType
import io.mhssn.colorpicker.ext.transparentBackground
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel { SettingsViewModel() }
) {
    val context = LocalContext.current
    val storageStore = LocalStorageStore.current
    val navHostController = LocalScreenNavigationController.current

    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedContentScope = LocalAnimateContentScope.current

    val ioScope = rememberCoroutineScope { Dispatchers.IO }

    ScreenScaffold(
        title = {
            SharedTransitionText(
                text = stringResource(R.string.text_settings_screen),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.size(8.dp))
            SchemeModules(context)
            AppModules(context, storageStore, ioScope)
            About(
                context,
                navHostController,
                viewModel,
                sharedTransitionScope,
                animatedContentScope
            )
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SchemeModules(context: Context) {
    val themeStore = LocalThemeStore.current

    GroupDetail(
        title = {
            Text(
                text = stringResource(R.string.text_settings_scheme_color)
            )
        }
    ) {
        var darkModeExpanded by remember { mutableStateOf(false) }

        DetailItem(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                darkModeExpanded = true
            },
            title = {
                Text(text = stringResource(R.string.text_dark_theme_mode))
            },
            subTitle = {
                Text(
                    text = String.format(
                        "%s: %s",
                        stringResource(R.string.text_now),
                        stringResource(themeStore.darkModeState.asStringId())
                    )
                )
            }
        ) {
            Column {
                DropdownMenu(
                    expanded = darkModeExpanded,
                    onDismissRequest = { darkModeExpanded = false },
                ) {
                    DarkModeState.entries.forEach {
                        DropdownMenuItem(
                            text = {
                                Text(text = stringResource(it.asStringId()))
                            },
                            onClick = {
                                themeStore.darkModeState = it
                                darkModeExpanded = false
                            }
                        )
                    }
                }
            }
        }

        DetailItem(
            modifier = Modifier.fillMaxWidth(),
            enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            onClick = {
                themeStore.isDynamicColor = !themeStore.isDynamicColor
            },
            title = {
                Text(text = stringResource(R.string.text_enable_dynamic_color))
            },
            subTitle = {
                Text(text = stringResource(R.string.text_enable_dynamic_color_tips))
            }
        ) {
            Switch(
                checked = themeStore.isDynamicColor,
                onCheckedChange = null
            )
        }

        CustomColorSchemeItem(themeStore, context)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.CustomColorSchemeItem(themeStore: ThemeStore, context: Context) {

    val mainScope = rememberCoroutineScope { Dispatchers.Main }
    val ioScope = rememberCoroutineScope { Dispatchers.IO }
    var loading by remember { mutableStateOf(false) }
    val updateColorSchemeDialogState = rememberSaveable { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        ioScope.launch(
            CoroutineExceptionHandler { _, throwable ->
                mainScope.launch {
                    Toast.makeText(context, throwable.message, Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            uri?.let {
                val localJson =
                    context.contentResolver.openInputStream(it)?.use { inputStream ->
                        inputStream.buffered().use { bis ->
                            String(bis.readBytes())
                        }
                    }

                if (localJson == null) throw RuntimeException("文件读取失败！")
                val m3 = try {
                    json.decodeFromString<Material3>(localJson)
                } catch (e: Exception) {
                    throw RuntimeException("主题安装失败！这似乎不是主题文件呢")
                }
                themeStore.customColorScheme.lightColorScheme = m3.schemes.light.toColorScheme()
                themeStore.customColorScheme.darkColorScheme = m3.schemes.dark.toColorScheme()
                themeStore.customColorScheme.material3 = m3

                // 安装到私有目录
                CustomColorScheme.CUSTOM_MATERIAL3_JSON_INSTALL_File.outputStream().use { os ->
                    os.buffered().use { bos ->
                        bos.write(localJson.toByteArray())
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        R.string.text_success_install_color_scheme,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            loading = false
        }
    }

    DetailItem(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            themeStore.customColorScheme.isCustomColorScheme =
                !themeStore.customColorScheme.isCustomColorScheme
        },
        title = {
            Text(text = stringResource(R.string.text_custom_color_scheme))
        },
        subTitle = {
            Text(text = stringResource(R.string.text_custom_color_scheme_tips))
        }
    ) {
        Switch(
            checked = themeStore.customColorScheme.isCustomColorScheme,
            onCheckedChange = null
        )
    }

    AnimatedVisibility(
        visible = themeStore.customColorScheme.isCustomColorScheme,
        label = "CustomColorSchemeItem"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            DetailItem(
                modifier = Modifier.fillMaxWidth(),
                title = {
                    Text(text = stringResource(R.string.text_custom_color_scheme_install))
                },
                subTitle = {
                    PlainTooltipBox(
                        tooltip = {
                            Text(text = themeStore.customColorScheme.material3?.description ?: "")
                        }
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append("${stringResource(R.string.text_now_color)}: ")
                                withStyle(
                                    style = SpanStyle(
                                        color = themeStore.customColorScheme.material3?.seed?.toColor()
                                            ?: MaterialTheme.colorScheme.primary,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append(themeStore.customColorScheme.material3?.seed)
                                }
                            },
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            ) {
                Button(
                    onClick = {
                        loading = true
                        filePickerLauncher.launch("application/json")
                    }
                ) {
                    Text(
                        text = stringResource(R.string.text_choose_file)
                    )
                }
            }

            DetailItem(
                modifier = Modifier.fillMaxWidth(),
                title = {
                    Text(text = stringResource(R.string.text_custom_color_scheme_update))
                },
                subTitle = {
                    Text(text = stringResource(R.string.text_custom_color_scheme_update_tips))
                },
                onClick = {
                    updateColorSchemeDialogState.value = true
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_right),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    if (loading) {
        LoadingDialog()
    }

    UpdateColorSchemeDialog(updateColorSchemeDialogState, themeStore)
}

@Composable
private fun UpdateColorSchemeDialog(
    updateColorSchemeDialogState: MutableState<Boolean>,
    themeStore: ThemeStore
) {
    if (updateColorSchemeDialogState.value) {
        val context = LocalContext.current
        var material3 by remember { mutableStateOf(themeStore.customColorScheme.material3) }
        val ioScope = rememberCoroutineScope { Dispatchers.IO }
        var loading by rememberSaveable { mutableStateOf(false) }

        AlertDialog(
            modifier = Modifier.padding(vertical = 28.dp),
            onDismissRequest = { updateColorSchemeDialogState.value = false },
            title = {
                Text(text = stringResource(R.string.text_custom_color_scheme_update))
            },
            text = {
                UpdateColorSchemeBox(material3) {
                    material3 = it
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        loading = true
                        ioScope.launch {
                            try {
                                val copy = material3?.copy()
                                themeStore.customColorScheme.material3 = copy
                                copy?.apply {
                                    // 安装到私有目录
                                    CustomColorScheme.CUSTOM_MATERIAL3_JSON_INSTALL_File.outputStream()
                                        .use { os ->
                                            os.buffered().use { bos ->
                                                bos.write(json.encodeToString(this).toByteArray())
                                            }
                                        }
                                    themeStore.customColorScheme.lightColorScheme =
                                        schemes.light.toColorScheme()
                                    themeStore.customColorScheme.darkColorScheme =
                                        schemes.dark.toColorScheme()
                                }
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        R.string.text_success_update_color_scheme,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                updateColorSchemeDialogState.value = false
                            } catch (e: Exception) {
                                withContext(Dispatchers.IO) {
                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                }
                            } finally {
                                loading = false
                            }
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.text_save))
                }
            },
            dismissButton = {
                FilledTonalButton(
                    onClick = {
                        updateColorSchemeDialogState.value = false
                    }
                ) {
                    Text(text = stringResource(R.string.text_cancel))
                }
            }
        )
    }
}

@Composable
private fun UpdateColorSchemeBox(material3: Material3?, onUpdate: (Material3) -> Unit) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        material3?.let {
            Text(
                text = stringResource(R.string.text_light_scheme),
                style = MaterialTheme.typography.labelSmall
            )
            SchemesBox(
                name = "light",
                it,
                onUpdate = onUpdate
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = stringResource(R.string.text_dark_scheme),
                style = MaterialTheme.typography.labelSmall
            )
            SchemesBox(
                name = "dark",
                it,
                onUpdate = onUpdate
            )
        }
    }
}

@Composable
private fun ColumnScope.SchemesBox(
    name: String,
    material3: Material3,
    onUpdate: (Material3) -> Unit,
) {
    var schemeBean by remember { mutableStateOf<Scheme?>(null) }
    var kProperties by remember { mutableStateOf<List<KProperty1<out Scheme, *>>>(emptyList()) }

    LaunchedEffect(material3) {
        val schemes = material3::class
            .memberProperties
            .firstOrNull { it.name == "schemes" }
            ?.getter
            ?.call(material3)
            ?.let { it as? Schemes }

        val scheme = schemes?.let { bean ->
            bean::class.memberProperties
                .firstOrNull { it.name == name }
                ?.getter
                ?.call(bean)
                ?.let { it as Scheme }
        }

        schemeBean = scheme

        kProperties = scheme?.let { bean ->
            bean::class.memberProperties.toList().sortedBy { it.name }
        } ?: emptyList()
    }

    AnimatedVisibility(
        visible = kProperties.isEmpty()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
        }
    }

    AnimatedVisibility(
        visible = kProperties.isNotEmpty()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {

            kProperties.forEach {
                var colorPickerDialogState by rememberSaveable { mutableStateOf(false) }
                FolderItem(
                    onClick = {
                        colorPickerDialogState = true
                    },
                    title = {
                        Text(
                            text = it.name,
                        )
                    },
                    icon = {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = it.getter.call(schemeBean)?.let { it as String }?.toColor()
                                ?: Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        ) {}
                    },
                    trailingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_right),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                if (colorPickerDialogState) {
                    ColorPickerDialog(
                        onDismissRequest = { colorPickerDialogState = false },
                        it,
                        schemeBean,
                    ) { bean ->
                        schemeBean = bean
                        onUpdate(
                            if (name == "light") {
                                material3.copy(schemes = material3.schemes.copy(light = bean))
                            } else {
                                material3.copy(schemes = material3.schemes.copy(dark = bean))
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ColorPickerDialog(
    onDismissRequest: () -> Unit,
    kProperty1: KProperty1<out Scheme, *>,
    schemeBean: Scheme?,
    onUpdate: (Scheme) -> Unit
) {
    val context = LocalContext.current
    var textColor by remember(key1 = kProperty1) { mutableStateOf(Color.Transparent.toHex()) }
    var loading by rememberSaveable { mutableStateOf(false) }
    val ioScope = rememberCoroutineScope { Dispatchers.IO }

    LaunchedEffect(kProperty1) {
        textColor =
            kProperty1.getter.call(schemeBean)?.let { it as String } ?: Color.Transparent.toHex()
    }

    AlertDialog(
        modifier = Modifier.padding(vertical = 28.dp),
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = kProperty1.name)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                ColorPicker(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    type = ColorPickerType.Ring()
                ) {
                    textColor = it.toHex()
                }

                var text by remember { mutableStateOf(textColor) }

                LaunchedEffect(textColor) {
                    text = textColor
                }

                Spacer(modifier = Modifier.size(16.dp))

                Row {
                    TextField(
                        value = text,
                        onValueChange = {
                            text = it
                        },
                        label = {
                            Surface(
                                modifier = Modifier
                                    .size(80.dp, 8.dp)
                                    .clip(CircleShape)
                                    .transparentBackground(verticalBoxesAmount = 36)
                                    .background(textColor.toColor()),
                                color = Color.Transparent
                            ) {}
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        singleLine = true
                    )
                    AnimatedVisibility(
                        visible = text != textColor
                    ) {
                        TextButton(
                            modifier = Modifier.padding(start = 16.dp),
                            onClick = {
                                try {
                                    textColor = text.toColor().toHex()
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        R.string.text_color_converte_failed,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        ) {
                            Text(text = stringResource(R.string.text_update))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    loading = true
                    ioScope.launch {
                        try {
                            kProperty1.isAccessible = true
                            val javaField =
                                kProperty1.javaField ?: throw RuntimeException("属性异常")
                            javaField.set(schemeBean, textColor)
                            onUpdate(schemeBean?.copy() ?: throw RuntimeException("属性异常"))
                            onDismissRequest()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                            }
                        } finally {
                            loading = false
                        }
                    }
                }
            ) {
                Text(text = stringResource(R.string.text_confirm))
            }
        },
        dismissButton = {
            FilledTonalButton(
                onClick = onDismissRequest
            ) {
                Text(text = stringResource(R.string.text_cancel))
            }
        }
    )

    if (loading) {
        LoadingDialog()
    }
}

@SuppressLint("DefaultLocale", "BatteryLife")
@Composable
private fun AppModules(context: Context, storageStore: StorageStore, ioScope: CoroutineScope) {

    var userExpanded by rememberSaveable { mutableStateOf(false) }
    var user by remember { mutableStateOf(ReusableShells.getDefaultInstance.user) }
    var refreshTickDialogState by rememberSaveable { mutableStateOf(false) }
    val tick = rememberSaveable { mutableLongStateOf(Config.REFRESH_TICK) }
    var autoStart by remember { mutableStateOf(storageStore.getBoolean(Const.APP_AUTO_START_SERVICE)) }

    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }

    GroupDetail(
        modifier = Modifier.fillMaxWidth(),
        title = {
            Text(
                text = stringResource(R.string.text_settings_app)
            )
        }
    ) {
        DetailItem(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val hasIgnored = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                if (!hasIgnored) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.setData("package:${context.packageName}".toUri())
                    context.startActivity(intent)
                } else {
                    autoStart = !autoStart
                    storageStore.putBoolean(Const.APP_AUTO_START_SERVICE, autoStart)
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.text_auto_start)
                )
            },
            subTitle = {
                Text(
                    text = stringResource(R.string.text_auto_start_tips)
                )
            },
            actions = {
                Switch(
                    checked = autoStart,
                    onCheckedChange = null
                )
            }
        )

        run {
            var askChangeDialog by rememberSaveable { mutableStateOf(false) }
            var expanded by rememberSaveable { mutableStateOf(false) }
            var tempStatus by rememberSaveable { mutableStateOf(TweakApplication.runtimeStatus) }

            if (askChangeDialog) {
                AlertDialog(
                    onDismissRequest = {
                        askChangeDialog = false
                    },
                    title = {
                        Text(text = stringResource(R.string.text_alert))
                    },
                    text = {
                        Text(
                            text = buildString {
                                append(
                                    String.format(
                                        stringResource(R.string.text_change_runtime_status_message),
                                        tempStatus.name
                                    )
                                )
                            }
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                TweakApplication.setRuntimeStatusState(tempStatus)
                                context.restartApp()
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.text_change_runtime_status_and_restart)
                            )
                        }
                    },
                    dismissButton = {
                        FilledTonalButton(
                            onClick = {
                                askChangeDialog = false
                            }
                        ) {
                            Text(text = stringResource(R.string.text_cancel))
                        }
                    }
                )
            }

            DetailItem(
                modifier = Modifier.fillMaxWidth(),
                title = {
                    Text(
                        text = stringResource(R.string.text_change_runtime_status)
                    )
                },
                subTitle = {
                    Text(
                        text = buildString {
                            append(stringResource(R.string.text_now_status))
                            append(": ")
                            append(TweakApplication.runtimeStatus.name)
                        }
                    )
                },
                actions = {
                    Column {
                        TextButton(
                            onClick = {
                                expanded = true
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.text_change)
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = {
                                expanded = false
                            }
                        ) {
                            RuntimeStatus.entries.filter {
                                it != RuntimeStatus.Normal && it != TweakApplication.runtimeStatus
                            }.forEach {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = it.name
                                        )
                                    },
                                    onClick = {
                                        askChangeDialog = true
                                        tempStatus = it
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }

        if (TweakApplication.runtimeStatus == RuntimeStatus.Root) {
            DetailItem(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    userExpanded = true
                },
                title = {
                    Text(
                        text = stringResource(R.string.text_change_root_user)
                    )
                },
                subTitle = {
                    Text(
                        text = String.format(
                            "%s: %s",
                            stringResource(R.string.text_change_root_user_tip),
                            user
                        )
                    )
                },
                actions = {
                    Column {
                        Icon(
                            painter = painterResource(R.drawable.ic_right),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        DropdownMenu(
                            expanded = userExpanded,
                            onDismissRequest = { userExpanded = false }
                        ) {
                            var enabled by rememberSaveable { mutableStateOf(true) }
                            Config.ROOT_USERS.forEach {
                                DropdownMenuItem(
                                    text = {
                                        Text(text = it)
                                    },
                                    enabled = enabled,
                                    onClick = {
                                        enabled = false
                                        ioScope.launch {
                                            user = it
                                            ReusableShells.changeUserIdAtAll(it)
                                            userExpanded = false
                                            enabled = true
                                        }
                                    },
                                    leadingIcon = {
                                        Checkbox(
                                            checked = it == user,
                                            onCheckedChange = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }

        DetailItem(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                refreshTickDialogState = true
            },
            title = {
                Text(text = stringResource(R.string.text_refresh_tick))
            },
            subTitle = {
                Text(
                    text = String.format(
                        "%s: %dms",
                        stringResource(R.string.text_now),
                        tick.longValue
                    )
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

    if (refreshTickDialogState) {
        RefreshTickDialog(
            tick,
            onDismissRequest = { refreshTickDialogState = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefreshTickDialog(
    tick: MutableState<Long>,
    onDismissRequest: () -> Unit,
) {
    val storageStore = LocalStorageStore.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(R.string.text_refresh_tick_set))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${tick.value}ms",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                MaterialSlider(
                    modifier = Modifier.fillMaxWidth(),
                    value = tick.value.toFloat(),
                    onValueChange = {
                        tick.value = it.toLong()
                    },
                    valueRange = 500f..5000f
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    storageStore.putInt(Const.APP_OVERVIEW_TICK, tick.value.toInt())
                    onDismissRequest()
                }
            ) {
                Text(text = stringResource(R.string.text_save))
            }
        },
        dismissButton = {
            FilledTonalButton(
                onClick = onDismissRequest
            ) {
                Text(text = stringResource(R.string.text_cancel))
            }
        }
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun About(
    context: Context,
    navHostController: NavHostController,
    viewModel: SettingsViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    val versionName = remember { context.getVersionName() }
    val versionCode = remember { context.getVersionCode() }

    val loadState = viewModel.loadState.collectAsStateWithLifecycle()
    var checkVersionLoading by rememberSaveable { mutableStateOf(false) }
    val checkVersionDialogState = rememberSaveable { mutableStateOf(false) }
    var hasUpdate by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(loadState) {
        viewModel.checkVersion(context.getVersionCode())
        loadState.watch("checkVersion") {
            when (it) {
                is io.github.lumkit.tweak.data.LoadState.Fail -> {
                    viewModel.clearLoadState("checkVersion")
                    withContext(Dispatchers.Main) {
                        if (checkVersionLoading) {
                            Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    hasUpdate = false
                    checkVersionDialogState.value = false
                    checkVersionLoading = false
                }

                is io.github.lumkit.tweak.data.LoadState.Loading -> Unit
                is io.github.lumkit.tweak.data.LoadState.Success -> {
                    viewModel.clearLoadState("checkVersion")
                    hasUpdate = true
                    withContext(Dispatchers.Main) {
                        if (checkVersionLoading) {
                            Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    if (checkVersionLoading) {
                        checkVersionDialogState.value = true
                    }
                    checkVersionLoading = false
                }

                null -> Unit
            }
        }
    }

    if (checkVersionLoading) {
        LoadingDialog()
    }

    UpdateDialog(checkVersionDialogState, viewModel, context)

    GroupDetail(
        title = {
            Text(text = stringResource(R.string.text_about))
        }
    ) {
        DetailItem(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                checkVersionLoading = true
                checkVersionDialogState.value = false
                viewModel.checkVersion(context.getVersionCode())
            },
            title = {
                Text(
                    text = stringResource(R.string.text_app_version)
                )
            },
            subTitle = {
                Text(
                    text = buildString {
                        append(versionName)
                        append(" ($versionCode)")
                    }
                )
            },
            actions = {
                AnimatedVisibility(
                    visible = hasUpdate
                ) {
                    Badge {
                        Text(text = stringResource(R.string.text_has_update))
                    }
                }
                Icon(
                    painter = painterResource(R.drawable.ic_right),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )

        DetailItem(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                context.startBrowser("https://tweak.lumtoolkit.com/")
            },
            title = {
                Text(
                    text = stringResource(R.string.text_app_website)
                )
            },
            subTitle = {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                textDecoration = TextDecoration.Underline,
                                color = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            append("https://tweak.lumtoolkit.com/")
                        }
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

        DetailItem(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                context.startBrowser("mqqapi://card/show_pslcard?src_type=internal&version=1&uin=2205903933&card_type=person&source=qrcode")
            },
            title = {
                Text(
                    text = stringResource(R.string.text_app_developer)
                )
            },
            subTitle = {
                Text(
                    text = buildAnnotatedString {
                        withLink(
                            link = LinkAnnotation.Clickable(
                                tag = "",
                                linkInteractionListener = {
                                    context.startBrowser("https://github.com/lumkit")
                                }
                            )
                        ) {
                            withStyle(
                                style = SpanStyle(
                                    textDecoration = TextDecoration.Underline,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                append(stringResource(R.string.text_app_developer_tips))
                            }
                        }
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

        DetailItem(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                context.startBrowser("mqqapi://card/show_pslcard?src_type=internal&version=1&uin=768798964&card_type=group&source=qrcode")
            },
            title = {
                Text(
                    text = stringResource(R.string.text_qq_group)
                )
            },
            subTitle = {
                Text(
                    text = stringResource(R.string.text_qq_group_tips)
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

        DetailItem(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                context.startBrowser("https://github.com/lumkit/Tweak-Android")
            },
            title = {
                Text(
                    text = stringResource(R.string.text_open_sources_url)
                )
            },
            subTitle = {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("https://github.com/lumkit/Tweak-Android")
                        }
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

        sharedTransitionScope.run {
            DetailItem(
                modifier = Modifier.fillMaxWidth().sharedBounds(
                    sharedContentState = rememberSharedContentState("${ScreenRoute.OPEN_SOURCE}-box"),
                    animatedVisibilityScope = animatedContentScope,
                ),
                onClick = {
                    navHostController.navigate(ScreenRoute.OPEN_SOURCE)
                },
                title = {

                    Text(
                        text = stringResource(R.string.text_open_sources),
                        modifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState("${ScreenRoute.OPEN_SOURCE}-title"),
                            animatedVisibilityScope = animatedContentScope
                        )
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

@Composable
private fun UpdateDialog(
    hasUpdate: MutableState<Boolean>,
    viewModel: SettingsViewModel,
    context: Context
) {
    val clientVersionBeanState by viewModel.clientVersionState.collectAsStateWithLifecycle()

    if (hasUpdate.value) {
        AlertDialog(
            modifier = Modifier.padding(vertical = 28.dp),
            onDismissRequest = {
                hasUpdate.value = false
            },
            title = {
                Text(
                    text = stringResource(R.string.text_has_update)
                )
            },
            text = {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append(stringResource(R.string.text_app_version))
                                append(": ${clientVersionBeanState?.versionName} (${clientVersionBeanState?.versionCode})\n")
                                append(stringResource(R.string.text_release_date))
                                append(": ${clientVersionBeanState?.createTime?.format()}\n")
                                append(stringResource(R.string.text_download_url))
                                append(": ")
                                withLink(
                                    link = LinkAnnotation.Clickable(
                                        tag = ""
                                    ) {
                                        context.startBrowser(clientVersionBeanState?.downloadUrl)
                                    }
                                ) {
                                    withStyle(
                                        style = SpanStyle(
                                            color = MaterialTheme.colorScheme.primary,
                                            textDecoration = TextDecoration.Underline
                                        )
                                    ) {
                                        append(clientVersionBeanState?.downloadUrl)
                                    }
                                }
                            },
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = clientVersionBeanState?.description
                                ?: stringResource(R.string.text_version_info_is_blank)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        hasUpdate.value = false
                        context.startBrowser(clientVersionBeanState?.downloadUrl)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.text_download_new_version)
                    )
                }
            },
            dismissButton = {
                FilledTonalButton(
                    onClick = {
                        hasUpdate.value = false
                    }
                ) {
                    Text(
                        text = "取消"
                    )
                }
            }
        )
    }
}