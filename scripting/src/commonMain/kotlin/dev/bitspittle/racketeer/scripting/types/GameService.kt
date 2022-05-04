package dev.bitspittle.racketeer.scripting.types

import dev.bitspittle.limp.types.Logger
import dev.bitspittle.racketeer.model.card.CardQueue
import dev.bitspittle.racketeer.model.game.GameData
import dev.bitspittle.racketeer.model.game.MutableGameState
import dev.bitspittle.racketeer.model.text.Describer
import dev.bitspittle.racketeer.scripting.methods.collection.ChooseHandler

/**
 * Misc. values and functionality provided by the game implementation needed by our scripting system
 */
interface GameService {
    val gameData: GameData
    val describer: Describer
    val gameState: MutableGameState
    val cardQueue: CardQueue
    val chooseHandler: ChooseHandler

    val logger: Logger
}
