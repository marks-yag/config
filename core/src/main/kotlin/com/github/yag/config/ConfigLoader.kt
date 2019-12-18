package com.github.yag.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*


class ConfigLoader private constructor() {

    companion object {

        private val LOG: Logger = LoggerFactory.getLogger(ConfigLoader::class.java)

        @JvmStatic
        fun load(vararg configFiles: String): Properties {
            val result = Properties()
            configFiles.forEach { configFile ->
                val prop = Properties()
                val url = ::load::class.java.classLoader.getResource(configFile)
                if (url != null) {
                    prop.load(url.openStream())
                    LOG.info("Load configuration from: $url.")
                } else {
                    try {
                        FileInputStream(configFile).use {
                            prop.load(it)
                            LOG.info("Load configuration from $configFile.")
                        }
                    } catch (e: FileNotFoundException) {
                        throw IOException("Can not find configuration file: $configFile")
                    }
                }
                override(result, prop)
            }

            return result
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



