package io.github.lumkit.tweak.ui.component.dialog

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.model.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

@Composable
fun ClipImageDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    originFilePath: String,
    onClipped: (String) -> Unit,
) {
    val ioScope = rememberCoroutineScope { Dispatchers.IO }
    val context = LocalContext.current

    val density = LocalDensity.current
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(IntOffset.Zero) }

    val canvasSize = remember { DpSize(264.dp, 264.dp) }

    var targetWidth by remember { mutableStateOf(0f) }
    var targetHeight by remember { mutableStateOf(0f) }

    var imageBitmap: ImageBitmap? by remember { mutableStateOf(null) }

    LaunchedEffect(originFilePath) {
        withContext(Dispatchers.IO) {
            if (originFilePath.trim().isEmpty()) return@withContext
            try {
                imageBitmap = BitmapFactory.decodeFile(originFilePath).asImageBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
                onDismissRequest()
                imageBitmap = null
            }
        }
    }

    LaunchedEffect(scale) {
        val maxOffsetX = 0f
        val maxOffsetY = 0f
        val minOffsetX = -(targetWidth - with(density) { canvasSize.width.toPx() }).coerceAtLeast(0f)
        val minOffsetY = -(targetHeight - with(density) { canvasSize.height.toPx() }).coerceAtLeast(0f)

        // 更新偏移量，限制在范围内
        val newOffsetX = (offset.x.toFloat()).coerceIn(minOffsetX, maxOffsetX)
        val newOffsetY = (offset.y.toFloat()).coerceIn(minOffsetY, maxOffsetY)

        offset = IntOffset(newOffsetX.roundToInt(), newOffsetY.roundToInt())
    }

    if (visible) {
        AlertDialog(
            onDismissRequest = {
                offset = IntOffset.Zero
                scale = 1f
                imageBitmap = null
                onDismissRequest()
            },
            title = {
                Text(text = title)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .size(canvasSize)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()

                                    val maxOffsetX = 0f
                                    val maxOffsetY = 0f
                                    val minOffsetX = -(targetWidth - size.width).coerceAtLeast(0f)
                                    val minOffsetY = -(targetHeight - size.height).coerceAtLeast(0f)

                                    // 更新偏移量，限制在范围内
                                    val newOffsetX =
                                        (offset.x + dragAmount.x).coerceIn(minOffsetX, maxOffsetX)
                                    val newOffsetY =
                                        (offset.y + dragAmount.y).coerceIn(minOffsetY, maxOffsetY)

                                    offset =
                                        IntOffset(newOffsetX.roundToInt(), newOffsetY.roundToInt())

                                }
                            }
                    ) {
                        clipRect(
                            left = 0f,
                            top = 0f,
                            right = size.width,
                            bottom = size.height
                        ) {
                            imageBitmap?.let {
                                // 画布的宽高
                                val canvasWidth = size.width
                                val canvasHeight = size.height

                                // 图像的宽高
                                val imageWidth = it.width.toFloat()
                                val imageHeight = it.height.toFloat()

                                val scaleShow = if (imageWidth / imageHeight > canvasWidth / canvasHeight) {
                                    // 图像较宽，以画布宽度为准
                                    canvasHeight / imageHeight
                                } else {
                                    // 图像较高，以画布高度为准
                                    canvasWidth / imageWidth
                                }

                                // 缩放后的图像宽高
                                targetWidth = imageWidth * scaleShow * scale
                                targetHeight = imageHeight * scaleShow * scale

                                drawImage(
                                    it,
                                    srcSize = IntSize(it.width, it.height),
                                    dstSize = IntSize(targetWidth.roundToInt(), targetHeight.roundToInt()),
                                    dstOffset = IntOffset(offset.x, offset.y),
                                )
                            }

                            val rectPath = Path().apply {
                                addRect(size.toRect())
                            }

                            val holePath = Path().apply {
                                addOval(
                                    Rect(
                                        0f,
                                        0f,
                                        size.width,
                                        size.height
                                    )
                                )
                            }

                            val combinedPath = Path().apply {
                                op(rectPath, holePath, PathOperation.Difference)
                            }

                            drawPath(
                                path = combinedPath,
                                color = Color(0xAA000000),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val exitColor = MaterialTheme.colorScheme.outline
                    val enterColor = MaterialTheme.colorScheme.primary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        run {
                            var tint by remember { mutableStateOf(exitColor) }
                            Icon(
                                painter = painterResource(R.drawable.ic_minus),
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier
                                    .size(16.dp)
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                when (event.type) {
                                                    PointerEventType.Enter -> tint = enterColor
                                                    PointerEventType.Exit -> tint = exitColor
                                                    PointerEventType.Press -> {
                                                        if (scale > 1f) {
                                                            scale -= .1f
                                                        } else {
                                                            scale = 1f
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                            )
                        }

                        Slider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            value = scale,
                            onValueChange = {
                                scale = it
                            },
                            valueRange = 1f..5f
                        )

                        run {
                            var tint by remember { mutableStateOf(exitColor) }
                            Icon(
                                painter = painterResource(R.drawable.ic_plus),
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier
                                    .size(16.dp)
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                when (event.type) {
                                                    PointerEventType.Enter -> tint = enterColor
                                                    PointerEventType.Exit -> tint = exitColor
                                                    PointerEventType.Press -> {
                                                        if (scale < 5f) {
                                                            scale += .1f
                                                        } else {
                                                            scale = 5f
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                var enabled by remember { mutableStateOf(true) }
                Button(
                    onClick = {
                        ioScope.launch {
                            enabled = false
                            try {
                                val bitmap = imageBitmap ?: throw RuntimeException("图像数据丢失！")
                                val path = saveClippedImage(
                                    imageBitmap = bitmap,
                                    scale = scale,
                                    offset = IntOffset(offset.x, offset.y),
                                    canvasSize = with(density) {
                                        IntSize(
                                            canvasSize.width.toPx().roundToInt(),
                                            canvasSize.height.toPx().roundToInt()
                                        )
                                    },
                                )
                                offset = IntOffset.Zero
                                scale = 1f
                                onClipped(path)
                                imageBitmap = null
                                onDismissRequest()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        buildString {
                                            append(context.getString(R.string.text_save_fail_msg))
                                            append(e.message)
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } finally {
                                enabled = true
                            }
                        }
                    },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "保存"
                    )
                }
            }
        )
    }
}

fun saveClippedImage(
    imageBitmap: ImageBitmap,
    scale: Float,
    offset: IntOffset,
    canvasSize: IntSize,
): String {
    // 将 ImageBitmap 转换为 BufferedImage
    val bufferedImage = imageBitmap.asAndroidBitmap()

    val minLength = minOf(imageBitmap.width, imageBitmap.height)

    // 计算Bitmap和预览窗口的比例
    val bitmapToCanvasRatio = minLength / (canvasSize.width * scale)

    // 使用比例和偏移量计算裁剪区域
    val cropX = (-offset.x * bitmapToCanvasRatio).coerceAtLeast(0f).toInt()
    val cropY = (-offset.y * bitmapToCanvasRatio).coerceAtLeast(0f).toInt()
    val cropWidth = (canvasSize.width * bitmapToCanvasRatio).toInt().coerceAtMost(imageBitmap.width - cropX)
    val cropHeight = (canvasSize.height * bitmapToCanvasRatio).toInt().coerceAtMost(imageBitmap.height - cropY)

    // 检查裁剪区域是否有效
    if (cropWidth <= 0 || cropHeight <= 0) {
        throw IllegalArgumentException("裁剪区域无效，可能超出图片范围。")
    }

    // 从 BufferedImage 中裁剪出显示区域
    val croppedImage = Bitmap.createBitmap(bufferedImage, cropX, cropY, cropWidth, cropHeight)

    // 创建文件路径并保存裁剪后的图像
    val file = File(Config.Path.appCacheDir, "clip.jpg")

    // 压缩图像到1MB以下
    var quality = 100
    val maxFileSize = 1 * 1024 * 1024 // 1MB
    var fileOutputStream: FileOutputStream? = null
    try {
        fileOutputStream = FileOutputStream(file)

        // 尝试逐步降低质量，直到文件小于1MB
        do {
            croppedImage.compress(Bitmap.CompressFormat.JPEG, quality, fileOutputStream)
            fileOutputStream.flush()

            if (file.length() > maxFileSize) {
                quality -= 5 // 每次降低压缩质量
            }
        } while (file.length() > maxFileSize && quality > 5)

    } catch (e: Exception) {
        e.printStackTrace()
        throw IllegalStateException("保存并压缩图片时出错")
    } finally {
        fileOutputStream?.close()
    }

    return file.absolutePath
}