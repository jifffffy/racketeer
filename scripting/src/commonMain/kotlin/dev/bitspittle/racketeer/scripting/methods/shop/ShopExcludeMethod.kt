package dev.bitspittle.racketeer.scripting.methods.shop

import dev.bitspittle.limp.Environment
import dev.bitspittle.limp.Evaluator
import dev.bitspittle.limp.Method
import dev.bitspittle.limp.types.Expr
import dev.bitspittle.racketeer.model.game.GameState
import dev.bitspittle.racketeer.model.game.GameStateChange
import dev.bitspittle.racketeer.model.shop.Exclusion

class ShopExcludeMethod(private val getGameState: () -> GameState) : Method("shop-exclude!", 1) {
    override suspend fun invoke(env: Environment, eval: Evaluator, params: List<Any>, options: Map<String, Any>, rest: List<Any>): Any {
        val excludeExpr = env.expectConvert<Expr>(params[0])
        getGameState().apply(GameStateChange.AddShopExclusion(Exclusion(excludeExpr.ctx.text) { card ->
            env.scoped { // Don't let any values defined during the lambda call escape
                val evaluator = eval.extend(mapOf("\$card" to card.instantiate()))
                env.expectConvert(evaluator.evaluate(env, excludeExpr))
            }
        }))

        return Unit
    }
}