package dev.bitspittle.racketeer.scripting.methods

import dev.bitspittle.limp.Environment
import dev.bitspittle.limp.Method
import dev.bitspittle.limp.types.Expr
import dev.bitspittle.limp.utils.toEnum
import dev.bitspittle.racketeer.scripting.types.GameProperty
import dev.bitspittle.racketeer.scripting.types.GameService

class GameAddMethod(private val service: GameService) : Method("game-add", 2) {
    override suspend fun invoke(env: Environment, params: List<Any>, options: Map<String, Any>, rest: List<Any>): Any {
        val identifier = env.expectConvert<Expr.Identifier>(params[0])
        val property = identifier.toEnum(GameProperty.values())
        val amount = env.expectConvert<Int>(params[1])

        val gameState = service.gameState
        when (property) {
            GameProperty.CASH -> gameState.cash += amount
            GameProperty.VP -> gameState.vp += amount
            GameProperty.INFLUENCE -> gameState.influence += amount
        }

        return Unit
    }
}