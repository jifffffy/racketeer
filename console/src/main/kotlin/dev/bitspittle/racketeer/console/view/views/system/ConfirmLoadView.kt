package dev.bitspittle.racketeer.console.view.views.system

import dev.bitspittle.racketeer.console.game.GameContext
import dev.bitspittle.racketeer.console.command.Command
import dev.bitspittle.racketeer.console.command.commands.system.NewGameCommand
import dev.bitspittle.racketeer.console.command.commands.system.UserDataSupport
import dev.bitspittle.racketeer.console.view.popAll
import dev.bitspittle.racketeer.console.view.views.game.GameView
import dev.bitspittle.racketeer.console.view.views.game.PlayCardsView
import dev.bitspittle.racketeer.console.view.views.game.PreDrawView
import dev.bitspittle.racketeer.model.snapshot.GameSnapshot
import net.mamoe.yamlkt.Yaml
import kotlin.io.path.readText

class ConfirmLoadView(ctx: GameContext, private val slot: Int) : GameView(ctx) {
    override fun createCommands(): List<Command> = listOf(
        object : Command(ctx) {
            override val type = Type.Warning
            override val title = "Confirm"

            override val description = "Press ENTER if you're sure you want to load the data in save slot #${slot + 1}. Otherwise, go back!"

            override suspend fun invoke(): Boolean {
                val path = UserDataSupport.pathForSlot(slot)
                val snapshot = Yaml.decodeFromString(GameSnapshot.serializer(), path.readText())
                snapshot.create(ctx.data, ctx.env, ctx.cardQueue) { state ->
                    ctx.state = state
                }

                ctx.viewStack.popAll()
                if (snapshot.isPreDraw) {
                    ctx.viewStack.replaceView(PreDrawView(ctx))
                } else {
                    ctx.viewStack.replaceView(PlayCardsView(ctx))
                }

                if (slot >= 0) {
                    ctx.app.logger.info("Slot #${slot + 1} successfully loaded!")
                }
                return true
            }
        }
    )
}