package dev.bitspittle.limp

import kotlin.reflect.KClass

abstract class Converter<T: Any>(val toClass: KClass<out T>) {
    abstract fun convert(value: Any): T?
}

@Suppress("UNCHECKED_CAST")
class Converters {
    private val converters = mutableMapOf<KClass<*>, Converter<*>>()

    fun <T: Any> register(converter: Converter<T>) {
        require(converters.values.none { it::class == converter::class }) { "Attempting to register more than once instance of a ${converter::class}" }
        converters[converter.toClass] = converter
    }

    fun <T: Any> convert(value: Any, toClass: KClass<T>): T? {
        return converters[toClass]?.convert(value) as T?
    }
}