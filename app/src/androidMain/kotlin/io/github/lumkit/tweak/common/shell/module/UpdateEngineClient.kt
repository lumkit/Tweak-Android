package io.github.lumkit.tweak.common.shell.module

import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.common.shell.io.impl.RootFile
import io.github.lumkit.tweak.common.shell.provide.ReusableShells
import io.github.lumkit.tweak.common.status.TweakException
import io.github.lumkit.tweak.data.UpdateEngineStatus
import io.github.lumkit.tweak.model.Config
import io.github.lumkit.tweak.services.PersistentTaskService.Companion.progress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class UpdateEngineClient {
    companion object {

        private val _updateEngineClientState =
            MutableStateFlow(UpdateEngineStatus.UPDATE_STATUS_IDLE to 0f)
        val updateEngineStatus = _updateEngineClientState.asStateFlow()

        private val _completeCodeState = MutableStateFlow(-1)
        val completeCodeState = _completeCodeState.asStateFlow()

        private val _tookState = MutableStateFlow(0L)
        val tookState = _tookState.asStateFlow()

        /**
         * 是否支持OTA更新
         */
        suspend fun support(): Boolean {
            val result = ReusableShells.execSync("ls /dev/block/bootdevice/by-name")
            return result.contains("_a|_b".toRegex()) && RootFile("/system/bin/update_engine_client").exists()
        }

        suspend fun unzipRom(romPath: String): String {
            val entry = ReusableShells.execSync("unzip -l $romPath")
            if (!entry.contains("payload\\.bin|payload_properties\\.txt".toRegex())) {
                throw TweakException("ROM不完整，请重新选择文件！")
            }

            val rootFile = RootFile(Config.Path.cacheDir.absolutePath)

            if (!rootFile.exists()) {
                rootFile.mkdirs()
            }

            ReusableShells.execSync(
                "rm -rf ${rootFile.path}/*",
                "unzip $romPath -d ${rootFile.path}",
                "chmod -R 777 ${rootFile.path}"
            )
            return rootFile.path
        }

        suspend fun installRom(dir: String) {
            val bin = File(dir, "payload.bin")
            val property = File(dir, "payload_properties.txt")
            val propertiesContent = ReusableShells.execSync("cat ${property.absolutePath}")

            val cmd = mutableListOf(
                "update_engine_client \\",
                "--update \\",
                "--payload='file://${bin.absolutePath}' \\",
                "--headers='"
            ).apply {
                propertiesContent.split("\n").forEach(::add)
                add("'")
            }

            ReusableShells.execSync(cmd)
        }
    }

    private val reusableShell =
        ReusableShells.getInstance("update_engine_client", redirectErrorStream = true, status = TweakApplication.runtimeStatus)
    private val updateEngineCoroutine = CoroutineScope(Dispatchers.IO)
    private val updateEnginClientFile = RootFile("/system/bin/update_engine_client")

    /**
     * 监听update_engine_client的action
     */
    fun watch() {
        reusableShell.setOnReadLineListener { line ->
            val info = line.info()

            // update_engine_client_android
            run {
                if (info.first.contains("update_engine_client_android")) {
                    when (info.infoCode()) {
                        -1 -> {
                            _updateEngineClientState.value = UpdateEngineStatus.ERROR to 0f
                        }

                        // onStatusUpdate回调
                        95, 96 -> {
                            val statusUpdate = info.onStatusUpdate()
                            _updateEngineClientState.value = statusUpdate
                            TweakApplication.application.progress(
                                progress = statusUpdate.second,
                                statusStringRes = when (statusUpdate.first) {
                                    UpdateEngineStatus.UPDATE_STATUS_IDLE -> R.string.text_status_idle
                                    UpdateEngineStatus.UPDATE_STATUS_CHECKING_FOR_UPDATE -> R.string.text_checking_for_update
                                    UpdateEngineStatus.UPDATE_STATUS_UPDATE_AVAILABLE -> R.string.text_update_available
                                    UpdateEngineStatus.UPDATE_STATUS_DOWNLOADING -> R.string.text_status_downloading
                                    UpdateEngineStatus.UPDATE_STATUS_VERIFYING -> R.string.text_status_verifying
                                    UpdateEngineStatus.UPDATE_STATUS_FINALIZING -> R.string.text_status_finishing
                                    UpdateEngineStatus.UPDATE_STATUS_UPDATED_NEED_REBOOT -> R.string.text_status_need_reboot
                                    UpdateEngineStatus.UPDATE_STATUS_REPORTING_ERROR_EVENT -> R.string.text_status_error_event
                                    UpdateEngineStatus.UPDATE_STATUS_ATTEMPTING_ROLLBACK -> R.string.text_status_attempting_rollback
                                    UpdateEngineStatus.UPDATE_STATUS_DISABLED -> R.string.text_status_disabled
                                    UpdateEngineStatus.ERROR -> R.string.text_unknown_error
                                }
                            )
                        }

                        // onPayloadApplicationComplete回调
                        104 -> {
                            val completeCode = info.onPayloadApplicationComplete()
                            _completeCodeState.value = completeCode
                        }

                        354 -> {
                            val took = info.commandTook()
                            _tookState.value = took
                        }
                    }
                }
            }

            run {

            }
        }
    }

    /**
     * 跟踪更新状态
     */
    fun follow() {
        updateEngineCoroutine.launch {
            while (isActive) {
                if (updateEnginClientFile.exists()) {
                    reusableShell.commitCmdSync("update_engine_client --follow")
                }
            }
        }
    }

    private fun Pair<String, String>.infoCode(): Int {
        val cc = first
        if (!cc.contains("update_engine_client_android")) return 0
        val info = cc.substring(cc.indexOf("[") + 1, cc.indexOf("]"))
        return info.substring(info.indexOf("(") + 1, info.indexOf(")")).trim().toIntOrNull() ?: -1
    }

    private fun String.info(): Pair<String, String> = run {
        substring(0, indexOf(" ")) to substring(indexOf(" ") + 1)
    }

    private fun Pair<String, String>.onStatusUpdate(): Pair<UpdateEngineStatus, Float> {
        val info = second
        val detail = info.substring(info.indexOf("(") + 1, info.lastIndexOf(")"))

        val data = detail.split("\\s+".toRegex())
        val statusStr = data[0].trim()
        val progressStr = data[2]
        return try {
            UpdateEngineStatus.valueOf(statusStr)
        } catch (_: Exception) {
            UpdateEngineStatus.ERROR
        } to (progressStr.toFloatOrNull() ?: 0f)
    }

    private fun Pair<String, String>.onPayloadApplicationComplete(): Int {
        return second.substring(second.length - 3).toIntOrNull() ?: -1
    }

    private fun Pair<String, String>.commandTook(): Long =
        second.trim().split("\\s+".toRegex())[2].toLongOrNull() ?: 0


}