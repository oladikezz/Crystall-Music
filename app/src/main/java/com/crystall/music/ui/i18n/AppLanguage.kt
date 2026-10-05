package com.crystall.music.ui.i18n

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String
) {
    EN("en", "English", "English", "🇺🇸"),
    RU("ru", "Russian", "Русский", "🇷🇺"),
    AZ("az", "Azerbaijani", "Azərbaycan", "🇦🇿"),
    TR("tr", "Turkish", "Türkçe", "🇹🇷");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: RU
        }
    }
}
