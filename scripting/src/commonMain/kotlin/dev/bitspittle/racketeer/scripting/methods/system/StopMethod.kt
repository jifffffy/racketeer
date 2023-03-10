package dev.bitspittle.racketeer.scripting.methods.system

import dev.bitspittle.limp.Environment
import dev.bitspittle.limp.Evaluator
import dev.bitspittle.limp.Method
import dev.bitspittle.racketeer.model.action.ActionQueue

class StopMethod(private val actionQueue: ActionQueue) : Method("stop!", 0) {
    override suspend fun invoke(
        env: Environment,
        eval: Evaluator,
        params: List<Any>,
        options: Map<String, Any>,
        rest: List<Any>
    ): Any {
        actionQueue.stop()

        return Unit
    }
}