package dev.bitspittle.racketeer.console.command.commands.system.unlock

import dev.bitspittle.racketeer.console.command.Command
import dev.bitspittle.racketeer.console.game.GameContext
import dev.bitspittle.racketeer.console.user.totalVp
import dev.bitspittle.racketeer.model.game.Unlock

class ViewUnlockCommand(ctx: GameContext, private val unlock: Unlock) : Command(ctx) {
    private val totalVp = ctx.userStats.games.totalVp

    override val type = when {
        !unlock.isUnlocked(ctx) -> Type.Disabled
        else -> Type.Normal
    }

    override val title = unlock.name
    override val extra = ctx.describer.describeVictoryPoints(unlock.vp)
    override val description = buildString {
        append(unlock.description)
        if (type == Type.Disabled) {
            append("\n\n")
            append("You will unlock this feature after earning ${unlock.vp - totalVp} more victory point(s).")
        }
    }
}
