package io.github.lumkit.tweak.ui.screen.main

import android.os.Process
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.common.util.format
import io.github.lumkit.tweak.common.util.getVersionCode
import io.github.lumkit.tweak.common.util.startBrowser
import io.github.lumkit.tweak.ui.screen.settings.SettingsViewModel

@Composable
fun MainInit() {
    val context = LocalContext.current
    val settingsViewModel = viewModel { SettingsViewModel() }

    val mustUpdateDialogState by settingsViewModel.mustUpdateDialogState.collectAsStateWithLifecycle()
    val latestVersionState by settingsViewModel.latestMustBeVersion.collectAsStateWithLifecycle()

    LaunchedEffect(mustUpdateDialogState) {
        if (!mustUpdateDialogState) {
            settingsViewModel.checkMustUpdate(context.getVersionCode())
        }
    }

    if (mustUpdateDialogState) {
        AlertDialog(
            properties = DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = false
            ),
            onDismissRequest = {
                settingsViewModel.dismiss()
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
                                append(": ${latestVersionState?.versionName} (${latestVersionState?.versionCode})\n")
                                append(stringResource(R.string.text_release_date))
                                append(": ${latestVersionState?.createTime?.format()}\n")
                                append(stringResource(R.string.text_download_url))
                                append(": ")
                                withLink(
                                    link = LinkAnnotation.Clickable(
                                        tag = ""
                                    ) {
                                        context.startBrowser(latestVersionState?.downloadUrl)
                                    }
                                ) {
                                    withStyle(
                                        style = SpanStyle(
                                            color = MaterialTheme.colorScheme.primary,
                                            textDecoration = TextDecoration.Underline
                                        )
                                    ) {
                                        append(latestVersionState?.downloadUrl)
                                    }
                                }
                            },
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = latestVersionState?.description ?: stringResource(R.string.text_version_info_is_blank)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Process.killProcess(Process.myPid())
                        context.startBrowser(latestVersionState?.downloadUrl)
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
                        Process.killProcess(Process.myPid())
                    }
                ) {
                    Text(
                        text = "关闭应用"
                    )
                }
            }
        )
    }
}