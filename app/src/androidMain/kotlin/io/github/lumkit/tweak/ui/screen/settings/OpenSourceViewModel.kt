package io.github.lumkit.tweak.ui.screen.settings

import androidx.compose.runtime.Immutable
import io.github.lumkit.tweak.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.Serializable

class OpenSourceViewModel: BaseViewModel() {

    @Immutable
    data class LicenseBean(
        var title: String,
        var author: String,
        var tip: String,
        var url: String
    ) : Serializable

    private val _licenseState = MutableStateFlow(emptyList<LicenseBean>())
    val licenseState = _licenseState.asStateFlow()

    init {
        _licenseState.value = listOf(
            LicenseBean(
                title = "Compose Multiplatform",
                author = "JetBrains",
                tip = "Compose Multiplatform, a modern UI framework for Kotlin that makes building performant and beautiful user interfaces easy and enjoyable.",
                url = "https://github.com/JetBrains/compose-multiplatform/",
            ),
            LicenseBean(
                title = "Ktor",
                author = "JetBrains",
                tip = "Framework for quickly creating connected applications in Kotlin with minimal effort",
                url = "https://github.com/ktorio/ktor",
            ),
            LicenseBean(
                title = "coil",
                author = "coil-kt",
                tip = "Image loading for Android and Compose Multiplatform.",
                url = "https://github.com/coil-kt/coil",
            ),
            LicenseBean(
                title = "Compose Color Picker",
                author = "mhssn95",
                tip = "A color picker for Jetpack compose \uD83C\uDFA8",
                url = "https://github.com/mhssn95/compose-color-picker",
            ),
            LicenseBean(
                title = "Scene 4",
                author = "helloklf",
                tip = "一个集高级重启、应用安装自动点击、CPU调频等多项功能于一体的工具箱。Tweak若干设计理念参照于此。",
                url = "https://github.com/helloklf/vtools",
            ),
            LicenseBean(
                title = "",
                author = "",
                tip = "",
                url = "",
            ),
            LicenseBean(
                title = "",
                author = "",
                tip = "",
                url = "",
            ),
            LicenseBean(
                title = "",
                author = "",
                tip = "",
                url = "",
            ),
        ).filter {
            it.tip.isNotBlank()
                    && it.url.isNotBlank()
                    && it.title.isNotBlank()
                    && it.author.isNotBlank()
        }.sortedBy { it.title }
    }

}