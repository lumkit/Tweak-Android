package io.github.lumkit.tweak.ui.screen.main.page

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.common.util.AppInfoLoader
import io.github.lumkit.tweak.common.util.autoUnit
import io.github.lumkit.tweak.data.RuntimeStatus
import io.github.lumkit.tweak.model.Config
import io.github.lumkit.tweak.model.ProcessInfo
import io.github.lumkit.tweak.ui.component.CircleIndicator
import io.github.lumkit.tweak.ui.component.HorizontalIndicator
import io.github.lumkit.tweak.ui.component.LintStackChart
import io.github.lumkit.tweak.ui.component.PlainTooltipBox
import io.github.lumkit.tweak.ui.component.RichTooltipBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.painterResource
import tweak_android.app.generated.resources.Res
import tweak_android.app.generated.resources.ic_battery2
import tweak_android.app.generated.resources.ic_capacity
import tweak_android.app.generated.resources.ic_temperature
import tweak_android.app.generated.resources.icon_android
import tweak_android.app.generated.resources.icon_global
import tweak_android.app.generated.resources.mode_fast

@Composable
fun OverviewPage(
    context: Context = LocalContext.current,
    viewModel: OverviewViewModel = viewModel { OverviewViewModel(context) }
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        MemoryCard(viewModel)
        GpuCard(viewModel)
        SocCard(viewModel, context)
        OtherCard(viewModel)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoryCard(viewModel: OverviewViewModel) {

    val memoryState by viewModel.memoryBeanState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        while (isActive) {
            viewModel.loadMemoryBeanState()
            delay(Config.REFRESH_TICK)
        }
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RichTooltipBox(
                tooltip = {
                    Text(
                        text = buildAnnotatedString {
                            appendLine(
                                String.format(
                                    "%s: %s",
                                    stringResource(R.string.text_total_external_storage),
                                    memoryState.romTotal.autoUnit()
                                )
                            )
                            appendLine(
                                String.format(
                                    "%s: %s(%.2f%s)",
                                    stringResource(R.string.text_used_external_storage),
                                    (memoryState.romTotal - memoryState.romFree).autoUnit(),
                                    memoryState.rowUsed * 100f,
                                    "%"
                                )
                            )
                            append(
                                String.format(
                                    "%s: %s",
                                    stringResource(R.string.text_memory_free),
                                    memoryState.romFree.autoUnit()
                                )
                            )
                        }
                    )
                }
            ) {
                CircleIndicator(
                    circleDiameter = 90.dp,
                    backgroundIndicatorStrokeWidth = 12.dp,
                    progress = memoryState.rowUsed
                ) {
                    Text(
                        text = stringResource(R.string.text_storage),
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MemoryBar(memoryState)
                SwapBar(memoryState)
                ProvideTextStyle(
                    value = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        lineHeight = 8.sp
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append("Used: ")
                                withStyle(
                                    style = SpanStyle(color = MaterialTheme.colorScheme.outline)
                                ) {
                                    append(
                                        String.format(
                                            "%.0f%s",
                                            memoryState.totalUsed * 100f,
                                            "%"
                                        )
                                    )
                                }
                            }
                        )
                        Text(
                            text = buildAnnotatedString {
                                append("SwapCache: ")
                                withStyle(
                                    style = SpanStyle(color = MaterialTheme.colorScheme.outline)
                                ) {
                                    append(memoryState.swapCache.autoUnit(decimalDigits = 0))
                                }
                            }
                        )
                        Text(
                            text = buildAnnotatedString {
                                append("Dirty: ")
                                withStyle(
                                    style = SpanStyle(color = MaterialTheme.colorScheme.outline)
                                ) {
                                    append(memoryState.dirty.autoUnit(decimalDigits = 0))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun MemoryBar(memoryState: OverviewViewModel.MemoryBean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            HorizontalIndicator(
                progress = memoryState.ramUsed,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.text_memory_disk))
                    append("    ")
                    withStyle(
                        style = SpanStyle(color = MaterialTheme.colorScheme.outline)
                    ) {
                        append(
                            String.format(
                                "%s/%s(%.0f%s%s)",
                                memoryState.ramFree.autoUnit(false, 1),
                                memoryState.ramTotal.autoUnit(decimalDigits = 0),
                                (1f - memoryState.ramUsed) * 100f,
                                "%",
                                stringResource(R.string.text_memory_free)
                            )
                        )
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun SwapBar(memoryState: OverviewViewModel.MemoryBean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            HorizontalIndicator(
                progress = memoryState.swapUsed,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.text_swap))
                    append("    ")
                    withStyle(
                        style = SpanStyle(color = MaterialTheme.colorScheme.outline)
                    ) {
                        append(
                            String.format(
                                "%s/%s(%.0f%s%s)",
                                memoryState.swapFree.autoUnit(false, decimalDigits = 1),
                                memoryState.swapTotal.autoUnit(decimalDigits = 0),
                                (1f - memoryState.swapUsed) * 100f,
                                "%",
                                stringResource(R.string.text_memory_free)
                            )
                        )
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GpuCard(viewModel: OverviewViewModel) {

    val gpuBeanState by viewModel.gpuBeanState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        while (isActive) {
            viewModel.loadGpuBeanState()
            delay(Config.REFRESH_TICK)
        }
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (TweakApplication.runtimeStatus == RuntimeStatus.Root) {
                CircleIndicator(
                    circleDiameter = 90.dp,
                    backgroundIndicatorStrokeWidth = 12.dp,
                    progress = gpuBeanState.used
                ) {
                    Text(
                        text = "GPU",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else if (TweakApplication.runtimeStatus == RuntimeStatus.Shizuku) {
                when {
                    gpuBeanState.describe.lowercase().contains("qualcomm") -> {

                    }

                    gpuBeanState.describe.lowercase().contains("arm") -> {

                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (TweakApplication.runtimeStatus == RuntimeStatus.Root) {
                    Text(
                        text = "${gpuBeanState.currentFreq}MHz",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = String.format(
                            "%s: %.0f%s",
                            stringResource(R.string.text_load),
                            gpuBeanState.used * 100f,
                            "%"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                PlainTooltipBox(
                    tooltip = {
                        Text(gpuBeanState.describe)
                    }
                ) {
                    Text(
                        text = gpuBeanState.describe,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp,
                        lineHeight = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SocCard(viewModel: OverviewViewModel, context: Context) {

    LaunchedEffect(Unit) {
        while (isActive) {
            viewModel.loadCpuDetailState()
            delay(Config.REFRESH_TICK)
        }
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.outlinedCardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            ) {
                ServicesBar(viewModel, context)
                Spacer(modifier = Modifier.width(8.dp))
                VerticalDivider(modifier = Modifier.padding(bottom = 12.dp))
                Spacer(modifier = Modifier.width(8.dp))
                CpuTotalBar(viewModel)
            }
            HorizontalDivider()
            CpuLoadBar(viewModel)
        }
    }
}

@Composable
private fun RowScope.ServicesBar(viewModel: OverviewViewModel, context: Context) {

    val runningServicesDetailState by viewModel.runningServicesDetailState.collectAsStateWithLifecycle()
    val appInfoLoader = remember { AppInfoLoader(context, 100) }

    LaunchedEffect(Unit) {
        while (isActive){
            viewModel.loadRunningServicesDetailState()
            delay(Config.REFRESH_TICK)
        }
    }

    AnimatedContent(
        modifier = Modifier.weight(1f),
        targetState = runningServicesDetailState.isNotEmpty()
    ) {
        if (it) {
            Row {
                LazyColumn(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .weight(1f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(runningServicesDetailState) { processInfo ->
                        ServiceItem(processInfo, appInfoLoader)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                     modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun ServiceItem(processInfo: ProcessInfo, appInfoLoader: AppInfoLoader) {

    var icon by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(processInfo.name) {
        icon = appInfoLoader.loadIcon(processInfo.name).await()?.toBitmap()?.asImageBitmap()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable {

            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon == null) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_linux),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
            } else {
                icon?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Text(
                text = processInfo.name,
                overflow = TextOverflow.Ellipsis,
                fontSize = 8.sp,
                lineHeight = 8.sp,
                softWrap = false,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = String.format("%.2f%s", processInfo.cpu, "%"),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                lineHeight = 8.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun RowScope.CpuTotalBar(viewModel: OverviewViewModel) {

    val cpuDetailState by viewModel.cpuDetailState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .weight(1f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                Text(
                    text = cpuDetailState.cpuName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.Center)
                )
                LintStackChart(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(.4f),
                    state = cpuDetailState.cpuTotalUsed,
                )
            }
            Text(
                text = String.format(
                    "%s: %s",
                    stringResource(R.string.text_cpu_composition),
                    cpuDetailState.composition
                ),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = String.format(
                    "%s: %.2f%s",
                    stringResource(R.string.text_load),
                    cpuDetailState.cpuTotalUsed.progress * 100f,
                    "%"
                ),
                color = MaterialTheme.colorScheme.outline,
                fontSize = 10.sp,
                lineHeight = 10.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(
            text = String.format("%.1f℃", cpuDetailState.cpuTemperature),
            color = MaterialTheme.colorScheme.outline,
            fontSize = 8.sp,
            lineHeight = 8.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CpuLoadBar(viewModel: OverviewViewModel) {

    val cpuDetailState by viewModel.cpuDetailState.collectAsStateWithLifecycle()
    val columns by remember { derivedStateOf { cpuDetailState.cores.size / 2 } }

    AnimatedContent(
        targetState = cpuDetailState.cores.isNotEmpty()
    ) {
        if (it) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = columns,
            ) {
                cpuDetailState.cores.onEach { core ->
                    CoreItem(core)
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(32.dp)
                        .size(32.dp)
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowScope.CoreItem(coreDetail: OverviewViewModel.CoreDetail) {
    Column(
        modifier = Modifier
            .weight(1f)
            .height(65.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            Text(
                text = String.format("%.0f%s", coreDetail.used.progress * 100f, "%"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.Center)
            )
            LintStackChart(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(.4f),
                state = coreDetail.used,
            )
        }
        Text(
            text = String.format(
                "%.0fMHz",
                (coreDetail.currentFreq.toLongOrNull() ?: 0L) / 1000f
            ),
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = String.format(
                "%.0f~%.0fMHz",
                (coreDetail.minFreq.toLongOrNull() ?: 0L) / 1000f,
                (coreDetail.maxFreq.toLongOrNull() ?: 0L) / 1000f,
            ),
            color = MaterialTheme.colorScheme.outline,
            fontSize = 8.sp,
            lineHeight = 8.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun OtherCard(viewModel: OverviewViewModel) {

    val otherDetailState by viewModel.otherDetailState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        while (isActive) {
            viewModel.loadOtherDetailState()
            delay(Config.REFRESH_TICK)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.outlinedCardColors()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OtherRow(
                    icon = {
                        Icon(
                            painter = painterResource(Res.drawable.ic_battery2),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    text = {
                        Text(
                            text = String.format("%.0fmA", otherDetailState.electricCurrent),
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                )
                OtherRow(
                    icon = {
                        Icon(
                            painter = painterResource(Res.drawable.ic_capacity),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    text = {
                        Text(
                            text = String.format(
                                "%d%s  %s",
                                otherDetailState.batteryLevel,
                                "%",
                                if (otherDetailState.voltage < 0f) {
                                    ""
                                } else {
                                    String.format("%.2fv", otherDetailState.voltage)
                                }
                            ),
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                )
                OtherRow(
                    icon = {
                        Icon(
                            painter = painterResource(Res.drawable.ic_temperature),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    text = {
                        Text(
                            text = String.format("%.1f℃", otherDetailState.temperature),
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                )
            }
        }

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.outlinedCardColors()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OtherRow(
                    icon = {
                        Icon(
                            painter = painterResource(Res.drawable.icon_android),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    text = {
                        Text(
                            text = String.format(
                                "Android %s(%d)",
                                otherDetailState.androidVersion,
                                otherDetailState.androidSDK
                            ),
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                )
                OtherRow(
                    icon = {
                        Icon(
                            painter = painterResource(Res.drawable.mode_fast),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    text = {
                        Text(
                            text = String.format(
                                "%s %s",
                                stringResource(R.string.text_running_duration),
                                otherDetailState.runningDuration,
                            ),
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                )
                OtherRow(
                    icon = {
                        Icon(
                            painter = painterResource(Res.drawable.icon_global),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    text = {
                        Text(
                            text = buildAnnotatedString {
                                withLink(
                                    link = LinkAnnotation.Url("https://tweak.lumtoolkit.com")
                                ) {
                                    append(stringResource(R.string.text_goto_websit))
                                }
                            },
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun OtherRow(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.outline
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                text()
            }
        }
    }
}