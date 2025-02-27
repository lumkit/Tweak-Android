package io.github.lumkit.tweak.model

object Const {
    const val APP_ENABLED_ACCESSIBILITY_SERVICE = "enabled_accessibility_service"
    const val APP_SHARED_RUNTIME_STATUS = "runtime_status"
    const val APP_AUTHORIZATION = "app_authorization"
    const val APP_AUTO_START_SERVICE: String = "app_auto_start_service"
    const val APP_OVERVIEW_TICK: String = "app_overview_tick"
    const val APP_SHELL_ROOT_USER: String = "app_shell_root_user"
    const val APP_ACCEPT_RISK: String = "app_accept_risk"
    const val APP_SHARED_RUNTIME_MODE_STATE: String = "runtime_mode_state"
    const val APP_SHARED_PROTOCOL_AGREE_STATE: String = "protocol_agree_state"
    const val APP_THEME_CUSTOM_COLOR_SCHEME: String = "custom_color_scheme"
    const val APP_THEME_DYNAMIC_COLOR: String = "theme_dynamic_color"
    const val APP_DARK_MODE_STATE: String = "dark_mode_state"
    const val APP_SHARED_PREFERENCE_ID: String = "shared"
    const val APP_BATTERY_CURRENT_NOW_UNIT = "app_battery_current_now_unit"
    const val APP_IGNORE_VERSION = "app_ignore_version"
    const val APP_DEVICE_ID = "device_id"

    object Notification {
        const val NOTIFICATION_CHANNEL_NAME_DEFAULT = "Tweak-Android"
        const val NOTIFICATION_CHANNEL_ID_DEFAULT = "tweak-android"
    }

    object Navigation {
        const val DEEP_LINE = "tweak://tweak.lumtoolkit.com/compose"
    }

    object SmartNotice {
        // 是否开启灵动通知
        const val SMART_NOTICE_SWITCH = "smart_notice_switch"
        // 状态通知
        const val SMART_NOTICE_NOTIFICATION = "smart_notice_notification"
        // 游戏模式
        const val SMART_NOTICE_GAME_MODE = "smart_notice_game_mode"

        // 宽高属性
        const val SMART_NOTICE_WIDTH = "smart_notice_width"
        const val SMART_NOTICE_HEIGHT = "smart_notice_height"
        const val SMART_NOTICE_PADDING = "smart_notice_padding"
        const val SMART_NOTICE_CUTOUT_RECT_LIST = "smart_notice_cutout_rect_list"
        const val SMART_NOTICE_CUSTOM_SIZE = "smart_notice_custom_size"
        const val SMART_NOTICE_ANIMATION_DELAY = "smart_notice_animation_delay"
        const val SMART_NOTICE_ANIMATION_DURATION = "smart_notice_animation_duration"

    }
}