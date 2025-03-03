package io.github.lumkit.tweak.ui.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.RelativeLayout
import androidx.cardview.widget.CardView
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import io.github.lumkit.tweak.Main
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.TweakApplication.Companion.density
import io.github.lumkit.tweak.common.util.getStatusBarHeight
import io.github.lumkit.tweak.data.CutoutRect
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.ui.local.json
import io.github.lumkit.tweak.ui.theme.AppTheme
import io.github.lumkit.tweak.ui.token.SmartNoticeCapsuleDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
class SmartNoticeWindow(
    context: Context,
    private val windowManager: WindowManager,
    private val windowLayoutParams: WindowManager.LayoutParams,
) : CardView(context) {

    companion object {

        enum class Gravity(
            val gravity: Int,
        ) {
            Start(android.view.Gravity.START), Center(android.view.Gravity.CENTER), End(android.view.Gravity.END);

            fun asString(): String = when (this) {
                Start -> TweakApplication.application.getString(R.string.text_cutout_gravity_start)
                Center -> TweakApplication.application.getString(R.string.text_cutout_gravity_center)
                End -> TweakApplication.application.getString(R.string.text_cutout_gravity_end)
            }
        }

        private val _cutoutRectListState by lazy {
            MutableStateFlow(
                json.decodeFromString<List<CutoutRect>>(
                    TweakApplication.shared.getString(
                        Const.SmartNotice.SMART_NOTICE_CUTOUT_RECT_LIST,
                        null
                    ) ?: "[]"
                )
            )
        }

        val cutoutRectListState by lazy { _cutoutRectListState.asStateFlow() }

        private val _islandDefaultSize = MutableStateFlow(Size.Zero)
        val islandDefaultSize = _islandDefaultSize.asStateFlow()

        var animatorDelay = MutableStateFlow(5000L)

        var animatorDuration = MutableStateFlow(550L)

        private val _islandCustomSize by lazy {
            MutableStateFlow(Size.Zero)
        }
        val islandCustomSize by lazy {
            _islandCustomSize.asStateFlow()
        }

        fun updateCutout(list: List<CutoutRect>) {
            _cutoutRectListState.value = list
        }

        fun setCustomSize(size: Size) {
            _islandCustomSize.value = size
        }
    }

    private val viewCoroutine = CoroutineScope(Dispatchers.Default)
    private var displayed = false
    private val container: RelativeLayout
    private val animationContainer: ComposeView

    init {
        alpha = 0f
        setCardBackgroundColor(Color.Black.toArgb())
        cardElevation = 0f

        container = RelativeLayout(context)
        container.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(container)

        animationContainer = ComposeView(context).apply {
            this.alpha = 0f
        }
        animationContainer.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        ).apply {
            addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        }
        container.addView(animationContainer)

        // 属性监听
        snapshot()
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun snapshot() = with(density) {
        viewCoroutine.launch {

            _islandCustomSize.value = Size(
                TweakApplication.shared.getFloat(
                    Const.SmartNotice.SMART_NOTICE_WIDTH,
                    SmartNoticeCapsuleDefault.CapsuleWidth.value
                ).dp.toPx(),
                TweakApplication.shared.getFloat(
                    Const.SmartNotice.SMART_NOTICE_HEIGHT,
                    SmartNoticeCapsuleDefault.CapsuleHeight.value
                ).dp.toPx()
            )

            animatorDelay.value = TweakApplication.shared.getLong(
                Const.SmartNotice.SMART_NOTICE_ANIMATION_DELAY,
                5000L
            )

            animatorDuration.value = TweakApplication.shared.getLong(
                Const.SmartNotice.SMART_NOTICE_ANIMATION_DURATION,
                550L
            )

            launch {
                _cutoutRectListState.collect {
                    val size = it.size
                    val width = if (size <= 1) {
                        SmartNoticeCapsuleDefault.CapsuleWidth.toPx()
                    } else {
                        val first = it.first()
                        val last = it.last()

                        last.right - first.left + SmartNoticeCapsuleDefault.CapsulePadding.toPx() * 2f
                    }

                    val height = if (size < 1) {
                        SmartNoticeCapsuleDefault.CapsuleHeight.toPx()
                    } else {
                        val first = it.first()
                        first.right - first.left + SmartNoticeCapsuleDefault.CapsulePadding.toPx() * 2f
                    }

                    _islandDefaultSize.value = Size(width, height)
                }
            }

            launch {
                _islandDefaultSize.filter { it != Size.Zero }.collect {
                    if (_islandCustomSize.value == Size.Zero) {
                        _islandCustomSize.value = it
                    }
                }
            }

            launch {
                _islandCustomSize.filter { it != Size.Zero }.collect {
                    withContext(Dispatchers.Main) {
                        if (TweakApplication.shared.getFloat(
                                Const.SmartNotice.SMART_NOTICE_CUTOUT_RADIUS,
                                -1f
                            ) == -1f
                        ) {
                            radius = min(it.width, it.height) / 2f
                            TweakApplication.shared.edit {
                                putFloat(
                                    Const.SmartNotice.SMART_NOTICE_CUTOUT_RADIUS,
                                    radius.toDp().value
                                )
                            }
                        }
                        TweakApplication.shared.edit {
                            putFloat(Const.SmartNotice.SMART_NOTICE_WIDTH, it.width.toDp().value)
                            putFloat(Const.SmartNotice.SMART_NOTICE_HEIGHT, it.height.toDp().value)
                        }
                    }
                }
            }

            launch {
                animatorDelay.collect {
                    TweakApplication.shared.edit(
                        commit = true
                    ) {
                        putLong(Const.SmartNotice.SMART_NOTICE_ANIMATION_DELAY, it)
                    }
                }
            }

            launch {
                animatorDuration.collect {
                    TweakApplication.shared.edit(
                        commit = true
                    ) {
                        putLong(Const.SmartNotice.SMART_NOTICE_ANIMATION_DURATION, it)
                    }
                }
            }
        }
    }

    fun release() {
        viewCoroutine.cancel()
        displayed = false
    }

    private var animatorSetEnd: AnimatorSet? = null
    private var animatorSetStart: AnimatorSet? = null

    @SuppressLint("ObjectAnimatorBinding")
    fun show() {
        with(density) {
            viewCoroutine.launch {
                val size = _islandCustomSize.filter { it != Size.Zero }
                    .first()
                withContext(Dispatchers.Main) {
                    animatorSetStart?.apply {
                        cancel()
                        removeAllListeners()
                    }
                    animatorSetStart = AnimatorSet()

                    animatorSetEnd?.apply {
                        cancel()
                        removeAllListeners()
                    }
                    animatorSetEnd = AnimatorSet()

                    val localRadius = TweakApplication.shared.getFloat(
                        Const.SmartNotice.SMART_NOTICE_CUTOUT_RADIUS,
                        -1f
                    )
                    radius = if (localRadius == -1f) {
                        size.height / 2f
                    } else {
                        localRadius.dp.toPx()
                    }

                    val localOffsetX = TweakApplication.shared.getFloat(
                        Const.SmartNotice.SMART_NOTICE_OFFSET_X,
                        0f
                    )
                    val localOffsetY = TweakApplication.shared.getFloat(
                        Const.SmartNotice.SMART_NOTICE_OFFSET_Y,
                        -1f
                    )

                    windowLayoutParams.y = if (localOffsetY == -1f) {
                        getStatusBarHeight() - size.height.roundToInt() - SmartNoticeCapsuleDefault.CapsulePaddingTop.roundToPx() / 2
                    } else {
                        localOffsetY.dp.roundToPx()
                    }

                    windowLayoutParams.x = localOffsetX.dp.roundToPx()

                    val width = ObjectAnimator.ofFloat(
                        this@SmartNoticeWindow,
                        "width",
                        windowLayoutParams.width.toFloat().let { if (it < 0) 0f else it },
                        size.width
                    ).apply {
                        addUpdateListener {
                            windowLayoutParams.width = (it.animatedValue as Float).roundToInt()
                            try {
                                windowManager.updateViewLayout(
                                    this@SmartNoticeWindow,
                                    windowLayoutParams
                                )
                            } catch (_: Exception) {

                            }
                        }
                    }
                    val height = ObjectAnimator.ofFloat(
                        this@SmartNoticeWindow,
                        "height",
                        windowLayoutParams.height.toFloat().let { if (it < 0) 0f else it },
                        size.height
                    ).apply {
                        addUpdateListener {
                            windowLayoutParams.height = (it.animatedValue as Float).roundToInt()
                            try {
                                windowManager.updateViewLayout(
                                    this@SmartNoticeWindow,
                                    windowLayoutParams
                                )
                            } catch (_: Exception) {

                            }
                        }
                    }

                    val alpha = ObjectAnimator.ofFloat(
                        this@SmartNoticeWindow,
                        "alpha",
                        this@SmartNoticeWindow.alpha,
                        1f,
                    )

                    animatorSetStart?.playTogether(width, height, alpha)
                    animatorSetStart?.duration = animatorDuration.value
                    animatorSetStart?.addListener(
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                super.onAnimationEnd(animation)
                                displayed = true
                            }
                        }
                    )

                    animatorSetStart?.start()
                }
            }
        }
    }

    @SuppressLint("ObjectAnimatorBinding")
    fun hide() {
        displayed = false

        animatorSetStart?.apply {
            cancel()
            removeAllListeners()
        }
        animatorSetStart = AnimatorSet()

        animatorSetEnd?.apply {
            cancel()
            removeAllListeners()
        }
        animatorSetStart = null
        animatorSetEnd = AnimatorSet()

        val width = ObjectAnimator.ofFloat(
            this@SmartNoticeWindow,
            "width",
            windowLayoutParams.width.toFloat().let { if (it < 0) 0f else it },
            0f
        ).apply {
            addUpdateListener {
                windowLayoutParams.width = (it.animatedValue as Float).roundToInt()
                try {
                    windowManager.updateViewLayout(this@SmartNoticeWindow, windowLayoutParams)
                } catch (_: Exception) {

                }
            }
        }
        val height = ObjectAnimator.ofFloat(
            this@SmartNoticeWindow,
            "height",
            windowLayoutParams.height.toFloat().let { if (it < 0) 0f else it }, 0f
        ).apply {
            addUpdateListener {
                windowLayoutParams.height = (it.animatedValue as Float).roundToInt()
                try {
                    windowManager.updateViewLayout(this@SmartNoticeWindow, windowLayoutParams)
                } catch (_: Exception) {

                }
            }
        }

        val alpha = ObjectAnimator.ofFloat(
            this@SmartNoticeWindow,
            "alpha",
            this.alpha,
            0f,
        )

        animatorSetEnd?.playTogether(width, height, alpha)
        animatorSetEnd?.duration = animatorDuration.value

        animatorSetEnd?.start()
    }

    abstract class SmartNoticeWindowScope {
        abstract val cutoutRect: Rect?
        abstract val startSize: Size
        abstract val density: Density
    }

    private var startSet: AnimatorSet? = null
    private var endSet: AnimatorSet? = null

    @SuppressLint("ObjectAnimatorBinding")
    fun toast(
        componentSize: SmartNoticeWindowScope.() -> DpSize,
        content: @Composable Density.(SmartNoticeWindow) -> Unit,
    ) {
        try {
            startSet?.cancel()
            startSet?.removeAllListeners()

            endSet?.cancel()
            endSet?.removeAllListeners()

            startSet = AnimatorSet()
            endSet = AnimatorSet()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        with(density) {
            viewCoroutine.launch {
                val size = _islandCustomSize.filter { it != Size.Zero }
                    .first()

                val rect = _cutoutRectListState
                    .map {
                        if (it.isEmpty()) {
                            null
                        } else {
                            val first = it.first()
                            val last = it.last()
                            Rect(
                                first.left,
                                first.top,
                                last.right,
                                last.bottom
                            )
                        }
                    }.first()

                val targetSize = componentSize(
                    object : SmartNoticeWindowScope() {
                        override val cutoutRect: Rect?
                            get() = rect
                        override val startSize: Size
                            get() = size
                        override val density: Density
                            get() = this@with
                    }
                ).toSize()

                withContext(Dispatchers.Main) {
                    animationContainer.setContent {
                        Main {
                            AppTheme {
                                Surface(
                                    color = Color.Black,
                                    contentColor = MaterialTheme.colorScheme.surface,
                                ) {
                                    content(this@SmartNoticeWindow)
                                }
                            }
                        }
                    }

                    val startChargeWidth = ObjectAnimator.ofFloat(
                        this@SmartNoticeWindow,
                        "width",
                        windowLayoutParams.width.toFloat(),
                        targetSize.width,
                    ).apply {
                        addUpdateListener {
                            val roundToInt = (it.animatedValue as Float).roundToInt()
                            windowLayoutParams.width = roundToInt
                            try {
                                windowManager.updateViewLayout(
                                    this@SmartNoticeWindow,
                                    windowLayoutParams
                                )
                            } catch (_: Exception) {

                            }
                        }
                    }
                    startChargeWidth.duration = animatorDuration.value

                    val startChargeHeight = ObjectAnimator.ofFloat(
                        this@SmartNoticeWindow,
                        "height",
                        windowLayoutParams.height.toFloat(),
                        targetSize.height,
                    ).apply {
                        addUpdateListener {
                            if (targetSize.height >= 1f) {
                                val roundToInt = (it.animatedValue as Float).roundToInt()
                                windowLayoutParams.height = roundToInt

                                try {
                                    windowManager.updateViewLayout(
                                        this@SmartNoticeWindow,
                                        windowLayoutParams
                                    )
                                } catch (_: Exception) {

                                }
                            }
                        }
                    }
                    startChargeHeight.duration = animatorDuration.value

                    val composeViewAlphaStart = ObjectAnimator.ofFloat(
                        animationContainer,
                        "alpha",
                        animationContainer.alpha,
                        1f
                    )
                    composeViewAlphaStart.duration = animatorDuration.value

                    startSet?.playTogether(
                        startChargeWidth,
                        startChargeHeight,
                        composeViewAlphaStart
                    )
                    startSet?.interpolator = OvershootInterpolator()
                    startSet?.addListener(
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                super.onAnimationEnd(animation)
                                val endChargeWidth = ObjectAnimator.ofFloat(
                                    this@SmartNoticeWindow,
                                    "width",
                                    windowLayoutParams.width.toFloat(),
                                    size.width
                                ).apply {
                                    addUpdateListener {
                                        val roundToInt =
                                            (it.animatedValue as Float).roundToInt()
                                        windowLayoutParams.width = roundToInt

                                        try {
                                            windowManager.updateViewLayout(
                                                this@SmartNoticeWindow,
                                                windowLayoutParams
                                            )
                                        } catch (_: Exception) {

                                        }
                                    }
                                }
                                endChargeWidth.duration = animatorDuration.value

                                val endChargeHeight = ObjectAnimator.ofFloat(
                                    this@SmartNoticeWindow,
                                    "height",
                                    windowLayoutParams.height.toFloat(),
                                    size.height
                                ).apply {
                                    addUpdateListener {
                                        val roundToInt =
                                            (it.animatedValue as Float).roundToInt()
                                        windowLayoutParams.height = roundToInt

                                        try {
                                            windowManager.updateViewLayout(
                                                this@SmartNoticeWindow,
                                                windowLayoutParams
                                            )
                                        } catch (_: Exception) {

                                        }
                                    }
                                }
                                endChargeHeight.duration = animatorDuration.value

                                val composeViewAlphaEnd = ObjectAnimator.ofFloat(
                                    animationContainer,
                                    "alpha",
                                    animationContainer.alpha,
                                    0f
                                )
                                composeViewAlphaEnd.duration = animatorDuration.value

                                endSet?.playTogether(
                                    endChargeWidth,
                                    endChargeHeight,
                                    composeViewAlphaEnd
                                )

                                endSet?.startDelay = animatorDelay.value
                                endSet?.interpolator = OvershootInterpolator()
                                endSet?.start()
                            }
                        }
                    )
                    startSet?.start()
                }
            }
        }
    }

    @SuppressLint("ObjectAnimatorBinding")
    fun minimize() {
        startSet?.cancel()
        startSet?.removeAllListeners()

        startSet = null

        endSet?.cancel()
        endSet?.removeAllListeners()

        endSet = AnimatorSet()

        viewCoroutine.launch {
            val size = _islandCustomSize.filter { it != Size.Zero }
                .first()

            withContext(Dispatchers.Main) {

                val endChargeWidth = ObjectAnimator.ofFloat(
                    this@SmartNoticeWindow,
                    "width",
                    windowLayoutParams.width.toFloat(),
                    size.width
                ).apply {
                    addUpdateListener {
                        val roundToInt =
                            (it.animatedValue as Float).roundToInt()
                        windowLayoutParams.width = roundToInt

                        try {
                            windowManager.updateViewLayout(
                                this@SmartNoticeWindow,
                                windowLayoutParams
                            )
                        } catch (_: Exception) {

                        }
                    }
                }
                endChargeWidth.duration = animatorDuration.value

                val endChargeHeight = ObjectAnimator.ofFloat(
                    this@SmartNoticeWindow,
                    "height",
                    windowLayoutParams.height.toFloat(),
                    size.height
                ).apply {
                    addUpdateListener {
                        val roundToInt =
                            (it.animatedValue as Float).roundToInt()
                        windowLayoutParams.height = roundToInt

                        try {
                            windowManager.updateViewLayout(
                                this@SmartNoticeWindow,
                                windowLayoutParams
                            )
                        } catch (_: Exception) {

                        }
                    }
                }
                endChargeHeight.duration = animatorDuration.value

                val composeViewAlphaEnd = ObjectAnimator.ofFloat(
                    animationContainer,
                    "alpha",
                    animationContainer.alpha,
                    0f
                )
                composeViewAlphaEnd.duration = animatorDuration.value

                endSet?.playTogether(
                    endChargeWidth,
                    endChargeHeight,
                    composeViewAlphaEnd
                )

                endSet?.interpolator = OvershootInterpolator()
                endSet?.start()
            }
        }
    }
}