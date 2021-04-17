package com.github.yag.config

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class SubTypes(val values: Array<Type>) {

    annotation class Type(val value: KClass<*>, val id: String)

}
