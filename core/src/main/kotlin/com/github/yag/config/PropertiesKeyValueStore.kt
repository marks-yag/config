package com.github.yag.config

import kotlin.NoSuchElementException

class PropertiesKeyValueStore(private val map: Map<String, String>, private val base: String = "") : NestedKeyValueStore {

    override fun getSubStore(key: String): PropertiesKeyValueStore {
        return PropertiesKeyValueStore(map, "$base$key.")
    }

    override fun getValue(key: String): String? {
        return map[getFullKey(key)]
    }

    override fun getFullKey(key: String) : String {
        return "$base$key"
    }

    override fun getEntries(): Set<Pair<String, String>> {
        return map.entries.filter {
            it.key.startsWith("$base")
        }.map {
            it.key.removePrefix("$base") to it.value
        }.filter {
            !it.first.contains('.')
        }.toSet()
    }
}
