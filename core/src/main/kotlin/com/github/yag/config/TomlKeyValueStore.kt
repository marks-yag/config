package com.github.yag.config

import org.tomlj.TomlParseResult

class TomlKeyValueStore(private val toml: TomlParseResult) : NestedKeyValueStore {
    override fun getSubStore(key: String): NestedKeyValueStore {
        TODO()
    }

    override fun getValue(key: String): String? {
        TODO("Not yet implemented")
    }

    override fun getFullKey(key: String): String {
        TODO("Not yet implemented")
    }

    override fun getEntries(): Set<Pair<String, String>> {
        TODO("Not yet implemented")
    }
}
