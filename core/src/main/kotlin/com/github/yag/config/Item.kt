package com.github.yag.config

data class Item(val value: Any, val annotation: Value, val required: Boolean = annotation.required)