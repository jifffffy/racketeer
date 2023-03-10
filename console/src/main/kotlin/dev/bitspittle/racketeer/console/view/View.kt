package dev.bitspittle.racketeer.console.view

import com.varabyte.kotter.foundation.input.Key
import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.MainRenderScope
import com.varabyte.kotter.runtime.render.RenderScope
import com.varabyte.kotterx.decorations.BorderCharacters
import com.varabyte.kotterx.decorations.bordered
import dev.bitspittle.racketeer.console.command.Command
import dev.bitspittle.racketeer.console.game.GameContext
import dev.bitspittle.racketeer.console.game.playtestId
import dev.bitspittle.racketeer.console.game.version
import dev.bitspittle.racketeer.console.utils.CloudFileService
import dev.bitspittle.racketeer.console.utils.UploadThrottleCategory
import dev.bitspittle.racketeer.console.utils.encodeToYaml
import dev.bitspittle.racketeer.console.utils.wrap
import dev.bitspittle.racketeer.console.view.views.admin.AdminMenuView
import dev.bitspittle.racketeer.console.view.views.game.cards.BrowsePilesView
import dev.bitspittle.racketeer.console.view.views.game.history.ReviewHistoryView
import dev.bitspittle.racketeer.console.view.views.game.play.GameSummaryView
import dev.bitspittle.racketeer.console.view.views.game.play.PlayCardsView
import dev.bitspittle.racketeer.console.view.views.system.OptionsMenuView
import dev.bitspittle.racketeer.model.game.GameStateStub
import dev.bitspittle.racketeer.model.game.allPiles

abstract class View(protected val ctx: GameContext, private val initialCurrIndex: Int = 0) {
    // region Commands
    protected abstract fun createCommands(): List<Command>
    private var shouldRefreshCommands = true
    private var _commandsSection: CommandsSection? = null
    private val commandsSection: CommandsSection
        get() {
            if (shouldRefreshCommands) {
                shouldRefreshCommands = false

                _commandsSection?.let { cs ->
                    val newIndex = refreshCursorPosition(cs.currIndex, cs.currCommand)
                    _commandsSection = CommandsSection(createCommands(), newIndex)
                } ?: run {
                    _commandsSection = CommandsSection(createCommands(), initialCurrIndex)
                }
            }
            return _commandsSection!!
        }

    private val currCommand get() = commandsSection.currCommand
    protected var currIndex
        get() = commandsSection.currIndex
        set(value) {
            commandsSection.currIndex = value
        }

    fun refreshCommands() {
        shouldRefreshCommands = true
    }
    // endregion

    protected open val title: String? = null
    protected open val subtitle: String? = null
    protected open val heading: String? = null

    protected open val showUpdateMessage: Boolean = false
    protected open val allowEsc: Boolean = true
    protected open val allowBrowseCards: Boolean = true

    private fun hasGameStarted() = ctx.state !== GameStateStub && ctx.state.allPiles.any { it.cards.isNotEmpty() }

    private fun allowAdminAccess(): Boolean {
        if (!allowEsc) return false
        if (!hasGameStarted()) return false
        return ctx.settings.admin.enabled && !(ctx.viewStack.contains { view -> view is AdminMenuView })
    }

    private fun allowBrowsingCards(): Boolean {
        if (!hasGameStarted()) return false
        return allowBrowseCards && ctx.viewStack.contains { view -> (view is PlayCardsView || view is GameSummaryView) } && !ctx.viewStack.contains { view -> view is BrowsePilesView || view is ReviewHistoryView }
    }

    private suspend fun doHandleKeys(key: Key): Boolean {
        return when (key) {
            Keys.ESC -> {
                if (allowEsc) {
                    if (ctx.viewStack.canGoBack) {
                        onEscRequested()
                        goBack()
                    } else {
                        ctx.viewStack.pushView(OptionsMenuView(ctx))
                    }
                    true
                } else {
                    false
                }
            }
            Keys.TICK, Keys.TILDE -> {
                if (allowAdminAccess()) {
                    ctx.viewStack.pushView(AdminMenuView(ctx))
                    true
                } else false
            }
            Keys.BACKSLASH -> {
                if (allowBrowsingCards()) {
                    ctx.viewStack.pushView(BrowsePilesView(ctx))
                    true
                } else false
            }
            Keys.EQUALS -> {
                // Lazily re-use the same check for browsing cards; it should be good enough and avoids duplicating code
                if (allowBrowsingCards()) {
                    ctx.viewStack.pushView(ReviewHistoryView(ctx))
                    true
                } else false
            }

            else -> currCommand.handleKey(key) || handleAdditionalKeys(key)
        }
    }

    protected open suspend fun handleAdditionalKeys(key: Key): Boolean = false

    /////


    /**
     * Give child views a chance to influence the new cursor position after [refreshCommands] is called.
     *
     * By default, the cursor stays in its old position.
     */
    protected open fun refreshCursorPosition(oldIndex: Int, oldCommand: Command): Int = oldIndex

    protected fun goBack() {
        ctx.viewStack.popView()
        // Refresh commands in case the screen we were in caused a change
        ctx.viewStack.currentView.refreshCommands()
    }

