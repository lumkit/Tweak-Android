package io.github.lumkit.tweak.ui.screen.notice

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.common.util.getDiveSize
import io.github.lumkit.tweak.common.util.makeText
import io.github.lumkit.tweak.data.SmartNoticeData
import io.github.lumkit.tweak.data.SmartNoticeGravity
import io.github.lumkit.tweak.data.SmartNoticeInterpolator
import io.github.lumkit.tweak.ui.component.FolderItem
import io.github.lumkit.tweak.ui.component.dialog.ValueEditDialog
import io.github.lumkit.tweak.ui.local.json
import io.github.lumkit.tweak.ui.view.SmartNoticeFactory
import io.github.lumkit.tweak.util.Aes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun SmartNoticePropertiesEditorDialog(
    visible: Boolean,
    activity: ComponentActivity,
    onDismissRequest: () -> Unit
) {
    if (visible) {
        val data by SmartNoticeFactory.globalSmartNoticeData.collectAsState()
        val density = LocalDensity.current

        AlertDialog(
            modifier = Modifier.padding(vertical = 28.dp),
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    text = stringResource(R.string.text_smart_notice_diy_properties)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    GravityRow(data) { SmartNoticeFactory.globalSmartNoticeData.value = it }
                    OffsetXRow(data, activity, density) { SmartNoticeFactory.globalSmartNoticeData.value = it }
                    OffsetYRow(data, activity, density) { SmartNoticeFactory.globalSmartNoticeData.value = it }
                    WidthRow(data, activity, density) { SmartNoticeFactory.globalSmartNoticeData.value = it }
                    HeightRow(data, activity, density) { SmartNoticeFactory.globalSmartNoticeData.value = it }
                    RadiusRow(data, activity, density) { SmartNoticeFactory.globalSmartNoticeData.value = it }
                    DurationRow(data, activity) { SmartNoticeFactory.globalSmartNoticeData.value = it }
                    DelayRow(data, activity) { SmartNoticeFactory.globalSmartNoticeData.value = it }
                    InterpolatorRow(data) { SmartNoticeFactory.globalSmartNoticeData.value = it }
                    ExportAndImportRow(activity)
                    ResetPropertiesRow()
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissRequest()
                        SmartNoticeFactory.saveData(activity)
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

@Composable
private fun GravityRow(
    data: SmartNoticeData,
    onChange: (SmartNoticeData) -> Unit,
) {
    FolderItem(
        title = {
            Text(
                text = stringResource(R.string.text_cutout_position)
            )
        },
        subtitle = {
            Text(
                text = stringResource(R.string.text_cutout_position_tips)
            )
        }
    ) {
        Column {
            var expanded by rememberSaveable { mutableStateOf(false) }
            TextButton(
                onClick = {
                    expanded = true
                }
            ) {
                Text(
                    text = data.gravity.asString()
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                SmartNoticeGravity.entries.forEach {
                    DropdownMenuItem(
                        leadingIcon = {
                            Checkbox(
                                checked = data.gravity == it,
                                onCheckedChange = null
                            )
                        },
                        text = {
                            Text(
                                text = it.asString()
                            )
                        },
                        onClick = {
                             val newData = data.copy(
                                 gravity = it
                             )
                            onChange(
                                newData
                             )
                            SmartNoticeFactory.globalSmartNoticeData.value = newData
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun OffsetXRow(
    data: SmartNoticeData,
    activity: Activity,
    density: Density,
    onChange: (SmartNoticeData) -> Unit,
) {
    var offsetX by rememberSaveable { mutableIntStateOf(data.x) }
    val screenSize = remember { activity.getDiveSize() }
    var valueEditDialogState by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(data) {
        offsetX = data.x
    }

    FolderItem(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.text_smart_notice_offset_x)
                )

                Text(
                    text = with(density) {
                        String.format(
                            "%.2fdp",
                            offsetX.toDp().value
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = null
                    ) {
                        valueEditDialogState = true
                    }
                )
            }
        },
        subtitle = {
            Slider(
                value = offsetX.toFloat(),
                onValueChange = {
                    offsetX = it.roundToInt()
                },
                onValueChangeFinished = {
                    onChange(
                        SmartNoticeFactory.globalSmartNoticeData
                            .value
                            .copy(x = offsetX)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                valueRange = remember {
                    max(-screenSize.width, -screenSize.height) .. min(screenSize.width, screenSize.height)
                }
            )
        }
    )

    ValueEditDialog(
        visible = valueEditDialogState,
        title = {
            Text(
                text = stringResource(R.string.text_smart_notice_offset_x)
            )
        },
        value = with(density) {
            offsetX.toDp().value.toString()
        }
    ) {
        if (it.isBlank()) {
            valueEditDialogState = false
            return@ValueEditDialog
        }
        try {
            val value = it.toFloat()
            if (value !in with(density) { max(-screenSize.width, -screenSize.height).toDp().value .. min(screenSize.width, screenSize.height).toDp().value }) {
                Toast.makeText(
                    activity,
                    R.string.text_value_is_out_bounds,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                valueEditDialogState = false
                with(density) {
                    onChange(
                        SmartNoticeFactory.globalSmartNoticeData
                            .value
                            .copy(x = value.dp.roundToPx())
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                activity,
                String.format(
                    activity.getString(R.string.text_throws_error),
                    e.message
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun OffsetYRow(
    data: SmartNoticeData,
    activity: Activity,
    density: Density,
    onChange: (SmartNoticeData) -> Unit,
) {
    var offsetY by rememberSaveable { mutableIntStateOf(data.y) }
    val screenSize = remember { activity.getDiveSize() }
    var valueEditDialogState by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(data) {
        offsetY = data.y
    }

    FolderItem(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.text_smart_notice_offset_y)
                )

                Text(
                    text = with(density) {
                        String.format(
                            "%.2fdp",
                            offsetY.toDp().value
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = null
                    ) {
                        valueEditDialogState = true
                    }
                )
            }
        },
        subtitle = {
            Slider(
                value = offsetY.toFloat(),
                onValueChange = {
                    offsetY = it.roundToInt()
                },
                onValueChangeFinished = {
                    onChange(
                        SmartNoticeFactory.globalSmartNoticeData
                            .value
                            .copy(y = offsetY)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                valueRange = remember {
                    0f .. min(screenSize.width, screenSize.height)
                }
            )
        }
    )

    ValueEditDialog(
        visible = valueEditDialogState,
        title = {
            Text(
                text = stringResource(R.string.text_smart_notice_offset_y)
            )
        },
        value = with(density) {
            offsetY.toDp().value.toString()
        }
    ) {
        if (it.isBlank()) {
            valueEditDialogState = false
            return@ValueEditDialog
        }
        try {
            val value = it.toFloat()
            if (value !in with(density) { 0f .. min(screenSize.width, screenSize.height).toDp().value }) {
                Toast.makeText(
                    activity,
                    R.string.text_value_is_out_bounds,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                valueEditDialogState = false
                with(density) {
                    onChange(
                        SmartNoticeFactory.globalSmartNoticeData
                            .value
                            .copy(y = value.dp.roundToPx())
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                activity,
                String.format(
                    activity.getString(R.string.text_throws_error),
                    e.message
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun WidthRow(
    data: SmartNoticeData,
    activity: Activity,
    density: Density,
    onChange: (SmartNoticeData) -> Unit,
) {
    var width by rememberSaveable { mutableIntStateOf(data.width) }
    val screenSize = remember { activity.getDiveSize() }
    var valueEditDialogState by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(data) {
        width = data.width
    }

    FolderItem(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.text_smart_notice_width)
                )

                Text(
                    text = with(density) {
                        String.format(
                            "%.2fdp",
                            width.toDp().value
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = null
                    ) {
                        valueEditDialogState = true
                    }
                )
            }
        },
        subtitle = {
            Slider(
                value = width.toFloat(),
                onValueChange = {
                    width = it.roundToInt()
                },
                onValueChangeFinished = {
                    onChange(
                        SmartNoticeFactory.globalSmartNoticeData
                            .value
                            .copy(width = width)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                valueRange = remember {
                    0f .. min(screenSize.width, screenSize.height)
                }
            )
        }
    )

    ValueEditDialog(
        visible = valueEditDialogState,
        title = {
            Text(
                text = stringResource(R.string.text_smart_notice_width)
            )
        },
        value = with(density) {
            width.toDp().value.toString()
        }
    ) {
        if (it.isBlank()) {
            valueEditDialogState = false
            return@ValueEditDialog
        }
        try {
            val value = it.toFloat()
            if (value !in with(density) { 0f .. min(screenSize.width, screenSize.height).toDp().value }) {
                Toast.makeText(
                    activity,
                    R.string.text_value_is_out_bounds,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                valueEditDialogState = false
                with(density) {
                    onChange(
                        SmartNoticeFactory.globalSmartNoticeData
                            .value
                            .copy(width = value.dp.roundToPx())
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                activity,
                String.format(
                    activity.getString(R.string.text_throws_error),
                    e.message
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun HeightRow(
    data: SmartNoticeData,
    activity: Activity,
    density: Density,
    onChange: (SmartNoticeData) -> Unit,
) {
    var height by rememberSaveable { mutableIntStateOf(data.height) }
    val screenSize = remember { activity.getDiveSize() }
    var valueEditDialogState by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(data) {
        height = data.height
    }

    FolderItem(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.text_smart_notice_height)
                )

                Text(
                    text = with(density) {
                        String.format(
                            "%.2fdp",
                            height.toDp().value
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = null
                    ) {
                        valueEditDialogState = true
                    }
                )
            }
        },
        subtitle = {
            Slider(
                value = height.toFloat(),
                onValueChange = {
                    height = it.roundToInt()
                },
                onValueChangeFinished = {
                    onChange(
                        SmartNoticeFactory.globalSmartNoticeData
                            .value
                            .copy(height = height)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                valueRange = remember {
                    0f .. min(screenSize.width, screenSize.height)
                }
            )
        }
    )

    ValueEditDialog(
        visible = valueEditDialogState,
        title = {
            Text(
                text = stringResource(R.string.text_smart_notice_height)
            )
        },
        value = with(density) {
            height.toDp().value.toString()
        }
    ) {
        if (it.isBlank()) {
            valueEditDialogState = false
            return@ValueEditDialog
        }
        try {
            val value = it.toFloat()
            if (value !in with(density) { 0f .. min(screenSize.width, screenSize.height).toDp().value }) {
                Toast.makeText(
                    activity,
                    R.string.text_value_is_out_bounds,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                valueEditDialogState = false
                with(density) {
                    onChange(
                        SmartNoticeFactory.globalSmartNoticeData
                            .value
                            .copy(height = value.dp.roundToPx())
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                activity,
                String.format(
                    activity.getString(R.string.text_throws_error),
                    e.message
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun RadiusRow(
    data: SmartNoticeData,
    activity: Activity,
    density: Density,
    onChange: (SmartNoticeData) -> Unit,
) {
    var radius by rememberSaveable { mutableFloatStateOf(data.radius) }
    val screenSize = remember { activity.getDiveSize() }
    var valueEditDialogState by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(data) {
        radius = data.radius
    }

    FolderItem(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.text_smart_notice_radius)
                )

                Text(
                    text = with(density) {
                        String.format(
                            "%.2fdp",
                            radius.toDp().value
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = null
                    ) {
                        valueEditDialogState = true
                    }
                )
            }
        },
        subtitle = {
            Slider(
                value = radius,
                onValueChange = {
                    radius = it
                },
                onValueChangeFinished = {
                    onChange(
                        SmartNoticeFactory.globalSmartNoticeData
                            .value
                            .copy(radius = radius)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                valueRange = remember {
                    0f .. (min(screenSize.width, screenSize.height) / 2f)
                }
            )
        }
    )

    ValueEditDialog(
        visible = valueEditDialogState,
        title = {
            Text(
                text = stringResource(R.string.text_smart_notice_radius)
            )
        },
        value = with(density) {
            radius.toDp().value.toString()
        }
    ) {
        if (it.isBlank()) {
            valueEditDialogState = false
            return@ValueEditDialog
        }
        try {
            val value = it.toFloat()
            if (value !in with(density) { 0f .. (min(screenSize.width, screenSize.height) / 2f).toDp().value }) {
                Toast.makeText(
                    activity,
                    R.string.text_value_is_out_bounds,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                valueEditDialogState = false
                with(density) {
                    onChange(
                        SmartNoticeFactory.globalSmartNoticeData
                            .value
                            .copy(radius = value.dp.toPx())
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                activity,
                String.format(
                    activity.getString(R.string.text_throws_error),
                    e.message
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun DurationRow(
    data: SmartNoticeData,
    activity: Activity,
    onChange: (SmartNoticeData) -> Unit,
) {
    var duration by rememberSaveable { mutableLongStateOf(data.duration) }
    var valueEditDialogState by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(data) {
        duration = data.duration
    }

    FolderItem(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.text_smart_notice_animation_duration)
                )

                Text(
                    text = String.format(
                        "%dms",
                        duration
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = null
                    ) {
                        valueEditDialogState = true
                    }
                )
            }
        },
        subtitle = {
            Slider(
                value = duration.toFloat(),
                onValueChange = {
                    duration = it.roundToLong()
                },
                onValueChangeFinished = {
                    onChange(
                        SmartNoticeFactory.globalSmartNoticeData
                            .value
                            .copy(duration = duration)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                valueRange = remember {
                    0f .. 5_000f
                }
            )
        }
    )

    ValueEditDialog(
        visible = valueEditDialogState,
        title = {
            Text(
                text = stringResource(R.string.text_smart_notice_animation_duration)
            )
        },
        unit = "ms",
        value = duration.toString()
    ) {
        if (it.isBlank()) {
            valueEditDialogState = false
            return@ValueEditDialog
        }
        try {
            val value = it.toLong()
            if (value !in 0L .. 5_000L) {
                Toast.makeText(
                    activity,
                    R.string.text_value_is_out_bounds,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                valueEditDialogState = false
                onChange(
                    SmartNoticeFactory.globalSmartNoticeData
                        .value
                        .copy(duration = value)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                activity,
                String.format(
                    activity.getString(R.string.text_throws_error),
                    e.message
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun DelayRow(
    data: SmartNoticeData,
    activity: Activity,
    onChange: (SmartNoticeData) -> Unit,
) {
    var delay by rememberSaveable { mutableLongStateOf(data.delay) }
    var valueEditDialogState by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(data) {
        delay = data.delay
    }

    FolderItem(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.text_smart_notice_delay_duration)
                )

                Text(
                    text = String.format(
                        "%dms",
                        delay
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = null
                    ) {
                        valueEditDialogState = true
                    }
                )
            }
        },
        subtitle = {
            Slider(
                value = delay.toFloat(),
                onValueChange = {
                    delay = it.roundToLong()
                },
                onValueChangeFinished = {
                    onChange(
                        SmartNoticeFactory.globalSmartNoticeData
                            .value
                            .copy(delay = delay)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                valueRange = remember {
                    500f .. 10_000f
                }
            )
        }
    )

    ValueEditDialog(
        visible = valueEditDialogState,
        title = {
            Text(
                text = stringResource(R.string.text_smart_notice_delay_duration)
            )
        },
        unit = "ms",
        value = delay.toString()
    ) {
        if (it.isBlank()) {
            valueEditDialogState = false
            return@ValueEditDialog
        }
        try {
            val value = it.toLong()
            if (value !in 500L .. 10_000L) {
                Toast.makeText(
                    activity,
                    R.string.text_value_is_out_bounds,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                valueEditDialogState = false
                onChange(
                    SmartNoticeFactory.globalSmartNoticeData
                        .value
                        .copy(delay = value)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                activity,
                String.format(
                    activity.getString(R.string.text_throws_error),
                    e.message
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@Composable
private fun InterpolatorRow(
    data: SmartNoticeData,
    onChange: (SmartNoticeData) -> Unit,
) {
    var interpolator by rememberSaveable { mutableStateOf(data.interpolator) }

    LaunchedEffect(data) {
        interpolator = data.interpolator
    }
    val density = LocalDensity.current

    FolderItem(
        title = {
            Text(
                text = stringResource(R.string.text_smart_notice_animation_interpolator)
            )
        },
        subtitle = {

        }
    ) {
        var expanded by rememberSaveable { mutableStateOf(false) }
        Column(
            horizontalAlignment = Alignment.End
        ) {
            TextButton(
                modifier = Modifier.width(125.dp),
                onClick = {
                    expanded = true
                }
            ) {
                Text(
                    text = interpolator.asString(),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                SmartNoticeInterpolator.entries.forEach {
                    DropdownMenuItem(
                        leadingIcon = {
                            Checkbox(
                                checked = interpolator == it,
                                onCheckedChange = null
                            )
                        },
                        text = {
                            Text(
                                text = it.asString()
                            )
                        },
                        onClick = {
                            onChange(
                                SmartNoticeFactory.globalSmartNoticeData
                                    .value.copy(
                                    interpolator = it
                                )
                            )
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportAndImportRow(
    activity: Activity,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ImportButton(activity)
        ExportButton(activity)
    }
}

@Composable
private fun RowScope.ImportButton(activity: Activity) {
    var dialogState by rememberSaveable { mutableStateOf(false) }

    TextButton(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        onClick = {
            dialogState = true
        },
    ) {
        Text(
            text = stringResource(R.string.text_import_configs)
        )
    }

    ImportDialog(dialogState, activity) { dialogState = false }
}

@Composable
private fun RowScope.ExportButton(activity: Activity) {
    var dialogState by rememberSaveable { mutableStateOf(false) }

    TextButton(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        onClick = {
            dialogState = true
        },
    ) {
        Text(
            text = stringResource(R.string.text_export_configs)
        )
    }

    ExportDialog(dialogState, activity) { dialogState = false }
}

@Composable
private fun ImportDialog(
    visible: Boolean,
    activity: Activity,
    onDismissRequest: () -> Unit
) {
    if (visible) {
        val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
        var text by rememberSaveable { mutableStateOf("") }
        var dialogState by rememberSaveable { mutableStateOf(false) }
        var propertiesImport by rememberSaveable { mutableStateOf<SmartNoticeData?>(null) }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    text = stringResource(R.string.text_import_configs)
                )
            },
            text = {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = text,
                    onValueChange = {
                        text = it
                    },
                    singleLine = true,
                    label = {
                        Text(
                            text = stringResource(R.string.text_input_properties)
                        )
                    }
                )
            },
            confirmButton = {
                var enabled by rememberSaveable { mutableStateOf(true) }
                Button(
                    onClick = {
                        coroutineScope.launch {
                            enabled = false
                            try {
                                propertiesImport = null
                                val decrypt = Aes.decrypt(
                                    TweakApplication.application,
                                    if (text.startsWith("tweak-smart-notice://")) {
                                        text.substring("tweak-smart-notice://".length)
                                    } else {
                                        text
                                    }
                                ).toString()
                                propertiesImport = json.decodeFromString<SmartNoticeData>(decrypt)
                                dialogState = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        activity,
                                        activity.getString(R.string.text_import_configs_fail) + e.makeText(),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } finally {
                                enabled = true
                            }
                        }
                    },
                    enabled = enabled
                ) {
                    Text(
                        text = stringResource(R.string.text_import)
                    )
                }
            },
            dismissButton = {
                FilledTonalButton(
                    onClick = onDismissRequest
                ) {
                    Text(
                        text = stringResource(R.string.text_cancel)
                    )
                }
            }
        )

        if (dialogState) {
            AlertDialog(
                onDismissRequest = {
                    dialogState = false
                },
                title = {
                    Text(
                        text = stringResource(R.string.text_alert)
                    )
                },
                text = {
                    Text(
                        text = String.format(
                            stringResource(R.string.text_import_configs_ask),
                            propertiesImport?.label ?: stringResource(R.string.text_unknown)
                        )
                    )
                },
                confirmButton = {
                    var enabled by rememberSaveable { mutableStateOf(true) }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                enabled = false
                                try {
                                    if (propertiesImport == null) {
                                        throw NullPointerException(activity.getString(R.string.text_properties_null))
                                    }

                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            activity,
                                            R.string.text_import_configs_success,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        SmartNoticeFactory.importProperties(activity, propertiesImport)
                                        dialogState = false
                                        onDismissRequest()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            activity,
                                            e.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } finally {
                                    enabled = true
                                }
                            }
                        },
                        enabled = enabled
                    ) {
                        Text(
                            text = stringResource(R.string.text_confirm)
                        )
                    }
                },
                dismissButton = {
                    FilledTonalButton(
                        onClick = {
                            dialogState = false
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.text_cancel)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun ExportDialog(
    visible: Boolean,
    activity: Activity,
    onDismissRequest: () -> Unit
) {
    if (visible) {
        val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
        var label by rememberSaveable { mutableStateOf("") }
        val clipboard = LocalClipboard.current

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    text = stringResource(R.string.text_export_configs)
                )
            },
            text = {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = label,
                    onValueChange = {
                        label = it
                    },
                    singleLine = true,
                    label = {
                        Text(
                            text = stringResource(R.string.text_input_label)
                        )
                    }
                )
            },
            confirmButton = {
                var enabled by rememberSaveable { mutableStateOf(true) }
                Button(
                    onClick = {
                        coroutineScope.launch {
                            enabled = false
                            val smartNoticeData = SmartNoticeFactory.globalSmartNoticeData.value.copy(
                                label = label
                            )
                            val jsonText = json.encodeToString(smartNoticeData)
                            val encrypt = Aes.encrypt(TweakApplication.application, jsonText)

                            withContext(Dispatchers.Main) {
                                clipboard.setClipEntry(
                                    ClipEntry(
                                        clipData = ClipData.newPlainText(
                                            label,
                                            buildString {
                                                append("tweak-smart-notice://")
                                                append(encrypt)
                                            }
                                        )
                                    )
                                )

                                Toast.makeText(
                                    activity,
                                    R.string.text_configs_exported,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            enabled = true
                            onDismissRequest()
                        }
                    },
                    enabled = enabled
                ) {
                    Text(
                        text = stringResource(R.string.text_copy_to_cut)
                    )
                }
            },
            dismissButton = {
                FilledTonalButton(
                    onClick = onDismissRequest
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
private fun ResetPropertiesRow() {
    var resetDialogState by rememberSaveable { mutableStateOf(false) }
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            resetDialogState = true
        },
        colors = ButtonDefaults.buttonColors()
            .copy(
                containerColor = MaterialTheme.colorScheme.error
            )
    ) {
        Text(
            text = stringResource(R.string.text_smart_notice_reset_properties)
        )
    }

    if (resetDialogState) {
        AlertDialog(
            onDismissRequest = { resetDialogState = false },
            title = {
                Text(
                    text = stringResource(R.string.text_alert)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.text_smart_notice_reset_properties_msg)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        SmartNoticeFactory.globalSmartNoticeData.value = SmartNoticeFactory.smartNoticeDataDefault
                        resetDialogState = false
                    },
                ) {
                    Text(
                        text = stringResource(R.string.text_confirm)
                    )
                }
            },
            dismissButton = {
                FilledTonalButton(
                    onClick = {
                        resetDialogState = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.text_cancel)
                    )
                }
            }
        )
    }
}