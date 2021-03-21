package com.github.yag.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.tomlj.Toml
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*


class ConfigLoader private constructor() {

    companion object {

        private val LOG: Logger = LoggerFactory.getLogger(ConfigLoader::class.java)

        @JvmStatic
        @JvmOverloads
        fun load(format: Format? = null, vararg configFiles: String): Properties {
            val realFormat = format?: run {
                val extension = configFiles.map { it.substringAfterLast('.', "") }.toSet().single()
                Format.getFormatByExtension(extension)
            }

            return configFiles.map {
                val url = ConfigLoader::class.java.classLoader.getResource(it)
                if (url != null) {
                    LOG.info("Load configuration from: {}.", url)
                    url.openStream()
                } else {
                    val path = Paths.get(it).toAbsolutePath()
                    if (Files.exists(path)) {
                        LOG.info("Load configuration from: {}.", path)
                        Files.newInputStream(path)
                    } else {
                        throw IOException("Can not find configuration file: $it")
                    }
                }
            }.let {
                SequenceInputStream(Collections.enumeration(it))
            }.use { input ->
                when (realFormat) {
                    Format.TOML -> {
                        Toml.parse(input)
                        TODO()
                    }
                    Format.INI -> {
                        Properties().apply {
                            load(input)
                        }
                    }
                }
            }
        }

        @JvmStatic
        fun override(target: Properties, newProp: Properties) {
            newProp.forEach { t, u ->
                if (target.containsKey(t)) {
                    LOG.info("Override configuration {} to {}.", t, u)
                }
                target[t] = u
            }
        }
    }
}



