package com.github.yag.config

import java.net.InetSocketAddress
import java.net.URI
import java.net.URL
import java.util.TreeSet

class StringConfig {

    @Value("username", required = true)
    var username: String = "root"

    @Value("password", required = true)
    @Encrypted("foo")
    lateinit var password: String

}

class NumberConfig {

    @Value("port", required = true)
    var port: Int = 80

    @Value("timeoutMs", required = true)
    var timeoutMs: Long = 1000L

    @Value("percent", required = true)
    var percent: Double = 0.8

}

class BooleanConfig {

    @Value("auth", required = true)
    var auth: Boolean = false

}

class EnumConfig {

    @Value("mode", required = true)
    var mode = Mode.CLUSTER

}

class EndpointConfig {

    @Value("address", required = true)
    lateinit var address: InetSocketAddress

    @Value("url", required = true)
    lateinit var url: URL

    @Value("uri", required = true)
    lateinit var uri: URI

}

class CollectionConfig {

    @Value("options", required = true)
    var options = TreeSet<Options>()

}

class MapConfig {

    @Value("options")
    val options = HashMap<String, Set<Options>>().apply {
        this["test"] = setOf(Options.Indexing, Options.Compression)
        this["prod"] = setOf(Options.Indexing, Options.Compression, Options.Encryption)
    }

}

class NestConfig {

    @Value("enum", required = true)
    var enum = EnumConfig()

    @Value("bool")
    val bool: BooleanConfig? = null

}

class DefaultNameConfig {

    @Value
    lateinit var hello: String

    @Value
    lateinit var helloWorld: String

}

enum class Options {
    Encryption,
    Compression,
    Indexing
}

enum class Mode {
    SINGLE, CLUSTER
}