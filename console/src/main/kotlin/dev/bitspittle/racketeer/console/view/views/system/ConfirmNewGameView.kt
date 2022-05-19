package dev.bitspittle.racketeer.console.view.views.system

import dev.bitspittle.racketeer.console.command.Command
import dev.bitspittle.racketeer.console.command.commands.system.NewGameCommand
import dev.bitspittle.racketeer.console.game.GameContext
import dev.bitspittle.racketeer.console.user.GameCancelReason
import dev.bitspittle.racketeer.console.user.GameStats
import dev.bitspittle.racketeer.console.user.saveInto
import dev.bitspittle.racketeer.console.view.View

class ConfirmNewGameView(ctx: GameContext) : View(ctx) {
    override fun createCommands(): List<Command> = listOf(
        object : Command(ctx) {
            override val type = Type.Warning
            override val title = "Confirm"

            override val description = "Once you confirm, the existing quick save from your last game will be deleted. If you don't want this to happen, go back!"

            override suspend fun invoke(): Boolean {
                ctx.userStats.games.add(GameStats(ctx.state.vp, cancelReason = GameCancelReason.ABORTED))
                ctx.userStats.games.saveInto(ctx.app.userDataDir)

                return NewGameCommand(ctx).invoke()
            }
        }
    )
}