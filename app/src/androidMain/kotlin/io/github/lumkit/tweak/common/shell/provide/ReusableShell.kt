package io.github.lumkit.tweak.common.shell.provide

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import java.io.BufferedReader
import java.io.BufferedWriter

/**
 * @author github@lumkit
 * 可复用的Process
 * @param user 用户身份，可以是是、su、suu
 */
class ReusableShell(
    val user: String,
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

    val isIdle: Boolean
        get() {
            return currentIsIdle
        }

    fun tryExit() {
        try {
            writer?.close()
            writer = null
        }catch (_: Exception) { }
        try {
            reader?.close()
            reader = null
        }catch (_: Exception) { }
        try {
            process?.destroy()
            process = null
        }catch (_: Exception) { }
        currentIsIdle = true
    }

    @Volatile
    private var enterLockTime = 0L

    private suspend fun startProcess() {
        if (process != null) return
        withTimeout(10_000) {
            mutex.withLock {
                enterLockTime = Clock.System.now().toEpochMilliseconds()
                try {
                    val process = RuntimeProvider.getProcess(user)
                    this@ReusableShell.process = process
                    this@ReusableShell.writer = process.outputStream.bufferedWriter()
                    this@ReusableShell.reader = process.inputStream.bufferedReader()

                    if (user.contains("su")) {
                        writer?.apply {
                            write(CHECK_ROOT_STATE)
                            flush()
                        }
                    }

                    CoroutineScope(Dispatchers.IO).launch(
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
                }catch (e: Exception) {
                    Log.e("getRuntimeShell", e.message ?: "Unknown error")
                } finally {
                    enterLockTime = 0L
                }
            }
        }
    }

    suspend fun commitCmdSync(cmd: String): String = withContext(Dispatchers.IO) {
        if (mutex.isLocked && enterLockTime > 0 && Clock.System.now().toEpochMilliseconds() - enterLockTime > LOCK_TIMEOUT) {
            tryExit()
            Log.e("commitCmdSync-Lock", "线程等待超时: ${System.currentTimeMillis() - enterLockTime > LOCK_TIMEOUT}ms")
        }
        startProcess()
        try {
            mutex.lock()
            currentIsIdle = false

            writer?.apply {
                withContext(Dispatchers.IO) {
                    write(ECHO_START)
                    write(cmd)
                    write(ECHO_END)
                    flush()
                }
            }

            val shellOutputCache = StringBuilder()
            var line: String?
            while (reader?.readLine().also { line = it } != null) {
                if (line?.contains(START_TAG) == true) {
                    shellOutputCache.clear()
                } else if (line?.contains(END_TAG) == true) {
                    shellOutputCache.append(line?.substring(0, line?.indexOf(END_TAG) ?: 0))
                    break
                } else {
                    if (line?.contains(START_TAG) == false && line?.contains(END_TAG) == false) {
                        shellOutputCache.append(line).append("\n")
                    }
                }
            }
            shellOutputCache.toString()
        } catch (e: Exception) {
            Log.e("commitCmdSync-Lock", e.toString())
            "error"
        } finally {
            enterLockTime = 0L
            mutex.unlock()
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