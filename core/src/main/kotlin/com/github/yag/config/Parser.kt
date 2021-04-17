package com.github.yag.config

fun interface Parser<T> {

    fun parse(str: String): T

}
