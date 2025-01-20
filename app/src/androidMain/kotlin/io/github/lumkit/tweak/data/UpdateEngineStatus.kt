package io.github.lumkit.tweak.data

enum class UpdateEngineStatus(val statusCode: Int) {
    // 空闲状态
    UPDATE_STATUS_IDLE(0),

    // 检查更新中
    UPDATE_STATUS_CHECKING_FOR_UPDATE(1),

    // 更新可用
    UPDATE_STATUS_UPDATE_AVAILABLE(2),

    // 下载中
    UPDATE_STATUS_DOWNLOADING(3),

    // 验证中
    UPDATE_STATUS_VERIFYING(4),

    // 最终化（完成）
    UPDATE_STATUS_FINALIZING(5),

    // 更新完成，等待重启
    UPDATE_STATUS_UPDATED_NEED_REBOOT(6),

    // 报告错误事件
    UPDATE_STATUS_REPORTING_ERROR_EVENT(7),

    // 尝试回滚
    UPDATE_STATUS_ATTEMPTING_ROLLBACK(8),

    // 被禁用
    UPDATE_STATUS_DISABLED(9),

    // Tweak 错误
    ERROR(-1);

    companion object {
        // 根据 statusCode 获取对应的 UpdateEngineStatus
        fun fromStatusCode(statusCode: Int): UpdateEngineStatus? {
            return entries.firstOrNull { it.statusCode == statusCode }
        }
    }
}