package io.github.lumkit.tweak.ui.screen.main.page

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDeepLink
import androidx.navigation.navDeepLink
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.common.BaseViewModel
import io.github.lumkit.tweak.common.shell.module.UpdateEngineClient
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.ui.screen.ScreenRoute
import io.github.lumkit.tweak.ui.screen.notice.SmartNoticeScreen
import io.github.lumkit.tweak.ui.screen.vabup.VabUpdaterScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import java.io.Serializable

@SuppressLint("StaticFieldLeak")
class FunctionPageViewModel(
    private val context: Context
) : BaseViewModel() {

    @Immutable
    data class FunPlate(
        @StringRes val title: Int,
        val modules: List<FunModule>,
    ): Serializable

    @Immutable
    data class FunModule(
        @DrawableRes val icon: Int,
        val title: String,
        val enabled: Boolean = true,
        val id: String,
        val route: String,
        @Transient val deepLinks: List<NavDeepLink> = emptyList(),
        @Transient val screen: @Composable () -> Unit
    ): Serializable

    private val _plateState = MutableStateFlow<List<FunPlate>>(emptyList())
    val plateState = _plateState.asStateFlow()

    private val _searchTextState = MutableStateFlow("")
    val searchTextState = _searchTextState.asStateFlow()

    init {
        val plates: MutableList<FunPlate> = mutableListOf()
        initSystemTools(plates)
        _plateState.value = plates
    }

    fun setSearchText(text: String) {
        _searchTextState.value = text
    }

    private fun initSystemTools(plates: MutableList<FunPlate>) {
        plates.add(
            FunPlate(
                title = R.string.text_fun_system_tools,
                modules = listOf(
                    FunModule(
                        icon = R.drawable.ic_update,
                        title = context.getString(R.string.text_vab_updater),
                        enabled = true,
                        id = ScreenRoute.VAB_UPDATE,
                        route = ScreenRoute.VAB_UPDATE,
                        screen = {
                            VabUpdaterScreen()
                        },
                        deepLinks = listOf(
                            navDeepLink {
                                uriPattern = "${Const.Navigation.DEEP_LINE}/${ScreenRoute.VAB_UPDATE}"
                            }
                        )
                    ),
                    FunModule(
                        icon = R.drawable.ic_smart_notice,
                        title = context.getString(R.string.text_smart_notice),
                        enabled = false,
                        id = ScreenRoute.SMART_NOTICE,
                        route = ScreenRoute.SMART_NOTICE,
                        screen = {
                            SmartNoticeScreen()
                        }
                    ),
                )
            )
        )
    }
}