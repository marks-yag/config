package com.github.yag.config

@Target(AnnotationTarget.FIELD)
annotation class Encrypted(val key: String)
