package io.github.lumkit.tweak.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lumkit.tweak.common.util.makeText
import io.github.lumkit.tweak.data.LoadState
import io.ktor.util.collections.ConcurrentMap
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseViewModel : ViewModel() {

    private val _loadStatePool = MutableStateFlow(ConcurrentMap<String, LoadState>())
    val loadState = _loadStatePool.asStateFlow()

    /**
     * 更新状态
     */
    fun updateLoadState(id: String, state: LoadState) {
        val copy = ConcurrentMap<String, LoadState>()
        copy.putAll(_loadStatePool.value)
        copy[id] = state
        _loadStatePool.value = copy
    }

    fun clearLoadState(id: String) {
        val copy = ConcurrentMap<String, LoadState>()
        copy.putAll(_loadStatePool.value)
        copy.remove(id)
        _loadStatePool.value = copy
    }

    /**
     * 启动一个用于IO操作的协程
     * @param id 协程的ID
     * @param onError Error回调
     * @param block 执行体
     */
    fun suspendLaunch(
        id: String,
        onError: BaseViewModelScope.(Throwable) -> Unit = {
            it.printStackTrace()
            fail(it.makeText())
        },
        block: suspend BaseViewModelScope.(CoroutineScope) -> Unit
    ) {
        val scope = object : BaseViewModelScope {
            override val id: String = id
        }
        viewModelScope.launch(
            CoroutineExceptionHandler { _, throwable ->
                onError(scope, throwable)
            }
        ) {
            withContext(Dispatchers.IO) {
                scope.block(this)
            }
        }
    }

    interface BaseViewModelScope {
        val id: String
    }

    fun BaseViewModelScope.updateLoadState(state: LoadState) {
        val copy = ConcurrentMap<String, LoadState>()
        copy.putAll(_loadStatePool.value)
        copy[id] = state
        _loadStatePool.value = copy
    }

    fun BaseViewModelScope.clearLoadState() {
        val copy = ConcurrentMap<String, LoadState>()
        copy.putAll(_loadStatePool.value)
        copy.remove(id)
        _loadStatePool.value = copy
    }

    fun BaseViewModelScope.loading() {
        updateLoadState(LoadState.Loading())
    }

    fun BaseViewModelScope.success(
        message: String,
    ) {
        updateLoadState(LoadState.Success(message = message))
    }

    fun BaseViewModelScope.fail(message: String) {
        updateLoadState(LoadState.Fail(message = message))
    }
}