package io.github.lumkit.tweak.ui.screen.notice

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.model.AppInfo
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.services.SmartNoticeService.Companion.updateMediaFilter
import io.github.lumkit.tweak.ui.component.FolderItem
import io.github.lumkit.tweak.ui.local.LocalStorageStore
import io.github.lumkit.tweak.ui.local.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

@Composable
fun NotificationDetailDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit
) {
    if (visible) {
        AlertDialog(
            modifier = Modifier.padding(vertical = 28.dp),
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    text = stringResource(R.string.text_smart_notice_observe_notification)
                )
            },
            text = {
                DetailBox()
            },
            confirmButton = {
                Button(
                    onDismissRequest
                ) {
                    Text(
                        text = stringResource(R.string.text_confirm)
                    )
                }
            },
            dismissButton = {
                FilledTonalButton(
                    onDismissRequest
                ) {
                    Text(
                        text = stringResource(R.string.text_cancel)
                    )
                }
            }
        )
    }
}

@Composable
private fun DetailBox() {
    val storageStore = LocalStorageStore.current

    var switch by rememberSaveable { mutableStateOf(false) }
    var appFilterDialogState by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        switch = storageStore.getBoolean(
            Const.SmartNotice.Observe.SMART_NOTICE_OBSERVE_NOTIFICATION,
            true
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        FolderItem(
            title = {
                Text(
                    text = stringResource(R.string.text_smart_notice_observe_notification_switch)
                )
            }
        ) {
            Switch(
                checked = switch,
                onCheckedChange = {
                    switch = it
                    storageStore.putBoolean(
                        Const.SmartNotice.Observe.SMART_NOTICE_OBSERVE_NOTIFICATION,
                        switch
                    )
                }
            )
        }

        FolderItem(
            padding = PaddingValues(vertical = 12.dp),
            onClick = {
                appFilterDialogState = true
            },
            title = {
                Text(
                    text = stringResource(R.string.text_apps_manager)
                )
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_right),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    }

    AppsManagerDialog(appFilterDialogState) { appFilterDialogState = false }
}

@Composable
private fun AppsManagerDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
) {
    if (visible) {
        val activity = LocalActivity.current as Activity
        val storageStore = LocalStorageStore.current

        val ioScope = rememberCoroutineScope { Dispatchers.IO }
        var filter by rememberSaveable { mutableStateOf(listOf<String>()) }
        var popup by rememberSaveable { mutableStateOf(false) }
        var apps by remember { mutableStateOf(listOf<AppInfo>()) }
        var searchText by rememberSaveable { mutableStateOf("") }

        LaunchedEffect(Unit) {
            val filterText = TweakApplication.shared.getString(
                Const.SmartNotice.SMART_NOTICE_NOTIFICATION_FILTER,
                null
            ) ?: Const.SmartNotice.NOTIFICATION_FILTER_DEFAULT
            filter = json.decodeFromString<List<String>>(filterText)
        }

        LaunchedEffect(TweakApplication.userApps, filter, searchText) {
            apps = TweakApplication.userApps.sortedWith(
                comparator = compareBy<AppInfo> {
                    !filter.contains(it.packageName)
                }.thenBy {
                    it.appName
                }
            ).filter {
                it.appName.lowercase().contains(searchText.lowercase().trim()) ||
                        it.packageName.lowercase().contains(searchText.lowercase().trim())
            }
        }

        AlertDialog(
            modifier = Modifier.padding(vertical = 28.dp),
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    text = stringResource(R.string.text_apps_manager)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                text = stringResource(R.string.text_search_app)
                            )
                        },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(apps) { info ->
                            ListItem(
                                modifier = Modifier.fillMaxWidth(),
                                leadingContent = {
                                    OutlinedCard {
                                        AsyncImage(
                                            modifier = Modifier.size(40.dp),
                                            model = info.icon,
                                            contentDescription = null,
                                            error = painterResource(R.mipmap.ic_tweak_logo)
                                        )
                                    }
                                },
                                headlineContent = {
                                    Text(
                                        text = info.appName,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = info.packageName,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                trailingContent = {
                                    var enabled by remember(info) { mutableStateOf(false) }

                                    LaunchedEffect(info) {
                                        enabled = filter.contains(info.packageName)
                                    }

                                    Switch(
                                        checked = enabled,
                                        onCheckedChange = {
                                            enabled = it
                                            ioScope.launch {
                                                popup = true
                                                try {
                                                    val list = mutableListOf<String>()
                                                    list.addAll(filter)
                                                    if (it) {
                                                        list.add(info.packageName)
                                                    } else {
                                                        list.remove(info.packageName)
                                                    }
                                                    filter = list
                                                } finally {
                                                    popup = false
                                                }
                                            }
                                        },
                                        enabled = !popup
                                    )
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val filterText = json.encodeToString(filter)
                        storageStore.putString(
                            Const.SmartNotice.SMART_NOTICE_NOTIFICATION_FILTER,
                            filterText
                        )
                        onDismissRequest()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.text_save)
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