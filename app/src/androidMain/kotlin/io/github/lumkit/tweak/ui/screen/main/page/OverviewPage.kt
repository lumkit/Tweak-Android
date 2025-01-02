package io.github.lumkit.tweak.ui.screen.main.page

import android.annotation.SuppressLint
import androidx.annotation.FloatRange
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.common.util.autoUnit
import io.github.lumkit.tweak.ui.component.CircleIndicator
import io.github.lumkit.tweak.ui.component.HorizontalIndicator
import io.github.lumkit.tweak.ui.component.RichTooltipBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

@Composable
fun OverviewPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        MemoryCard()
        GpuCard()
        SocCard()
        OtherCard()
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Immutable
data class MemoryBean(
    val ramTotal: Long = 100_000,
    val ramFree: Long = 100_000,
    @FloatRange(from = 0.0, to = 1.0) val ramUsed: Float = 0f,
    val romTotal: Long = 100_000,
    val romFree: Long = 100_000,
    @FloatRange(from = 0.0, to = 1.0) val rowUsed: Float = 0f,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoryCard() {

    var memoryState by remember { mutableStateOf(MemoryBean()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            memoryState = MemoryBean(
                ramUsed = Random.nextDouble(from = 0.0, until = 1.0).toFloat(),
                rowUsed = Random.nextDouble(from = 0.0, until = 1.0).toFloat(),
            )
            delay(2000)
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RichTooltipBox(
                tooltip = {
                    Text(
                        text = "使用量：${memoryState.rowUsed}"
                    )
                }
            ) {
                CircleIndicator(
                    circleDiameter = 110.dp,
                    backgroundIndicatorStrokeWidth = 16.dp,
                    progress = memoryState.rowUsed
                ) {
                    Text(
                        text = stringResource(R.string.text_storage),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MemoryBar(memoryState)
                SwapBar(memoryState)
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun MemoryBar(memoryState: MemoryBean) {
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
                                memoryState.ramFree.autoUnit(false),
                                memoryState.ramTotal.autoUnit(),
                                memoryState.ramUsed * 100f,
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

@Composable
private fun SwapBar(memoryState: MemoryBean) {

}

@Composable
private fun GpuCard() {

}

@Composable
private fun SocCard() {

}

@Composable
private fun OtherCard() {

}