package com.github.yag.config

import java.net.InetSocketAddress

sealed class ValueType<T> {

    open fun toString(obj: T) : String = obj.toString()

    abstract fun parse(str: String) : T

}

class EnumType<T : Enum<*>>(private val type: Class<T>): ValueType<T>() {

    override fun toString(obj: T): String {
        return obj.name
    }

    override fun parse(str: String): T {
        return getEnumValue(type, str) as T
    }

}

class IntType : ValueType<Int>() {

    override fun parse(str: String): Int {
        return str.toInt()
    }
}

class InetSocketAddressType : ValueType<InetSocketAddress>() {

    override fun toString(obj: InetSocketAddress): String {
        return "${obj.hostString}:${obj.port}"
    }

    override fun parse(str: String): InetSocketAddress {
        return str.split(":").let { InetSocketAddress(it[0], it[1].toInt()) }
    }
}