    private inline fun runUnsafeCode(block: () -> Unit) {
        try {
            block()
        } catch (ex: Exception) {
            ctx.app.logger.error(ex.message ?: "Code threw exception without a message: ${ex::class.simpleName}")

            // At this point, let's try to send an automatic crash report. We wrap it in an aggressive try/catch block
            // though because there's nothing more fun than throwing an exception while trying to handle an exception
            // NOTE: Admin players are probably messing with the game and their crashes are probably just distractions
            // as they experiment with cards, so only send data for regular players
            if (!ctx.settings.admin.enabled) {
                try {
                    val filename = run {
                        val viewName = this::class.simpleName!!.lowercase().removeSuffix("view")
                        val command = currCommand.title
                            .map { if (it.isLetterOrDigit()) it else '_' }
                            .joinToString("")
                            // Collapse all underscores and make sure any of them don't show up in weird places
                            .replace(Regex("__+"), "_")
                            .trim('_')
                            .lowercase()
                        "versions:${ctx.app.version}:users:${ctx.app.playtestId}:crashes:$viewName-$command.yaml"
                    }
                    ctx.app.cloudFileService.upload(
                        filename,
                        CloudFileService.MimeTypes.YAML,
                        throttleKey = UploadThrottleCategory.CRASH_REPORT,
                    ) { ctx.encodeToYaml() }
                } catch (ignored: Throwable) {
                }
            }
        }
    }

    suspend fun handleKey(key: Key): Boolean {
        return when (key) {
            Keys.ENTER -> {
                if (currCommand.type !in listOf(Command.Type.Disabled, Command.Type.Hidden, Command.Type.Blocked)) {
                    runUnsafeCode { currCommand.invoke() }
                    true
                } else {
                    // Blocked should still consider the key handled; disabled does not
                    return currCommand.type == Command.Type.Blocked
                }
            }

            else -> doHandleKeys(key) || commandsSection.handleKey(key)
        }
    }

    suspend fun handleInputChanged(input: String) = runUnsafeCode {
        doHandleInputChanged(input)
    }
    suspend fun handleInputEntered(input: String, clearInput: () -> Unit) = runUnsafeCode {
        doHandleInputEntered(input, clearInput)
    }
    protected open suspend fun doHandleInputChanged(input: String) = Unit
    protected open suspend fun doHandleInputEntered(input: String, clearInput: () -> Unit) = Unit

    fun renderInto(scope: MainRenderScope) {
        scope.apply {
            renderHeader()

            renderContentUpper()
            currCommand.renderContentUpperInto(this)
            commandsSection.renderInto(this)
            currCommand.renderContentLowerInto(this)
            renderContentLower()

            commandsSection.currCommand.description?.let { description ->
                bordered(borderCharacters = BorderCharacters.CURVED, paddingLeftRight = 1) {
                    text(description.wrap())
                }
                textLine()
            }

            renderFooterUpper()
            currCommand.renderFooterUpperInto(this)
            renderFooter()
            currCommand.renderFooterLowerInto(this)
            renderFooterLower()

            if (showUpdateMessage && ctx.app.isUpdateAvailable) {
                textLine()
                yellow {
                    textLine("A new version of the game is available. Check your email for instructions.".wrap())
                }
            }

        }
    }

    /** Content (with possible `input()`) rendered just above all commands */
    protected open fun MainRenderScope.renderContentUpper() = Unit
    /** Content (with possible `input()`) rendered just below all commands */
    protected open fun MainRenderScope.renderContentLower() = Unit
    protected open fun RenderScope.renderFooterUpper() = Unit
    protected open fun RenderScope.renderFooterLower() = Unit

    private fun RenderScope.renderHeader() {
        textLine() // Give the top line some breathing space from the prompt

        val state = ctx.state

        if (ctx.state !== GameStateStub) {
            text(ctx.describer.describeCash(state.cash))
            text(' ')
            text(ctx.describer.describeInfluence(state.influence))
            text(' ')
            text(ctx.describer.describeLuck(state.luck))
            text(' ')
            text(ctx.describer.describeVictoryPoints(state.vp))
            textLine()

            textLine()
            scopedState {
                val numRemainingTurns = state.numTurns - state.turn
                if (numRemainingTurns == 1) red() else if (numRemainingTurns <= 4) yellow()
                bold { textLine("Turn ${state.turn + 1} out of ${state.numTurns}") }
            }
            textLine()
        }

        title?.let { title ->
            bold { textLine(title.uppercase()) }
            textLine()
        }
        subtitle?.let { subtitle ->
            underline { textLine(subtitle) }
            textLine()
        }
        heading?.let { heading ->
            textLine(heading)
            textLine()
        }
    }

    private fun RenderScope.renderFooter() {
        if (allowBrowsingCards()) {
            text("Press "); cyan { text("\\") }; textLine(" to browse all card piles.")
            text("Press "); cyan { text("=") }; textLine(" to review game history.")
        }
        if (allowAdminAccess()) {
            text("Press "); cyan { text("~") }; textLine(" to access the admin menu.")
        }
        text("Press "); cyan { text("UP/DOWN") }; text(", "); cyan { text("HOME/END") }; text(", and "); cyan { text("PGUP/PGDN") }; textLine(" to navigate choices.")
        if (allowEsc) {
            text("Press "); cyan { text("ESC") }
            if (ctx.viewStack.canGoBack) textLine(" to go back.") else textLine(" to open options.")
        }
    }

    internal open suspend fun onEntered() = Unit
    protected open fun onEscRequested() = Unit
}

