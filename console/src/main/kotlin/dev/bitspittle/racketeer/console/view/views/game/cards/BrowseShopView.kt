package dev.bitspittle.racketeer.console.view.views.game.cards

import dev.bitspittle.racketeer.console.command.Command
import dev.bitspittle.racketeer.console.game.GameContext
import dev.bitspittle.racketeer.console.view.View

class BrowseShopView(ctx: GameContext) : View(ctx) {
    init {
        check(ctx.state.shop.stock.filterNotNull().isNotEmpty())
    }

    override val subtitle get() = "Shop (Tier ${ctx.state.shop.tier + 1})"

    override fun createCommands(): List<Command> =
        ctx.state.shop.stock.filterNotNull()
            .map { card ->
                object : Command(ctx) {
                    override val title = ctx.describer.describeCard(card, concise = true)
                    override val extra = ctx.describer.describeCash(card.template.cost)
                    override val description = ctx.describer.describeCard(card.template)
                }
            }
}