package dev.bitspittle.racketeer.console.command.commands

import dev.bitspittle.racketeer.console.command.Command
import dev.bitspittle.racketeer.console.game.GameContext
import dev.bitspittle.racketeer.console.view.views.BrowseHandView
import dev.bitspittle.racketeer.console.view.views.BrowseStreetView

class BrowseStreetCommand(ctx: GameContext) : Command(ctx) {
    override val type = if (ctx.state.street.cards.isNotEmpty()) Type.Read else Type.Disabled
    override val title = "Browse the street (${ctx.state.street.cards.size})"

    override val description = "Look over the cards in the street."

    override suspend fun invoke(): Boolean {
        ctx.viewStack.pushView(BrowseStreetView(ctx))
        return true
    }
}

