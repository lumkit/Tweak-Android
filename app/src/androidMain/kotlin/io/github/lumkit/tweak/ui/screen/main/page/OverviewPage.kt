package io.github.lumkit.tweak.ui.screen.main.page

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.common.util.autoUnit
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.ui.component.CircleIndicator
import io.github.lumkit.tweak.ui.component.HorizontalIndicator
import io.github.lumkit.tweak.ui.component.PlainTooltipBox
import io.github.lumkit.tweak.ui.component.RichTooltipBox
import io.github.lumkit.tweak.ui.local.LocalStorageStore
import io.github.lumkit.tweak.ui.local.StorageStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun OverviewPage(
    context: Context = LocalContext.current,
    viewModel: OverviewViewModel = viewModel { OverviewViewModel(context) }
) {
    val storageStore = LocalStorageStore.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        MemoryCard(viewModel, storageStore)
        GpuCard(viewModel, storageStore)
        SocCard(viewModel, storageStore)
        OtherCard(viewModel, storageStore)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoryCard(viewModel: OverviewViewModel, storageStore: StorageStore) {

    val memoryState by viewModel.memoryBeanState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        while (isActive) {
            viewModel.loadMemoryBeanState()
            delay(storageStore.getInt(Const.APP_OVERVIEW_TICK, default = 2000).toLong())
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
private fun GpuCard(viewModel: OverviewViewModel, storageStore: StorageStore) {

    val gpuBeanState by viewModel.gpuBeanState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        while (isActive) {
            viewModel.loadGpuBeanState()
            delay(storageStore.getInt(Const.APP_OVERVIEW_TICK, default = 2000).toLong())
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

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
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
private fun SocCard(viewModel: OverviewViewModel, storageStore: StorageStore) {

}

@Composable
private fun OtherCard(viewModel: OverviewViewModel, storageStore: StorageStore) {

}