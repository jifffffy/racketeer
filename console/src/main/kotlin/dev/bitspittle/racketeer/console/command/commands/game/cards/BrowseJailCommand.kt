package dev.bitspittle.racketeer.console.command.commands.game.cards

import dev.bitspittle.racketeer.console.game.GameContext
import dev.bitspittle.racketeer.console.command.Command
import dev.bitspittle.racketeer.console.view.views.game.cards.BrowseJailView

class BrowseJailCommand(ctx: GameContext) : Command(ctx) {
    override val type = if (ctx.state.jail.cards.isNotEmpty()) Type.Normal else Type.Disabled
    override val title = "Browse jail (${ctx.state.jail.cards.size})"

    override val description = "Look over the cards that have been jailed."

    override suspend fun invoke(): Boolean {
        ctx.viewStack.pushView(BrowseJailView(ctx))
        return true
    }
}

