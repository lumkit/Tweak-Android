package io.github.lumkit.tweak.common.permissions

import android.content.Context
import androidx.core.content.PermissionChecker

/**
 * 检测权限
 */
fun Context.checkPermission(permission: String): Boolean =
    PermissionChecker.checkSelfPermission(
        this,
        permission
    ) == PermissionChecker.PERMISSION_GRANTED