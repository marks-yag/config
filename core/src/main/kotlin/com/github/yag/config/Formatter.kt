package com.github.yag.config

fun interface Formatter<T> {

    fun format(obj: T) : String

}
