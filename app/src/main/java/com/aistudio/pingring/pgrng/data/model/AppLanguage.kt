package com.aistudio.pingring.pgrng.data.model

enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val flag: String
) {
    TURKISH(
        code = "tr",
        nativeName = "Türkçe",
        englishName = "Turkish",
        flag = "🇹🇷"
    ),
    ENGLISH(
        code = "en",
        nativeName = "English",
        englishName = "English",
        flag = "🇬🇧"
    ),
    RUSSIAN(
        code = "ru",
        nativeName = "Русский",
        englishName = "Russian",
        flag = "🇷🇺"
    );

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: TURKISH
        }
    }
}
