package com.github.yag.config

import org.slf4j.LoggerFactory
import org.tomlj.TomlArray
import org.tomlj.TomlTable

class TomlKeyValueStore(private val table: TomlTable, private val base: String = "") : NestedKeyValueStore {

    override fun getSubStore(key: String): NestedKeyValueStore {
        val prefix = "${getFullKey(key)}."
        return TomlKeyValueStore(table.getTableOrEmpty(key), prefix)
    }

    override fun <T : Any> getValue(key: String, type: Class<T>, encryptedKey: String?): T? {
        require(!key.contains('.')) {
            key
        }
        return table.get(key)?.let {  value ->
            val str = if (value is TomlArray) {
                Array(value.size()) {
                    value.get(it)
                }.joinToString(",")
            } else {
                value.toString()
            }
            SimpleObjectParser.parse(type, str, encryptedKey)
        }
    }

    override fun getFullKey(key: String): String {
        require(!key.contains('.')) {
            key
        }
        return "$base$key"
    }

    override fun readCollection(key: String): Collection<String>? {
        require(!key.contains('.')) {
            key
        }

        val array = table.getArrayOrEmpty(key)
        val size = array.size()
        return Array(size) {
            array.get(it).toString()
        }.toList()
    }

    override fun getEntries(): Set<String> {
        return table.keySet()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(TomlKeyValueStore::class.java)
    }
}
