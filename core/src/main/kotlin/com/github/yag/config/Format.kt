package com.github.yag.config

import java.lang.IllegalArgumentException

enum class Format(private vararg val extensions: String) {
    TOML("toml"),
    INI("ini", "properties");

    companion object {
        @JvmStatic
        fun getFormatByExtension(extension: String): Format {
            return values().firstOrNull { format ->
                format.extensions.any {
                    it.equals(extension, true)
                }
            } ?: throw IllegalArgumentException("Unrecognized extension: $extension")
        }
    }
}
