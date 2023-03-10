package dev.bitspittle.racketeer.console.view.views.system

import dev.bitspittle.racketeer.console.command.Command
import dev.bitspittle.racketeer.console.command.commands.system.NewGameCommand
import dev.bitspittle.racketeer.console.game.GameContext
import dev.bitspittle.racketeer.console.game.playtestId
import dev.bitspittle.racketeer.console.game.version
import dev.bitspittle.racketeer.console.user.GameCancelReason
import dev.bitspittle.racketeer.console.user.GameStats
import dev.bitspittle.racketeer.console.user.saveInto
import dev.bitspittle.racketeer.console.utils.CloudFileService
import dev.bitspittle.racketeer.console.utils.encodeToYaml
import dev.bitspittle.racketeer.console.view.View
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ConfirmRestartView(ctx: GameContext) : View(ctx) {
    override val showUpdateMessage = true // Let the user know there's a new version BEFORE they start a new game

    override fun createCommands(): List<Command> = listOf(
        object : Command(ctx) {
            override val type = Type.Danger
            override val title = "Confirm"

            override val description = "Press ENTER if you're sure you want to restart this game. Otherwise, go back!"

            override suspend fun invoke(): Boolean {
                // The user restarted. Why? Let's send some data, maybe we can find out.
                if (!ctx.settings.admin.enabled) {
                    ctx.app.cloudFileService.upload(
                        buildString {
                            append("versions:${ctx.app.version}:")
                            append("users:${ctx.app.playtestId}:")
                            val utcNow =
                                Instant.now().atOffset(ZoneOffset.UTC)
                                    .format(DateTimeFormatter.ofPattern("MM-dd-yyyy|HH:mm:ss"))
                            append("restarts:$utcNow:")
                            append("turn:${ctx.state.turn}:vp:${ctx.state.vp}")
                            append(".yaml")
                        },
                        CloudFileService.MimeTypes.YAML
                    ) { ctx.encodeToYaml() }
                }

                ctx.userStats.games.add(GameStats.from(ctx.state, GameCancelReason.RESTARTED))
                ctx.userStats.games.saveInto(ctx.app.userDataDir)

                return NewGameCommand(ctx).invoke()
            }
        }
    )
}