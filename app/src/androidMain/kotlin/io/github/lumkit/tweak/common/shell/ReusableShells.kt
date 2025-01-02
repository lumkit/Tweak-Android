package io.github.lumkit.tweak.common.shell

import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.model.Const
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object ReusableShells {
    private val shells = ConcurrentHashMap<String, ReusableShell>()

    @Synchronized
    fun getInstance(key: String, user: String): ReusableShell {
        val shell = ReusableShell(user)
        if (!shells.containsKey(key)) {
            shells[key] = shell
        }
        return shell
    }

    @Synchronized
    fun destroyInstance(key: String) {
        if (!shells.containsKey(key)) {
            return
        } else {
            shells[key]?.also { it.tryExit() }
            shells.remove(key)
        }
    }

    fun destroyAll() {
        shells.onEach {
            it.value.tryExit()
        }
        shells.clear()
    }

    private val rootUser: String
        get() = TweakApplication.shared.getString(Const.APP_SHELL_ROOT_USER, null) ?: "su"

    private val defaultReusableShell: ReusableShell
        get() {
            val key = "defaultReusableShell"
            val default = shells[key]
            return if (default == null) {
                val shell = ReusableShell(rootUser)
                shells[key] = shell
                shell
            } else {
                default
            }
        }
    private val secondaryReusableShell: ReusableShell
        get() {
            val key = "secondaryReusableShell"
            val default = shells[key]
            return if (default == null) {
                val shell = ReusableShell(rootUser)
                shells[key] = shell
                shell
            } else {
                default
            }
        }

    val getDefaultInstance: ReusableShell
        get() = if (defaultReusableShell.isIdle || !secondaryReusableShell.isIdle) {
            defaultReusableShell
        } else {
            secondaryReusableShell
        }

    suspend fun checkRoot(): Boolean {
        return getDefaultInstance.checkRoot()
    }

    fun tryExit() {
        defaultReusableShell.tryExit()
        secondaryReusableShell.tryExit()
    }

    /**
     * 同步执行命令行
     */
    suspend fun execSync(vararg cmd: String): String = defaultReusableShell.commitCmdSync(cmd.joinToString("\n\n"))

    /**
     * 同步执行命令行
     */
    suspend fun execSync(cmd: List<String>): String = defaultReusableShell.commitCmdSync(cmd.joinToString("\n\n"))
}