package io.github.lumkit.tweak.ui.screen.main.page

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FunctionPageViewModel: BaseViewModel() {

    @Immutable
    data class FunPlate(
        @StringRes val title: Int,
        val modules: List<FunModule>,
    )

    @Immutable
    data class FunModule(
        val icon: @Composable () -> Unit,
        val title: @Composable () -> Unit,
        val enabled: Boolean = true,
        val id: String,
        val route: String,
    )

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
                        icon = {

                        },
                        title = {

                        },
                        enabled = true,
                        id = "",
                        route = ""
                    )
                )
            )
        )
    }
}