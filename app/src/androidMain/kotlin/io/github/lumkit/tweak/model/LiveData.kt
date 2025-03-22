package io.github.lumkit.tweak.model

import java.util.concurrent.ConcurrentHashMap

class LiveData<T> {
    private val _observeMap = ConcurrentHashMap<Any, (T?) -> Unit>()
    var value: T? = null
        set(value) {
            field = value
            _observeMap.forEach { (_, observe) -> observe(value)  }
        }

    fun observe(observer: (T?) -> Unit) {
        _observeMap[observer::class] = observer
    }

    fun removeObserver(observer: (T?) -> Unit) {
        _observeMap.remove(observer::class)
    }

    fun clear() {
        _observeMap.clear()
    }
}