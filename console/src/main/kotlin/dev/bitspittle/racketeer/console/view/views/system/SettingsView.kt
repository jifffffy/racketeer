package dev.bitspittle.racketeer.console.view.views.system

import dev.bitspittle.racketeer.console.command.Command
import dev.bitspittle.racketeer.console.command.commands.game.choose.SelectItemCommand
import dev.bitspittle.racketeer.console.game.GameContext
import dev.bitspittle.racketeer.console.user.Settings
import dev.bitspittle.racketeer.console.user.saveInto
import dev.bitspittle.racketeer.console.view.View

class SettingsView(ctx: GameContext, categories: List<Category>) : View(ctx) {
    enum class Category {
        ADMIN,
    }

    private class Entry(
        ctx: GameContext,
        name: String,
        desc: String,
        get: Settings.() -> Boolean,
        val set: Settings.(Boolean) -> Unit,
        type: Command.Type = Command.Type.Normal
    ) {
        val command = SelectItemCommand(
            ctx,
            name,
            selected = ctx.settings.get(),
            description = desc,
            type = type
        )
    }

    private var category = categories.first()

    private val entries = mutableMapOf(
        Category.ADMIN to listOf(
            Entry(
                ctx,
                "Show code",
                "Set true to surface game scripts in the UI.",
                { admin.showCode },
                { value -> admin.showCode = value },
            ),
            Entry(
                ctx,
                "Enable admin features",
                "Uncheck this to disable access to the admin menu and clear other admin-specific features.\n" +
                        "\n" +
                        "You can restore them by going into the options mode and typing \"thegodfather\".",
                { admin.enabled },
                { value -> admin.enabled = value },
            )
        )
    )

    private fun createNewSettings() = Settings().apply {
        setFrom(ctx.settings)
        entries.values.flatten().forEach { entry ->
            entry.set(this, entry.command.selected)
        }
    }

    override fun createCommands(): List<Command> =
        entries.getValue(category).map { it.command } +
                object : Command(ctx) {
                    override val type get() = if (createNewSettings() != ctx.settings) Type.Normal else Type.Disabled
                    override val title: String = "Confirm"
                    override val description = "Press ENTER to confirm the above choice(s)."

                    override suspend fun invoke(): Boolean {
                        ctx.settings.setFrom(createNewSettings())
                        ctx.settings.saveInto(ctx.app.userDataDir)
                        ctx.app.logger.info("User settings updated and saved!")
                        ctx.viewStack.popView()
                        return true
                    }
                }
}
