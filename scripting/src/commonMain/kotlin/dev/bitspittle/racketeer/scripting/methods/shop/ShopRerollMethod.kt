package dev.bitspittle.racketeer.scripting.methods.shop

import dev.bitspittle.limp.Environment
import dev.bitspittle.limp.Evaluator
import dev.bitspittle.limp.Method
import dev.bitspittle.limp.converters.PlaceholderConverter
import dev.bitspittle.limp.types.Expr
import dev.bitspittle.racketeer.model.shop.Shop

class ShopRerollMethod(private val getShop: () -> Shop) : Method("shop-reroll!", 1) {
    override suspend fun invoke(env: Environment, eval: Evaluator, params: List<Any>, options: Map<String, Any>, rest: List<Any>): Any {
        val cardFilterExpr = env.scoped {
            env.addConverter(PlaceholderConverter(Expr.Stub(true)))
            env.expectConvert<Expr>(params[0])
        }

        getShop().restock { card ->
            val evaluator = eval.extend(mapOf("\$card" to card.instantiate()))
            env.expectConvert(evaluator.evaluate(env, cardFilterExpr))
        }

        return Unit
    }
}