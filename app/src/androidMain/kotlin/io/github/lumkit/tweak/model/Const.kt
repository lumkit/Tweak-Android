package io.github.lumkit.tweak.model

object Const {
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
    const val APP_WORK_STATUS = "app_work_status"
    const val APP_BATTERY_CURRENT_NOW_UNIT = "app_battery_current_now_unit"

    object Notification {
        const val NOTIFICATION_CHANNEL_NAME_DEFAULT = "Tweak-Android"
        const val NOTIFICATION_CHANNEL_ID_DEFAULT = "tweak-android"
    }

    object Navigation {
        const val DEEP_LINE = "tweak://tweak.lumtoolkit.com/compose"
    }
}