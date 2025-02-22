package io.github.lumkit.tweak.ui.screen.runtime

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewModelScope
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.common.BaseViewModel
import io.github.lumkit.tweak.common.permissions.checkPermission
import io.github.lumkit.tweak.common.shell.provide.ReusableShells
import io.github.lumkit.tweak.common.shell.provide.RuntimeProvider
import io.github.lumkit.tweak.common.status.TweakException
import io.github.lumkit.tweak.common.util.ShizukuStatic
import io.github.lumkit.tweak.data.RuntimeStatus
import io.github.lumkit.tweak.model.Config
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.ui.local.StorageStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.io.File

@SuppressLint("StaticFieldLeak")
class RuntimeModeViewModel(
    private val context: Context,
    private val storageStore: StorageStore,
) : BaseViewModel() {

    private val _initConfigDialogState = MutableStateFlow(false)
    val initConfigDialogState = _initConfigDialogState.asStateFlow()

    private val _initConfigDialogMessage = MutableStateFlow("正在检查必要权限...")
    val initConfigDialogMessage = _initConfigDialogMessage.asStateFlow()

    private val _rootModeDialogState = MutableStateFlow(false)
    val rootModeDialogState = _rootModeDialogState.asStateFlow()

    private val _shizukuModeDialogState = MutableStateFlow(false)
    val shizukuModeDialogState = _shizukuModeDialogState.asStateFlow()

    private val _permissionState = MutableStateFlow(PermissionState.Request)
    val permissionState = _permissionState.asStateFlow()

    fun setRootModeDialogState(state: Boolean) {
        _rootModeDialogState.value = state
    }

    fun setShizukuModeDialogState(state: Boolean) {
        _shizukuModeDialogState.value = state
    }

    fun setInitConfigDialogState(state: Boolean) {
        _initConfigDialogState.value = state
    }

    /**
     * 初始化Root配置
     */
    fun initRootModeConfig(
        error: (Throwable) -> Unit,
    ) = suspendLaunch(
        id = "initRootModeConfig",
        onError = {
            it.printStackTrace()
            viewModelScope.launch { error(it) }
        }
    ) {
        loading()

        TweakApplication.setRuntimeStatusState(RuntimeStatus.Root)

        installBusybox()
        checkNecessaryPermissionsForRoot()

        success("初始化成功")
    }

    /**
     * 初始化Shizuku配置
     */
    fun initShizukuModeConfig(
        error: (Throwable) -> Unit
    ) = suspendLaunch(
        id = "initShizukuModeConfig",
        onError = {
            it.printStackTrace()
            viewModelScope.launch { error(it) }
        }
    ) {
        loading()

        TweakApplication.setRuntimeStatusState(RuntimeStatus.Shizuku)

        installBusybox()
        checkNecessaryPermissionsForShizuku()

        success("初始化成功！")
    }

    /**
     * 安装Busybox
     */
    fun installBusybox() {
        val abi = Build.SUPPORTED_ABIS.first().lowercase()
        val assetPath = if (abi.contains("arm")) {
            "bin/aarch64/busybox"
        } else {
            "bin/x86_64/busybox"
        }

        // 安装Busybox到本地
        val busyboxFile = Config.Path.BusyboxFile
        if (!busyboxFile.exists()) {
            context.assets.open(assetPath).use {
                it.buffered().use { bis ->
                    busyboxFile.outputStream().use { fos ->
                        fos.buffered().use { bos ->
                            bis.copyTo(bos)
                        }
                    }
                }
            }
        }

        // 设置环境变量
        RuntimeProvider.setWorkPath(busyboxFile.absolutePath)
    }

    enum class PermissionState {
        Request, Success, RetryCheckRoot, RetryCheckShizuku
    }

    private suspend fun checkNecessaryPermissionsForRoot() {
        _rootModeDialogState.value = false
        _initConfigDialogState.value = true
        _initConfigDialogMessage.value = "开始检查Root权限..."
        _permissionState.value = PermissionState.Request

        val checkRoot = ReusableShells.checkRoot()

        TweakApplication.rootUserState = checkRoot

        // 没有Root
        if (!checkRoot) {
            val exception =
                TweakException("未检测到Root权限，请确保设备已获得Root权限并将Tweak纳入Root管理器的白名单之中。")
            _permissionState.value = PermissionState.RetryCheckRoot
            _initConfigDialogMessage.value = exception.message
            throw exception
        }

        val binDir = Config.Path.binDir
        val scriptFile = File(binDir, "install_busybox.sh")
        context.assets.open("script/install_busybox.sh").use {
            it.buffered().use { bis ->
                scriptFile.outputStream().use { fos ->
                    fos.buffered().use { bos ->
                        bis.copyTo(bos)
                    }
                }
            }
        }

        ReusableShells.destroyAll()

        val result = ReusableShells.execSync("sh ${scriptFile.absolutePath} ${binDir.absolutePath} & echo 'success'")

        Log.d("Install Busybox", result)

        grantPermission()

        if (!(context.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE) && context.checkPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        ) {
            ActivityCompat.requestPermissions(
                context as ComponentActivity,
                mutableListOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                    Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Manifest.permission.WAKE_LOCK,
                ).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray(),
                0x11
            )
        }

        if (!Settings.System.canWrite(context)) {
            // TODO 授予修改系统设置权限

        }

        _permissionState.value = PermissionState.Success
        storageStore.putBoolean(Const.APP_SHARED_RUNTIME_MODE_STATE, true)
    }

    private suspend fun checkNecessaryPermissionsForShizuku() {
        _shizukuModeDialogState.value = false
        _initConfigDialogState.value = true
        _initConfigDialogMessage.value = "开始检查Shizuku权限..."
        _permissionState.value = PermissionState.Request

        TweakApplication.rootUserState = false

        if (!Shizuku.pingBinder()) {
            val exception = TweakException("Shizuku未激活！")
            _permissionState.value = PermissionState.RetryCheckShizuku
            _initConfigDialogMessage.value = exception.message
            throw exception
        }

        if (!ShizukuStatic.checkPermission(12138)) {
            val exception = TweakException("请先授予Shizuku权限")
            _permissionState.value = PermissionState.RetryCheckShizuku
            _initConfigDialogMessage.value = exception.message
            throw exception
        }

        val binDir = Config.Path.binDir
        val scriptFile = File(binDir, "install_busybox.sh")
        context.assets.open("script/install_busybox.sh").use {
            it.buffered().use { bis ->
                scriptFile.outputStream().use { fos ->
                    fos.buffered().use { bos ->
                        bis.copyTo(bos)
                    }
                }
            }
        }

        ReusableShells.destroyAll()

        val result = ReusableShells.execSync("sh ${scriptFile.absolutePath} ${binDir.absolutePath} & echo 'success'")

        Log.d("Install Busybox", result)

        grantPermission()

        if (!(context.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE) && context.checkPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        ) {
            ActivityCompat.requestPermissions(
                context as ComponentActivity,
                mutableListOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                    Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Manifest.permission.WAKE_LOCK,
                ).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray(),
                0x11
            )
        }

        if (!Settings.System.canWrite(context)) {
            // TODO 授予修改系统设置权限

        }

        _permissionState.value = PermissionState.Success
        storageStore.putBoolean(Const.APP_SHARED_RUNTIME_MODE_STATE, true)
    }

    private suspend fun grantPermission() {
        _initConfigDialogMessage.value = "开始检测必要的权限..."
        _permissionState.value = PermissionState.Request
        val stringBuilder = StringBuilder()
        // 必需的权限
        val requiredPermission = mutableListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CHANGE_CONFIGURATION,
            Manifest.permission.WRITE_SECURE_SETTINGS,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requiredPermission.addAll(
                listOf(
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                )
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermission.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        requiredPermission.onEach {
            when (it) {
                Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                        stringBuilder.append("appops set --uid ${context.packageName} MANAGE_EXTERNAL_STORAGE allow\n")
                    }
                }

                Manifest.permission.SYSTEM_ALERT_WINDOW -> {
                    if (!context.checkPermission(it)) {
                        val option = it.substring("android.permission.".length)
                        stringBuilder.append("appops set ${context.packageName} $option allow\n")
                        stringBuilder.append("pm grant ${context.packageName} $it\n")
                    }
                }

                else -> {
                    val option = it.substring("android.permission.".length)
                    stringBuilder.append("appops set ${context.packageName} $option allow\n")
                    stringBuilder.append("pm grant ${context.packageName} $it\n")
                }
            }
        }

        if (!context.checkPermission(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)) {
            stringBuilder.append("dumpsys deviceidle whitelist +${context.packageName};\n")
        }

        val result = ReusableShells.execSync(stringBuilder.toString())
        Log.d("grantPermission", result)
    }

    fun finish() {
        setInitConfigDialogState(false)
        setRootModeDialogState(false)
        setShizukuModeDialogState(false)
        _permissionState.value = PermissionState.Request
    }
}