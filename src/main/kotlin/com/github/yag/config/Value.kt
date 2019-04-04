package com.github.yag.config

@Target(AnnotationTarget.FIELD)
annotation class Value(
    val config: String = "",
    val required: Boolean = false,
    val desc: String = ""
)