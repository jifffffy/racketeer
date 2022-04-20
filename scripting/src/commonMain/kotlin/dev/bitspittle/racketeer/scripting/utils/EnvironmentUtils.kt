package dev.bitspittle.racketeer.scripting.utils

import dev.bitspittle.limp.Environment
import dev.bitspittle.limp.Evaluator
import dev.bitspittle.limp.Method
import dev.bitspittle.racketeer.model.card.Card
import dev.bitspittle.racketeer.model.game.GameState
import dev.bitspittle.racketeer.scripting.converters.PileToCardsConverter
import dev.bitspittle.racketeer.scripting.converters.MutablePileToCardsConverter
import dev.bitspittle.racketeer.scripting.methods.card.*
import dev.bitspittle.racketeer.scripting.methods.collection.ChooseMethod
import dev.bitspittle.racketeer.scripting.methods.effect.FxAddMethod
import dev.bitspittle.racketeer.scripting.methods.game.*
import dev.bitspittle.racketeer.scripting.methods.pile.PileCopyToMethod
import dev.bitspittle.racketeer.scripting.methods.pile.PileGetMethod
import dev.bitspittle.racketeer.scripting.methods.pile.PileMoveToMethod
import dev.bitspittle.racketeer.scripting.methods.shop.ShopExcludeMethod
import dev.bitspittle.racketeer.scripting.methods.shop.ShopRerollMethod
import dev.bitspittle.racketeer.scripting.methods.system.CancelMethod
import dev.bitspittle.racketeer.scripting.methods.system.StopMethod
import dev.bitspittle.racketeer.scripting.methods.text.IconConvertMethod
import dev.bitspittle.racketeer.scripting.types.GameService

/**
 * Add a bunch of game-specific methods and other values here.
 */
fun Environment.installGameLogic(service: GameService) {
    // System
    addMethod(StopMethod(service.cardQueue))
    addMethod(CancelMethod())

    // Collection
    addMethod(ChooseMethod(service.logger, service.chooseHandler))

    // Game
    addMethod(GameGetMethod(service::gameState))
    addMethod(GameSetMethod(service::gameState))
    addMethod(GameDrawMethod(service::gameState))

    // Card
    addMethod(CardGetMethod())
    addMethod(CardSetMethod())
    addMethod(CardUpgradeMethod())
    addMethod(CardHasUpgradeMethod())
    addMethod(CardHasTypeMethod(service.gameData.cardTypes))
    addMethod(CardRemoveMethod(service::gameState))
    addMethod(CardTriggerMethod(service.cardQueue))
    addMethod(CardPileMethod(service::gameState))

    // Pile
    addConverter(MutablePileToCardsConverter())
    addConverter(PileToCardsConverter())
    addMethod(PileCopyToMethod(service::gameState))
    addMethod(PileMoveToMethod(service::gameState))
    addMethod(PileGetMethod(service.describer, service::gameState))

    // Effects
    addMethod(FxAddMethod(service::gameState))

    // Shop
    addMethod(ShopRerollMethod(service.gameState::shop))
    addMethod(ShopExcludeMethod(service.gameState::shop))
    (0..4).forEach { i -> storeValue("\$tier${i + 1}", i) }

    // Text
    addMethod(IconConvertMethod(service.describer))
}

/**
 * Add all variables related to the current game state into the environment.
 *
 * You probably want to do this within an [Environment.scoped] block, to avoid ever accidentally referring to stale game
 * state from previous turns.
 */
fun GameState.addVariablesInto(env: Environment) {
    env.storeValue("\$all-cards", allCards, allowOverwrite = true)
    env.storeValue("\$shop-tier", shop.tier, allowOverwrite = true)

    env.storeValue("\$deck", deck, allowOverwrite = true)
    env.storeValue("\$hand", hand, allowOverwrite = true)
    env.storeValue("\$street", street, allowOverwrite = true)
    env.storeValue("\$discard", discard, allowOverwrite = true)
    env.storeValue("\$jail", jail, allowOverwrite = true)

    env.storeValue("\$shop", shop.stock.filterNotNull(), allowOverwrite = true)

    env.addMethod(object : Method("\$owned", 0) {
        override suspend fun invoke(
            env: Environment,
            eval: Evaluator,
            params: List<Any>,
            options: Map<String, Any>,
            rest: List<Any>
        ): Any {
            return getOwnedCards()
        }
    }, allowOverwrite = true)
}

/**
 * Store the current card in the environment.
 *
 * You probably want to do this within an [Environment.scoped] block, tied to the lifetime of the current card being
 * played.
 */
fun Card.addVariableTo(env: Environment) {
    env.storeValue("\$this", this)
}
