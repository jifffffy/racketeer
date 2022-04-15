package dev.bitspittle.racketeer.console.command.commands.admin

import dev.bitspittle.racketeer.console.command.Command
import dev.bitspittle.racketeer.console.game.GameContext
import dev.bitspittle.racketeer.console.view.popPastAndRefresh
import dev.bitspittle.racketeer.console.view.views.admin.AdminMenuView
import dev.bitspittle.racketeer.model.card.Card
import dev.bitspittle.racketeer.model.card.Pile

class ChoosePileToCommand(
    ctx: GameContext,
    private val pile: Pile,
    private val cards: List<Card>,
    forceDisabled: Boolean = false
) : Command(ctx) {
    override val type: Type = if (forceDisabled) Type.Disabled else Type.Warning
    override val title = "${pile.toTitle(ctx.state)} (${pile.cards.size})"

    override suspend fun invoke(): Boolean {
        ctx.state.move(cards, pile)
        ctx.viewStack.popPastAndRefresh() { view -> view is AdminMenuView }
        return true
    }
}

