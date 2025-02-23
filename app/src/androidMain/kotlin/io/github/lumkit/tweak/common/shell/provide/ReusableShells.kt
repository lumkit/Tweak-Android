package io.github.lumkit.tweak.common.shell.provide

import androidx.core.content.edit
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.data.RuntimeStatus
import io.github.lumkit.tweak.model.Const
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object ReusableShells {
    private val shells = ConcurrentHashMap<String, ReusableShell>()

    private const val MAX_DEFAULT_PRECESS_SIZE = 8

    @Synchronized
    fun getInstance(
        key: String,
        user: String = rootUser,
        redirectErrorStream: Boolean = false,
        status: RuntimeStatus
    ): ReusableShell {
        val shell = ReusableShell(user, redirectErrorStream, status)
        if (!shells.containsKey(key)) {
            shells[key] = shell
        }
        return shell
    }

    fun destroyInstance(key: String) {
        if (!shells.containsKey(key)) {
            return
        } else {
            shells[key]?.tryExit()
            shells.remove(key)
        }
    }

    fun changeUserIdAtAll(id: String) {
        TweakApplication.shared.edit { putString(Const.APP_SHELL_ROOT_USER, id) }
        destroyAll()
    }

    fun destroyAll() {
        shells.onEach {
            // 跳过更新引擎进程
            if (it.key != "update_engine_client")
                it.value.tryExit()
        }
        shells.clear()
    }

    private val rootUser: String
        get() = when (TweakApplication.runtimeStatus) {
            RuntimeStatus.Normal -> "sh"
            RuntimeStatus.Shizuku -> "sh"
            RuntimeStatus.Root -> TweakApplication.shared.getString(Const.APP_SHELL_ROOT_USER, null)
                ?: "su"
        }

    private val defaultReusableShell: ReusableShell
        get() = getDefault("defaultReusableShell")

    private fun getDefault(key: String): ReusableShell {
        val default = shells[key]
        return if (default == null) {
            val shell = ReusableShell(rootUser, status = TweakApplication.runtimeStatus)
            shells[key] = shell
            shell
        } else {
            default
        }
    }

    val getDefaultInstance: ReusableShell
        get() {
            var shell = defaultReusableShell
            for (i in 0 until MAX_DEFAULT_PRECESS_SIZE) {
                val key = "default-$i"
                val process = getDefault(key)
                if (!process.isIdle) {
                    continue
                }
                shell = process
            }
            return shell
        }

    suspend fun checkRoot(): Boolean {
        return getDefaultInstance.checkRoot()
    }

    fun tryExit() {
        defaultReusableShell.tryExit()
        for (i in 0 until MAX_DEFAULT_PRECESS_SIZE) {
            val key = "default-$i"
            shells[key]?.tryExit()
        }
    }

    /**
     * 同步执行命令行
     */
    suspend fun execSync(vararg cmd: String): String =
        defaultReusableShell.commitCmdSync(cmd.joinToString("\n"))

    /**
     * 同步执行命令行
     */
    suspend fun execSync(cmd: List<String>): String =
        defaultReusableShell.commitCmdSync(cmd.joinToString("\n"))
}