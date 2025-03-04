package io.github.lumkit.tweak.ui.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.RelativeLayout
import androidx.cardview.widget.CardView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.lumkit.tweak.Main
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.TweakApplication.Companion.density
import io.github.lumkit.tweak.common.util.AppInfoLoader
import io.github.lumkit.tweak.common.util.formatUptimeMinute
import io.github.lumkit.tweak.common.util.getStatusBarHeight
import io.github.lumkit.tweak.data.CutoutRect
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.services.SmartNoticeService
import io.github.lumkit.tweak.services.media.MediaCallback
import io.github.lumkit.tweak.services.media.MediaStruct
import io.github.lumkit.tweak.ui.local.json
import io.github.lumkit.tweak.ui.theme.AppTheme
import io.github.lumkit.tweak.ui.token.SmartNoticeCapsuleDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@SuppressLint("ViewConstructor")
class SmartNoticeWindow(
    private val service: SmartNoticeService,
    private val windowManager: WindowManager,
    private val windowLayoutParams: WindowManager.LayoutParams,
) : CardView(service) {

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

    private val viewCoroutine = CoroutineScope(Dispatchers.IO)
    val mediaComponentExpanded = MutableStateFlow(false)
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
                _islandDefaultSize.filter { it.width != 0f && it.height != 0f }.collect {
                    if (_islandCustomSize.value == Size.Zero) {
                        _islandCustomSize.value = it
                    }
                }
            }

            launch {
                _islandCustomSize.filter { it.width != 0f && it.height != 0f }.collect {
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

            launch {
                service.topMediaCallback.collect {
                    val musicObserve = TweakApplication.shared.getBoolean(
                        Const.SmartNotice.Observe.SMART_NOTICE_OBSERVE_MUSIC,
                        true
                    )
                    withContext(Dispatchers.Main) {
                        if (it != null) {
                            if (musicObserve) {
                                showMedia()
                            } else {
                                hideMedia()
                            }
                        } else {
                            hideMedia()
                        }
                    }
                }
            }
        }
    }

    fun release() {
        mediaComponentExpanded.value = false
        viewCoroutine.cancel()
    }

    var isShow = false
    private var widthAnimator: ValueAnimator? = null
    private var heightAnimator: ValueAnimator? = null
    private var alphaAnimator: ObjectAnimator? = null
    private var alphaAnimatorComposeView: ObjectAnimator? = null
    private var offsetXAnimator: ValueAnimator? = null
    private var offsetYAnimator: ValueAnimator? = null
    private var radiusAnimator: ObjectAnimator? = null

    fun show() {
        isShow = true
        mediaComponentExpanded.value = false

        if (widthAnimator != null && widthAnimator?.isRunning == true) {
            widthAnimator?.cancel()
        }
        if (heightAnimator != null && heightAnimator?.isRunning == true) {
            heightAnimator?.cancel()
        }
        if (alphaAnimator != null && alphaAnimator?.isRunning == true) {
            alphaAnimator?.cancel()
        }

        val musicObserve = TweakApplication.shared.getBoolean(
            Const.SmartNotice.Observe.SMART_NOTICE_OBSERVE_MUSIC,
            true
        )
        if (service.topMediaCallback.value != null && musicObserve) {
            showMedia()
            return
        }

        with(density) {
            val size = _islandCustomSize.value

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

            widthAnimator = ValueAnimator.ofFloat(
                windowLayoutParams.width.toFloat().let { if (it < 0) 0f else it },
                size.width
            ).apply {
                duration = animatorDuration.value
                addUpdateListener {
                    windowLayoutParams.width = (it.animatedValue as Float).roundToInt()
                    try {
                        windowManager.updateViewLayout(
                            this@SmartNoticeWindow,
                            windowLayoutParams
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            heightAnimator = ValueAnimator.ofFloat(
                windowLayoutParams.height.toFloat().let { if (it < 0) 0f else it },
                size.height
            ).apply {
                duration = animatorDuration.value
                addUpdateListener {
                    windowLayoutParams.height = (it.animatedValue as Float).roundToInt()
                    try {
                        windowManager.updateViewLayout(
                            this@SmartNoticeWindow,
                            windowLayoutParams
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            alphaAnimator = ObjectAnimator.ofFloat(
                this@SmartNoticeWindow,
                "alpha",
                this@SmartNoticeWindow.alpha,
                1f,
            ).apply {
                duration = animatorDuration.value
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            if (service.topMediaCallback.value != null && musicObserve) {
                                showMedia()
                            }
                        }
                    }
                )
            }

            widthAnimator?.start()
            heightAnimator?.start()
            alphaAnimator?.start()
        }
    }

    fun hide() {
        isShow = false
        mediaComponentExpanded.value = false

        if (widthAnimator != null && widthAnimator?.isRunning == true) {
            widthAnimator?.cancel()
        }
        if (heightAnimator != null && heightAnimator?.isRunning == true) {
            heightAnimator?.cancel()
        }
        if (alphaAnimator != null && alphaAnimator?.isRunning == true) {
            alphaAnimator?.cancel()
        }

        widthAnimator = ValueAnimator.ofFloat(
            windowLayoutParams.width.toFloat().let { if (it < 0) 0f else it },
            0f
        ).apply {
            this.duration = animatorDuration.value
            addUpdateListener {
                windowLayoutParams.width = (it.animatedValue as Float).roundToInt()
                try {
                    windowManager.updateViewLayout(this@SmartNoticeWindow, windowLayoutParams)
                } catch (_: Exception) {
                }
            }
        }
        heightAnimator = ValueAnimator.ofFloat(
            windowLayoutParams.height.toFloat().let { if (it < 0) 0f else it },
            0f
        ).apply {
            this.duration = animatorDuration.value
            addUpdateListener {
                windowLayoutParams.height = (it.animatedValue as Float).roundToInt()
                try {
                    windowManager.updateViewLayout(this@SmartNoticeWindow, windowLayoutParams)
                } catch (_: Exception) {
                }
            }
        }

        alphaAnimator = ObjectAnimator.ofFloat(
            this@SmartNoticeWindow,
            "alpha",
            this.alpha,
            0f,
        ).apply {
            this.duration = animatorDuration.value
        }

        widthAnimator?.start()
        heightAnimator?.start()
        alphaAnimator?.start()
    }

    abstract class SmartNoticeWindowScope {
        abstract val cutoutRect: Rect?
        abstract val startSize: Size
        abstract val density: Density
    }

    fun toast(
        componentSize: SmartNoticeWindowScope.() -> DpSize,
        content: @Composable Density.(SmartNoticeWindow) -> Unit,
    ) {
        if (widthAnimator != null && widthAnimator?.isRunning == true) {
            widthAnimator?.cancel()
        }
        if (heightAnimator != null && heightAnimator?.isRunning == true) {
            heightAnimator?.cancel()
        }

        if (alphaAnimatorComposeView != null && alphaAnimatorComposeView?.isRunning == true) {
            alphaAnimatorComposeView?.cancel()
        }

        // 如果媒体组件是展开状态，则不执行提示动画
        if (mediaComponentExpanded.value) {
            return
        }

        with(density) {
            val size = _islandCustomSize.value

            val rect = if (_cutoutRectListState.value.isEmpty()) {
                null
            } else {
                val first = _cutoutRectListState.value.first()
                val last = _cutoutRectListState.value.last()
                Rect(
                    first.left,
                    first.top,
                    last.right,
                    last.bottom
                )
            }

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

            widthAnimator = ValueAnimator.ofFloat(
                windowLayoutParams.width.toFloat(),
                targetSize.width,
            ).apply {
                this.duration = animatorDuration.value
                this.interpolator = OvershootInterpolator()
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

            heightAnimator = ValueAnimator.ofFloat(
                windowLayoutParams.height.toFloat(),
                targetSize.height,
            ).apply {
                this.duration = animatorDuration.value
                this.interpolator = OvershootInterpolator()
                addUpdateListener {
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

            alphaAnimatorComposeView = ObjectAnimator.ofFloat(
                animationContainer,
                "alpha",
                animationContainer.alpha,
                0f,
                1f
            ).apply {
                this.duration = animatorDuration.value
                var set = false
                addUpdateListener {
                    val alpha = (it.animatedValue) as Float
                    if (alpha <= .05f && !set) {
                        set = true
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
                    }
                }
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            if (!set) {
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
                            }
                        }
                    }
                )
            }

            widthAnimator?.start()
            heightAnimator?.start()

            heightAnimator?.addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        minimize(true)
                    }
                }
            )
            alphaAnimatorComposeView?.start()
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    fun minimize(delay: Boolean = false) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(
            {
                if (mediaComponentExpanded.value) {
                    return@postDelayed
                }
                val musicObserve = TweakApplication.shared.getBoolean(
                    Const.SmartNotice.Observe.SMART_NOTICE_OBSERVE_MUSIC,
                    true
                )
                if (service.topMediaCallback.value != null && musicObserve) {
                    showMedia()
                    return@postDelayed
                }

                val size = _islandCustomSize.value

                if (widthAnimator != null && widthAnimator?.isRunning == true) {
                    widthAnimator?.cancel()
                }
                if (heightAnimator != null && heightAnimator?.isRunning == true) {
                    heightAnimator?.cancel()
                }
                if (alphaAnimatorComposeView != null && alphaAnimatorComposeView?.isRunning == true) {
                    alphaAnimatorComposeView?.cancel()
                }

                widthAnimator = ValueAnimator.ofFloat(
                    windowLayoutParams.width.toFloat(),
                    size.width
                ).apply {
                    this.interpolator = OvershootInterpolator()
                    this.duration = animatorDuration.value
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

                heightAnimator = ValueAnimator.ofFloat(
                    windowLayoutParams.height.toFloat(),
                    size.height
                ).apply {
                    this.interpolator = OvershootInterpolator()
                    this.duration = animatorDuration.value
                    addUpdateListener {
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

                alphaAnimatorComposeView = ObjectAnimator.ofFloat(
                    animationContainer,
                    "alpha",
                    animationContainer.alpha,
                    0f
                ).apply {
                    this.duration = animatorDuration.value
                    addListener(
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                super.onAnimationEnd(animation)
                                animationContainer.setContent { }
                            }
                        }
                    )
                }


                widthAnimator?.start()
                heightAnimator?.start()
                heightAnimator?.addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            if (service.topMediaCallback.value != null && musicObserve) {
                                showMedia()
                            }
                        }
                    }
                )
                alphaAnimatorComposeView?.start()
            },
            if (delay) {
                animatorDelay.value
            } else {
                0L
            }
        )
    }

    fun showMedia(delay: Boolean = false, interpolator: Boolean = true) {
        handler.removeCallbacksAndMessages(null)

        handler.postDelayed(
            {
                mediaComponentExpanded.value = false

                if (widthAnimator != null && widthAnimator?.isRunning == true) {
                    widthAnimator?.cancel()
                }
                if (heightAnimator != null && heightAnimator?.isRunning == true) {
                    heightAnimator?.cancel()
                }
                if (alphaAnimatorComposeView != null && alphaAnimatorComposeView?.isRunning == true) {
                    alphaAnimatorComposeView?.cancel()
                }
                if (radiusAnimator != null && radiusAnimator?.isRunning == true) {
                    radiusAnimator?.cancel()
                }
                if (offsetYAnimator != null && offsetYAnimator?.isRunning == true) {
                    offsetYAnimator?.cancel()
                }

                if (service.topMediaCallback.value == null) {
                    return@postDelayed
                }
                with(density) {
                    widthAnimator = ValueAnimator.ofFloat(
                        windowLayoutParams.width.toFloat(),
                        SmartNoticeCapsuleDefault.Media.Width.toPx()
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
                        if (interpolator) {
                            this.interpolator = OvershootInterpolator()
                        }
                        this.duration = animatorDuration.value
                    }

                    heightAnimator = ValueAnimator.ofFloat(
                        windowLayoutParams.height.toFloat(),
                        SmartNoticeCapsuleDefault.Media.Height.toPx()
                    ).apply {
                        addUpdateListener {
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
                        if (interpolator) {
                            this.interpolator = OvershootInterpolator()
                        }
                        this.duration = animatorDuration.value
                    }

                    alphaAnimatorComposeView = ObjectAnimator.ofFloat(
                        animationContainer,
                        "alpha",
                        animationContainer.alpha,
                        0f,
                        1f,
                    ).apply {
                        this.duration = animatorDuration.value
                        var set = false
                        addUpdateListener {
                            val alpha = (it.animatedValue) as Float
                            if (alpha <= .05f && !set) {
                                set = true
                                animationContainer.setContent {
                                    Main {
                                        AppTheme {
                                            Surface(
                                                color = Color.Black,
                                                contentColor = MaterialTheme.colorScheme.surface,
                                            ) {
                                                MediaComponent()
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        addListener(
                            object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    super.onAnimationEnd(animation)
                                    if (!set) {
                                        animationContainer.setContent {
                                            Main {
                                                AppTheme {
                                                    Surface(
                                                        color = Color.Black,
                                                        contentColor = MaterialTheme.colorScheme.surface,
                                                    ) {
                                                        MediaComponent()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }

                    radiusAnimator = ObjectAnimator.ofFloat(
                        this@SmartNoticeWindow,
                        "radius",
                        this@SmartNoticeWindow.radius,
                        SmartNoticeCapsuleDefault.Media.Radius.toPx()
                    ).apply {
                        this.duration = animatorDuration.value
                    }

                    val localOffsetY = TweakApplication.shared.getFloat(
                        Const.SmartNotice.SMART_NOTICE_OFFSET_Y,
                        -1f
                    )

                    offsetYAnimator = ValueAnimator.ofFloat(
                        windowLayoutParams.y.toFloat(),
                        if (localOffsetY == -1f) {
                            getStatusBarHeight() - _islandCustomSize.value.height.roundToInt() - SmartNoticeCapsuleDefault.CapsulePaddingTop.toPx() / 2f
                        } else {
                            localOffsetY.dp.toPx()
                        }
                    ).apply {
                        if (interpolator) {
                            this.interpolator = OvershootInterpolator()
                        }
                        this.duration = animatorDuration.value
                        addUpdateListener {
                            val roundToInt = (it.animatedValue as Float).roundToInt()
                            windowLayoutParams.y = roundToInt
                            try {
                                windowManager.updateViewLayout(
                                    this@SmartNoticeWindow,
                                    windowLayoutParams
                                )
                            } catch (_: Exception) {
                            }
                        }
                    }

                    widthAnimator?.start()
                    heightAnimator?.start()
                    alphaAnimatorComposeView?.start()
                    radiusAnimator?.start()
                    offsetYAnimator?.start()
                }
            },
            if (delay) {
                animatorDelay.value
            } else {
                0L
            }
        )
    }

    fun hideMedia() {
        mediaComponentExpanded.value = false

        if (widthAnimator != null && widthAnimator?.isRunning == true) {
            widthAnimator?.cancel()
        }
        if (heightAnimator != null && heightAnimator?.isRunning == true) {
            heightAnimator?.cancel()
        }
        if (alphaAnimatorComposeView != null && alphaAnimatorComposeView?.isRunning == true) {
            alphaAnimatorComposeView?.cancel()
        }
        if (radiusAnimator != null && radiusAnimator?.isRunning == true) {
            radiusAnimator?.cancel()
        }
        if (offsetYAnimator != null && offsetYAnimator?.isRunning == true) {
            offsetYAnimator?.cancel()
        }

        val size = _islandCustomSize.value

        with(density) {
            widthAnimator = ValueAnimator.ofFloat(
                windowLayoutParams.width.toFloat(),
                size.width
            ).apply {
                this.interpolator = OvershootInterpolator()
                this.duration = animatorDuration.value
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

            heightAnimator = ValueAnimator.ofFloat(
                windowLayoutParams.height.toFloat(),
                size.height
            ).apply {
                this.interpolator = OvershootInterpolator()
                this.duration = animatorDuration.value
                addUpdateListener {
                    val roundToInt = (it.animatedValue as Float).roundToInt()
                    windowLayoutParams.height = roundToInt
                    try {
                        windowManager.updateViewLayout(
                            this@SmartNoticeWindow,
                            windowLayoutParams
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            alphaAnimatorComposeView = ObjectAnimator.ofFloat(
                animationContainer,
                "alpha",
                animationContainer.alpha,
                0f
            ).apply {
                this.duration = animatorDuration.value
            }

            val localOffsetY = TweakApplication.shared.getFloat(
                Const.SmartNotice.SMART_NOTICE_OFFSET_Y,
                -1f
            )

            offsetYAnimator = ValueAnimator.ofFloat(
                windowLayoutParams.y.toFloat(),
                if (localOffsetY == -1f) {
                    getStatusBarHeight() - _islandCustomSize.value.height.roundToInt() - SmartNoticeCapsuleDefault.CapsulePaddingTop.toPx() / 2f
                } else {
                    localOffsetY.dp.toPx()
                }
            ).apply {
                this.interpolator = OvershootInterpolator()
                this.duration = animatorDuration.value
                addUpdateListener {
                    val roundToInt = (it.animatedValue as Float).roundToInt()
                    windowLayoutParams.y = roundToInt
                    try {
                        windowManager.updateViewLayout(
                            this@SmartNoticeWindow,
                            windowLayoutParams
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            radiusAnimator = ObjectAnimator.ofFloat(
                this@SmartNoticeWindow,
                "radius",
                this@SmartNoticeWindow.radius,
                TweakApplication.shared.getFloat(
                    Const.SmartNotice.SMART_NOTICE_CUTOUT_RADIUS,
                    min(size.width, size.height) / 2f
                ).dp.toPx()
            ).apply {
                this.duration = animatorDuration.value
            }

            widthAnimator?.start()
            heightAnimator?.start()
            alphaAnimatorComposeView?.start()
            offsetYAnimator?.start()
            radiusAnimator?.start()
        }
    }

    private fun mediaExpanded() {
        mediaComponentExpanded.value = !mediaComponentExpanded.value
        if (mediaComponentExpanded.value) {
            with(density) {
                if (widthAnimator != null && widthAnimator?.isRunning == true) {
                    widthAnimator?.cancel()
                }
                if (heightAnimator != null && heightAnimator?.isRunning == true) {
                    heightAnimator?.cancel()
                }
                if (radiusAnimator != null && radiusAnimator?.isRunning == true) {
                    radiusAnimator?.cancel()
                }
                if (offsetYAnimator != null && offsetYAnimator?.isRunning == true) {
                    offsetYAnimator?.cancel()
                }

                widthAnimator = ValueAnimator.ofFloat(
                    windowLayoutParams.width.toFloat(),
                    SmartNoticeCapsuleDefault.Media.ExpandedWidth.toPx()
                ).apply {
                    this.interpolator = OvershootInterpolator()
                    this.duration = animatorDuration.value
                    addUpdateListener {
                        val roundToInt = (it.animatedValue as Float).roundToInt()
                        windowLayoutParams.width = roundToInt
                        try {
                            windowManager.updateViewLayout(
                                this@SmartNoticeWindow,
                                windowLayoutParams
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                heightAnimator = ValueAnimator.ofFloat(
                    windowLayoutParams.height.toFloat(),
                    SmartNoticeCapsuleDefault.Media.ExpandedHeight.toPx()
                ).apply {
                    this.interpolator = OvershootInterpolator()
                    this.duration = animatorDuration.value
                    addUpdateListener {
                        val roundToInt = (it.animatedValue as Float).roundToInt()
                        windowLayoutParams.height = roundToInt
                        try {
                            windowManager.updateViewLayout(
                                this@SmartNoticeWindow,
                                windowLayoutParams
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                radiusAnimator = ObjectAnimator.ofFloat(
                    this@SmartNoticeWindow,
                    "radius",
                    this@SmartNoticeWindow.radius,
                    SmartNoticeCapsuleDefault.Media.ExpandedRadius.toPx()
                ).apply {
                    this.duration = animatorDuration.value
                }

                offsetYAnimator = ValueAnimator.ofFloat(
                    windowLayoutParams.y.toFloat(),
                    getStatusBarHeight().toFloat()
                ).apply {
                    this.interpolator = OvershootInterpolator()
                    this.duration = animatorDuration.value
                    addUpdateListener {
                        val roundToInt =
                            (it.animatedValue as Float).roundToInt()
                        windowLayoutParams.y = roundToInt
                        try {
                            windowManager.updateViewLayout(
                                this@SmartNoticeWindow,
                                windowLayoutParams
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                widthAnimator?.start()
                heightAnimator?.start()
                radiusAnimator?.start()
                offsetYAnimator?.start()
            }
        } else {
            showMedia(delay = false)
        }
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    private fun Density.MediaComponent() {
        val topCallback by service.topMediaCallback.collectAsStateWithLifecycle()
        topCallback?.let { callback ->
            val expanded by mediaComponentExpanded.collectAsStateWithLifecycle()
            val mediaStruct = callback.mediaStruct

            var cover by remember { mutableStateOf<Bitmap?>(null) }

            LaunchedEffect(mediaStruct) {
                mediaStruct.cover?.let {
                    if (cover != it) {
                        cover = it
                    }
                }
            }

            SharedTransitionLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        mediaExpanded()
                    }
            ) {
                SharedBox(
                    expanded,
                    mediaStruct,
                    this,
                    cover,
                    callback,
                )
            }
        }
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    private fun Density.SharedBox(
        expanded: Boolean,
        mediaStruct: MediaStruct,
        sharedTransitionScope: SharedTransitionScope,
        cover: Bitmap?,
        callback: MediaCallback
    ) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = {
                fadeIn(
                    tween(
                        durationMillis = animatorDuration.value.toInt(),
                    )
                ).togetherWith(
                    fadeOut(
                        tween(
                            durationMillis = animatorDuration.value.toInt(),
                        )
                    )
                )
            }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                if (!it) {
                    MinimizeBox(
                        sharedTransitionScope,
                        this@AnimatedContent,
                        mediaStruct,
                        cover,
                        callback
                    )
                } else {
                    ExpandedBox(
                        sharedTransitionScope,
                        this@AnimatedContent,
                        mediaStruct,
                        cover,
                        callback
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    private fun Density.MinimizeBox(
        sharedTransitionScope: SharedTransitionScope,
        animatedContentScope: AnimatedContentScope,
        mediaStruct: MediaStruct,
        cover: Bitmap?,
        callback: MediaCallback
    ) {
        val appInfoLoader = remember { AppInfoLoader(context, 100) }
        var icon by remember { mutableStateOf<Bitmap?>(null) }

        LaunchedEffect(mediaStruct) {
            icon = appInfoLoader.loadIcon(mediaStruct.packageName)
                .await()?.toBitmap()
        }

        val rotation = remember { Animatable(0f) }

        LaunchedEffect(mediaStruct) {
            snapshotFlow {
                mediaStruct.playbackState.state
            }.onEach { state ->
                val isPlaying = state == PlaybackState.STATE_PLAYING
                if (isPlaying) {
                    while (isActive) {
                        rotation.animateTo(
                            targetValue = rotation.value + 360f,
                            animationSpec = tween(
                                durationMillis = 12_000,
                                easing = LinearEasing
                            )
                        )
                    }
                } else {
                    rotation.stop()
                }
            }.launchIn(this)
        }

        var size by remember { mutableStateOf(IntSize.Zero) }

        with(sharedTransitionScope) {
            Row(
                modifier = Modifier
                    .size(
                        SmartNoticeCapsuleDefault.Media.Width,
                        SmartNoticeCapsuleDefault.Media.Height
                    )
                    .onSizeChanged {
                        size = it
                    }
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState("row"),
                        animatedVisibilityScope = animatedContentScope,
                        resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(
                            ContentScale.FillBounds, Alignment.Center,
                        ),
                        enter = fadeIn(
                            tween(
                                durationMillis = animatorDuration.collectAsStateWithLifecycle().value.toInt()
                            )
                        ),
                        exit = fadeOut(
                            tween(
                                durationMillis = animatorDuration.collectAsStateWithLifecycle().value.toInt()
                            )
                        )
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(size.height.toDp() - 8.dp)
                        .rotate(rotation.value)
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState("cover"),
                            animatedVisibilityScope = animatedContentScope,
                            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(
                                ContentScale.FillBounds, Alignment.Center,
                            ),
                            enter = fadeIn(
                                tween(
                                    durationMillis = animatorDuration.collectAsStateWithLifecycle().value.toInt()
                                )
                            ),
                            exit = fadeOut(
                                tween(
                                    durationMillis = animatorDuration.collectAsStateWithLifecycle().value.toInt()
                                )
                            )
                        ),
                    shape = CircleShape,
                    border = BorderStroke(
                        .5.dp,
                        Color.White
                    )
                ) {
                    AsyncImage(
                        modifier = Modifier.fillMaxSize(),
                        model = cover,
                        contentDescription = null,
                    )
                }

                OutlinedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(size.height.toDp() - 8.dp)
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState("logo"),
                            animatedVisibilityScope = animatedContentScope,
                            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(
                                ContentScale.FillBounds, Alignment.Center,
                            ),
                            enter = fadeIn(
                                tween(
                                    durationMillis = animatorDuration.collectAsStateWithLifecycle().value.toInt()
                                )
                            ),
                            exit = fadeOut(
                                tween(
                                    durationMillis = animatorDuration.collectAsStateWithLifecycle().value.toInt()
                                )
                            )
                        )
                        .clickable(
                            indication = null,
                            interactionSource = null
                        ) {
                            callback.mediaController.sessionActivity?.send(0)
                        },
                    shape = CircleShape,
                    border = BorderStroke(
                        .5.dp,
                        Color.White
                    )
                ) {
                    AsyncImage(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.1f),
                        model = icon,
                        contentDescription = null,
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    private fun ExpandedBox(
        sharedTransitionScope: SharedTransitionScope,
        animatedContentScope: AnimatedContentScope,
        mediaStruct: MediaStruct,
        cover: Bitmap?,
        callback: MediaCallback
    ) {
        val playAnimate = remember { Animatable(0f) }
        var elapsed by rememberSaveable { mutableFloatStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }
        var minimize by rememberSaveable { mutableIntStateOf(10) }
        var timerJob by remember { mutableStateOf<Job?>(null) }

        LaunchedEffect(mediaStruct) {
            snapshotFlow {
                mediaStruct.playbackState.state
            }.distinctUntilChanged()
                .onEach { state ->
                    val isPlay = state == PlaybackState.STATE_PLAYING
                    val current =
                        (callback.mediaController.playbackState?.position
                            ?: 0L).toFloat()
                    playAnimate.snapTo(current)
                    if (isPlay) {
                        minimize = 10
                        timerJob?.cancel()
                        if (!isDragging) {

                            playAnimate.animateTo(
                                targetValue = mediaStruct.duration.toFloat(),
                                animationSpec = tween(
                                    durationMillis = mediaStruct.duration.toInt() - current.roundToInt(),
                                    easing = LinearEasing
                                ),
                            )
                        }
                    } else {
                        playAnimate.stop()

                        // 暂停后10秒最小化
                        timerJob = launch {
                            while (isActive && minimize.let { minimize--; minimize > 0 }) {
                                delay(1000)
                            }
                            if (!mediaStruct.isPlaying()) {
                                mediaExpanded()
                            }
                        }
                    }
                }.launchIn(this)
        }

        with(sharedTransitionScope) {
            Column(
                modifier = Modifier
                    .size(
                        SmartNoticeCapsuleDefault.Media.ExpandedWidth,
                        SmartNoticeCapsuleDefault.Media.ExpandedHeight,
                    )
                    .padding(16.dp)
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState("row"),
                        animatedVisibilityScope = animatedContentScope,
                        resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(
                            ContentScale.FillBounds, Alignment.Center,
                        ),
                        enter = fadeIn(
                            tween(
                                durationMillis = animatorDuration.collectAsStateWithLifecycle().value.toInt()
                            )
                        ),
                        exit = fadeOut(
                            tween(
                                durationMillis = animatorDuration.collectAsStateWithLifecycle().value.toInt()
                            )
                        )
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedCard(
                        modifier = Modifier
                            .size(50.dp)
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState("cover"),
                                animatedVisibilityScope = animatedContentScope,
                                resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(
                                    ContentScale.FillBounds, Alignment.Center,
                                ),
                                enter = fadeIn(
                                    tween(
                                        durationMillis = animatorDuration.collectAsStateWithLifecycle().value.toInt()
                                    )
                                ),
                                exit = fadeOut(
                                    tween(
                                        durationMillis = animatorDuration.collectAsStateWithLifecycle().value.toInt()
                                    )
                                )
                            )
                            .clickable(
                                indication = null,
                                interactionSource = null
                            ) {
                                callback.mediaController.sessionActivity?.send(0)
                                mediaExpanded()
                            },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            .5.dp,
                            color = Color.White
                        )
                    ) {
                        AsyncImage(
                            modifier = Modifier.fillMaxSize(),
                            model = cover,
                            contentDescription = null,
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clickable(
                                indication = null,
                                interactionSource = null
                            ) {
                                callback.mediaController.sessionActivity?.send(0)
                                mediaExpanded()
                            }
                    ) {
                        Text(
                            text = mediaStruct.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(
                            modifier = Modifier.height(1.dp)
                        )
                        Text(
                            text = mediaStruct.artist,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = .7f),
                            softWrap = false,
                        )
                    }

                    Icon(
                        painter = painterResource(R.drawable.ic_up),
                        contentDescription = null,
                        modifier = Modifier 
                            .size(14.dp)
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState("logo"),
                                animatedVisibilityScope = animatedContentScope,
                                resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(
                                    ContentScale.FillBounds, Alignment.Center,
                                ),
                                enter = fadeIn(
                                    tween(
                                        durationMillis = animatorDuration.collectAsStateWithLifecycle().value.toInt()
                                    )
                                ),
                                exit = fadeOut(
                                    tween(
                                        durationMillis = animatorDuration.collectAsStateWithLifecycle().value.toInt()
                                    )
                                )
                            ),
                        tint = Color.White
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_previous),
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(
                                indication = null,
                                interactionSource = null,
                            ) {
                                callback.mediaController.transportControls.skipToPrevious()
                            },
                        tint = Color.White
                    )

                    AnimatedContent(
                        mediaStruct.isPlaying()
                    ) {
                        if (it) {
                            Icon(
                                painter = painterResource(R.drawable.ic_play_pause),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable(
                                        indication = null,
                                        interactionSource = null,
                                    ) {
                                        if (mediaStruct.isPlaying()) {
                                            callback.mediaController.transportControls.pause()
                                        } else {
                                            callback.mediaController.transportControls.play()
                                        }
                                    },
                                tint = Color.White
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_play_start),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable(
                                        indication = null,
                                        interactionSource = null,
                                    ) {
                                        if (mediaStruct.isPlaying()) {
                                            callback.mediaController.transportControls.pause()
                                        } else {
                                            callback.mediaController.transportControls.play()
                                        }
                                    },
                                tint = Color.White
                            )
                        }
                    }

                    Icon(
                        painter = painterResource(R.drawable.ic_play_next),
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(
                                indication = null,
                                interactionSource = null,
                            ) {
                                callback.mediaController.transportControls.skipToNext()
                            },
                        tint = Color.White
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        modifier = Modifier.width(36.dp),
                        text = playAnimate.value.roundToLong().formatUptimeMinute(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = .7f),
                        softWrap = false,
                        overflow = TextOverflow.StartEllipsis
                    )

                    Slider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        value = if (isDragging) elapsed else playAnimate.value,
                        onValueChange = {
                            elapsed = it
                            isDragging = true
                        },
                        onValueChangeFinished = {
                            isDragging = false
                            callback.mediaController.transportControls.seekTo(
                                elapsed.roundToLong()
                            )
                        },
                        valueRange = 0f..mediaStruct.duration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFDFDFD),
                            activeTrackColor = Color(0xFFFDFDFD),
                            inactiveTickColor = Color(0xFFFDFDFD)
                                .copy(alpha = .24f)
                        )
                    )

                    Text(
                        modifier = Modifier.width(36.dp),
                        text = mediaStruct.duration.formatUptimeMinute(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = .7f),
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}