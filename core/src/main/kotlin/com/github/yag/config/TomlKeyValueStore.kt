package com.github.yag.config

import org.slf4j.LoggerFactory
import org.tomlj.TomlParseResult

class TomlKeyValueStore(private val toml: TomlParseResult) : NestedKeyValueStore {

    init {
        toml.errors().forEach {
            LOG.warn("Parse toml failed: {}.", it)
        }
    }

    override fun getSubStore(key: String): NestedKeyValueStore {
        TODO()
    }

    override fun getValue(key: String): String? {
        return toml.getString(key)
    }

    override fun getFullKey(key: String): String {

        TODO("Not yet implemented")
    }

    override fun getEntries(): Set<Pair<String, String>> {
        return toml.keySet().map {
            it to getValue(it)!!
        }.toSet()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(TomlKeyValueStore::class.java)
    }
}
