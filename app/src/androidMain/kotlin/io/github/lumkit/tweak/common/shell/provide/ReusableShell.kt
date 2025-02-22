package io.github.lumkit.tweak.common.shell.provide

import android.util.Log
import io.github.lumkit.tweak.data.RuntimeStatus
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import java.io.BufferedReader
import java.io.BufferedWriter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * @author github@lumkit
 * 可复用的Process
 * @param user 用户身份，可以是是、su、suu
 */
class ReusableShell(
    val user: String,
    private val redirectErrorStream: Boolean = false,
    val status: RuntimeStatus,
) {
    companion object {
        private const val CHECK_ROOT_STATE =
            "if [[ \$(id -u 2>&1) == '0' ]] || [[ \$(\$UID) == '0' ]] || [[ \$(whoami 2>&1) == 'root' ]] || [[ \$(set | grep 'USER_ID=0') == 'USER_ID=0' ]]; then\n" +
                    "  echo 'success'\n" +
                    "else\n" +
                    "if [[ -d /cache ]]; then\n" +
                    "  echo 1 > /cache/tweak_root\n" +
                    "  if [[ -f /cache/tweak_root ]] && [[ \$(cat /cache/tweak_root) == '1' ]]; then\n" +
                    "    echo 'success'\n" +
                    "    rm -rf /cache/tweak_root\n" +
                    "    return\n" +
                    "  fi\n" +
                    "fi\n" +
                    "exit 1\n" +
                    "exit 1\n" +
                    "fi\n"

        private const val LOCK_TIMEOUT = 10_000
        private const val START_TAG = "|SH>>|"
        private const val END_TAG = "|<<SH|"
        private const val ECHO_START = "\necho '$START_TAG'\n"
        private const val ECHO_END = "\necho '$END_TAG'\n"
    }

    @Volatile
    private var process: Process? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private val mutex = Mutex()
    private var currentIsIdle = true

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val listenerPool = ConcurrentHashMap<String, (String) -> Unit>()

    fun setOnReadLineListener(lineListener: (String) -> Unit) {
        listenerPool["default"] = lineListener
    }

    fun addOnReadLineListener(
        name: String = UUID.randomUUID().toString(),
        lineListener: (String) -> Unit
    ) {
        listenerPool[name] = lineListener
    }

    fun removeOnReadLineListener(name: String) {
        listenerPool.remove(name)
    }

    val isIdle: Boolean
        get() {
            return currentIsIdle
        }

    fun tryExit() {
        try {
            reader?.close()
            reader = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            writer?.close()
            writer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            process?.destroy()
            process = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            listenerPool.clear()
            coroutineScope.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        currentIsIdle = true
    }

    @Volatile
    private var enterLockTime = 0L

    private suspend fun startProcess() {
        if (process != null) return
        withTimeout(10_000) {
            mutex.withLock<Unit> {
                enterLockTime = Clock.System.now().toEpochMilliseconds()
                try {
                    val process = when (status) {
                        RuntimeStatus.Normal, RuntimeStatus.Root -> {
                            RuntimeProvider.getProcess(
                                if (status == RuntimeStatus.Root) user else "sh",
                                redirectErrorStream
                            )
                        }

                        RuntimeStatus.Shizuku -> RuntimeProvider.getAdbProcess()
                    }

                    this@ReusableShell.process = process
                    this@ReusableShell.writer = process.outputStream.bufferedWriter()
                    this@ReusableShell.reader = process.inputStream.bufferedReader()

                    if (status == RuntimeStatus.Root) {
                        if (user.contains("su")) {
                            writer?.apply {
                                write(CHECK_ROOT_STATE)
                                flush()
                            }
                        }
                    }

                    coroutineScope.launch(
                        CoroutineExceptionHandler { _, throwable ->
                            Log.e("ErrorReader", throwable.message ?: "Error reading error stream")
                        }
                    ) {
                        process.errorStream.bufferedReader().use {
                            while (isActive) {
                                Log.e("ReusableShell", it.readLine() ?: break)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ReusableShell", e.message ?: "Unknown error")
                } finally {
                    enterLockTime = 0L
                }
            }
        }
    }

    suspend fun commitCmdSync(cmd: String): String = withContext(Dispatchers.IO) {
        if (mutex.isLocked && enterLockTime > 0 && Clock.System.now()
                .toEpochMilliseconds() - enterLockTime > LOCK_TIMEOUT
        ) {
            tryExit()
            Log.e(
                "commitCmdSync-Lock",
                "线程等待超时: ${System.currentTimeMillis() - enterLockTime > LOCK_TIMEOUT}ms"
            )
        }
        startProcess()
        try {
            mutex.withLock {
                currentIsIdle = false

                writer?.apply {
                    write(ECHO_START)
                    write(cmd + "\n")
                    write(ECHO_END)
                    flush()
                }

                val shellOutputCache = StringBuilder()
                var line: String?
                while (reader?.readLine().also { line = it } != null) {
                    if (line?.contains(START_TAG) == true) {
                        shellOutputCache.clear()
                    } else if (line?.contains(END_TAG) == true) {
                        break
                    } else {
                        if (line?.contains(START_TAG) == false && line?.contains(END_TAG) == false) {
                            line?.let { text ->
                                coroutineScope.launch {
                                    try {
                                        listenerPool.onEach {
                                            it.value.invoke(text)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                shellOutputCache.append(text).append("\n")
                            }
                        }
                    }
                }
                shellOutputCache.let {
                    var result = it.toString()
                    if (result.isNotEmpty()) {
                        result = result.substring(0, result.length - 1)
                    }
                    result
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("commitCmdSync-Lock", e.toString())
            "error"
        } finally {
            enterLockTime = 0L
            currentIsIdle = true
        }
    }

    suspend fun checkRoot(): Boolean {
        val r = commitCmdSync(CHECK_ROOT_STATE).lowercase()
        return if (r == "error" || r.contains("permission denied") || r.contains("not allowed") || r == "not found") {
            if (user.contains("su")) {
                tryExit()
            }
            false
        } else if (r.contains("success")) {
            true
        } else {
            if (user.contains("su")) {
                tryExit()
            }
            false
        }
    }
}