package dev.bitspittle.limp

import kotlin.random.Random
import kotlin.reflect.KClass

class Environment(val random: Random = Random.Default) {
    private val methodsStack = mutableListOf<MutableMap<String, Method>?>()
    private val variablesStack = mutableListOf<MutableMap<String, Value>?>()
    private val convertersStack = mutableListOf<MutableMap<KClass<*>, Converter<*>>?>()
    init {
        // Populate stacks
        pushScope()
    }

    fun addMethod(method: Method) {
        val methods = methodsStack.last() ?: mutableMapOf()
        methodsStack[methodsStack.lastIndex] = methods

        require(!methods.contains(method.name)) { "Attempted to register a method named \"${method.name}\" when one already exists at the current scope."}
        methods[method.name] = method
    }

    fun addConverter(converter: Converter<*>) {
        val converters = convertersStack.last() ?: mutableMapOf()
        convertersStack[convertersStack.lastIndex] = converters

        require(converters.values.none { it::class == converter::class }) { "Attempting to register more than once instance of a ${converter::class}" }
        converters[converter.toClass] = converter
    }

    fun storeValue(name: String, value: Value) {
        val variables = variablesStack.last() ?: mutableMapOf()
        variablesStack[variablesStack.lastIndex] = variables

        require(!variables.contains(name)) { "Attempted to register a variable named \"$name\" when one already exists at the current scope."}
        variables[name] = value
    }

    inline fun <T: Any?> scoped(block: () -> T): T {
        return try {
            pushScope()
            block()
        }
        finally {
            popScope()
        }
    }

    fun pushScope() {
        methodsStack.add(null)
        variablesStack.add(null)
        convertersStack.add(null)
    }

    fun popScope() {
        check(methodsStack.size > 1) // We should always have a ground state at least, which we set up at init time

        methodsStack.removeLast()
        variablesStack.removeLast()
        convertersStack.removeLast()
    }

    fun getMethod(name: String): Method? {
        return methodsStack.reversed().asSequence()
            .filterNotNull()
            .mapNotNull { methods -> methods[name] }
            .firstOrNull()
    }

    fun loadValue(name: String): Value? {
        return variablesStack.reversed().asSequence()
            .filterNotNull()
            .mapNotNull { variables -> variables[name] }
            .firstOrNull()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> tryConvert(value: Value, toClass: KClass<T>): T? {
        if (toClass.isInstance(value.wrapped)) {
            return value.wrapped as T
        }

        return convertersStack.reversed().asSequence()
            .filterNotNull()
            .mapNotNull { converters -> converters[toClass]?.convert(value.wrapped) as T? }
            .firstOrNull()
    }

    fun expectMethod(name: String): Method {
        return getMethod(name) ?: throw IllegalArgumentException("No method named \"$name\" is registered")
    }

    fun expectValue(name: String): Value {
        return loadValue(name) ?: throw IllegalArgumentException("No variable named \"$name\" is registered")
    }

    fun <T: Any> expectConvert(value: Value, toClass: KClass<T>): T {
        return tryConvert(value, toClass) ?: throw IllegalArgumentException("Could not convert ${value.wrapped::class} (value = \"${value.wrapped}\") to $toClass")
    }

    inline fun <reified T: Any> tryConvert(value: Value): T? = tryConvert(value, T::class)
    inline fun <reified T: Any> expectConvert(value: Value): T = expectConvert(value, T::class)
}