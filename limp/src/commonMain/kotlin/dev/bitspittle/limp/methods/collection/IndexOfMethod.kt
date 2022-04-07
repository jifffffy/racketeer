package dev.bitspittle.limp.methods.collection

import dev.bitspittle.limp.Environment
import dev.bitspittle.limp.Evaluator
import dev.bitspittle.limp.Method
import dev.bitspittle.limp.converters.ValueToExprConverter
import dev.bitspittle.limp.types.Expr

class IndexOfMethod : Method("index-of", 2) {
    override suspend fun invoke(env: Environment, params: List<Any>, options: Map<String, Any>, rest: List<Any>): Any {
        val list = env.expectConvert<List<*>>(params[0])

        val predicate = env.tryConvert<Expr>(params[1])
        return if (predicate != null) {
            list.indexOfFirst { item ->
                item != null && env.expectConvert(Evaluator(mapOf("\$it" to item)).evaluate(env, predicate))
            }
        } else {
            list.indexOf(params[1])
        }
    }
}