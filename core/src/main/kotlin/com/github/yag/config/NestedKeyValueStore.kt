package com.github.yag.config

interface NestedKeyValueStore {

    fun getSubStore(key: String) : NestedKeyValueStore

    fun getValue(key: String) : String?

    fun getFullKey(key: String) : String

    fun getEntries() : Set<Pair<String, String>>

}
