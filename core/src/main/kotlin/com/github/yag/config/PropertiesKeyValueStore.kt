package com.github.yag.config

import kotlin.NoSuchElementException

class PropertiesKeyValueStore(private val map: Map<String, String>, private val base: String = "") : NestedKeyValueStore {

    override fun getSubStore(key: String): PropertiesKeyValueStore {
        val prefix = "${getFullKey(key)}."
        return PropertiesKeyValueStore(map, prefix)
    }

    override fun getValue(key: String): String? {
        require(!key.contains('.')) {
            key
        }
        return map[getFullKey(key)]
    }

    override fun getFullKey(key: String) : String {
        require(!key.contains('.')) {
            key
        }
        return "$base$key"
    }

    override fun getEntries(): Set<Pair<String, String>> {
        return map.entries.filter {
            it.key.startsWith(base)
        }.map {
            it.key.removePrefix(base) to it.value
        }.filter {
            !it.first.contains('.')
        }.toSet()
    }
}
