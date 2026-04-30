package demo.dataModel.did

import core.GridBind

/**
 * 27安全等级 - 使用枚举类
 */
enum class SecurityLevel27(
    val level: Int,
    val requiresAuthentication: Boolean
) {
    @GridBind(headerText = "Level1", pattern = "Level1|level1|LEVEL1|1")
    LEVEL1(level = 1, requiresAuthentication = false),

    @GridBind(headerText = "Level2", pattern = "Level2|level2|LEVEL2|2")
    LEVEL2(level = 2, requiresAuthentication = true),

    @GridBind(headerText = "Level3", pattern = "Level3|level3|LEVEL3|3")
    LEVEL3(level = 3, requiresAuthentication = true),

    @GridBind(headerText = "Level4", pattern = "Level4|level4|LEVEL4|4")
    LEVEL4(level = 4, requiresAuthentication = true);

    companion object {
        /**
         * 从整数解析安全等级
         */
        fun fromLevel(level: Int): SecurityLevel27? =
            entries.find { it.level == level }

        /**
         * 从字符串解析，支持多种格式
         */
        fun fromString(name: String): SecurityLevel27? = when {
            name.toIntOrNull() != null -> fromLevel(name.toInt())
            else -> entries.find {
                it.name.equals(name, ignoreCase = true) ||
                        it.toString().equals(name, ignoreCase = true)
            }
        }
    }

    /**
     * 比较安全等级
     */
    infix fun isAtLeast(other: SecurityLevel27): Boolean =
        this.level >= other.level

    /**
     * 检查是否需要认证
     */
    fun requiresAuth(): Boolean = requiresAuthentication
}