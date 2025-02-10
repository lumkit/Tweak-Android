package io.github.lumkit.tweak.util

import java.util.regex.Pattern

/**
 * 字符串校验
 */
object FieldValidatorUtil {

    /**
     * 用户名校验
     * @param field 字段
     * @return boolean
     */
    fun usernameValid(field: String?): Boolean =
        field != null && Pattern.compile("^[a-zA-Z0-9_-]{6,16}$").matcher(field).matches()


    /**
     * 密码校验
     * @param field 字段
     * @return boolean
     */
    fun passwordValid(field: String?): Boolean =
        field != null && Pattern.compile("^[a-zA-Z0-9_.@]{6,20}$").matcher(field).matches()

    /**
     * 邮箱校验
     * @param field 字段
     * @return boolean
     */
    fun emailValid(field: String?): Boolean =
        field != null && Pattern.compile("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$")
            .matcher(field)
            .matches()

}