package dev.bitspittle.racketeer.console.view.views.game.buildings

import com.varabyte.kotter.foundation.input.CharKey
import com.varabyte.kotter.foundation.input.Key
import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.MainRenderScope
import com.varabyte.kotter.runtime.render.RenderScope
import dev.bitspittle.racketeer.console.command.Command
import dev.bitspittle.racketeer.console.command.commands.buildings.ViewBlueprintCommand
import dev.bitspittle.racketeer.console.game.GameContext
import dev.bitspittle.racketeer.console.utils.BlueprintSearcher
import dev.bitspittle.racketeer.console.view.View
import dev.bitspittle.racketeer.model.building.Blueprint

fun Blueprint.shouldMask(ctx: GameContext) = !ctx.userStats.buildings.contains(this.name)

class BuildingListView(ctx: GameContext, private var sortingOrder: SortingOrder = SortingOrder.NAME) : View(ctx) {
    enum class SortingOrder {
        COST,
        NAME;

        fun next(): SortingOrder {
            return SortingOrder.values()[(this.ordinal + 1) % SortingOrder.values().size]
        }
    }

    private val blueprints = ctx.data.blueprints.sortedBy { it.name }

    private val blueprintSearcher = BlueprintSearcher(blueprints.filter { !it.shouldMask(ctx) })
    private var searchPrefix = ""

    override fun createCommands(): List<Command> =
        blueprints
            .let { blueprints ->
                when (sortingOrder) {
                    SortingOrder.COST -> blueprints.sortedBy { it.buildCost.cash + it.buildCost.influence }
                    SortingOrder.NAME -> blueprints // Already sorted by name
                }
            }
            .map { ViewBlueprintCommand(ctx, it) }

    override suspend fun handleAdditionalKeys(key: Key): Boolean {
        return if (sortingOrder == SortingOrder.NAME && (key is CharKey && (key.code.isLetter() || key.code == ' '))) {
            searchPrefix += key.code.lowercase()
            currIndex = blueprintSearcher.search(searchPrefix)?.let { found -> blueprints.indexOf(found) } ?: 0
            true
        } else if (sortingOrder == SortingOrder.NAME && key == Keys.BACKSPACE) {
            searchPrefix = searchPrefix.dropLast(1)
            true
        } else if (key == Keys.TAB) {
            currIndex = 0
            sortingOrder = sortingOrder.next()
            searchPrefix = ""
            refreshCommands()
            true
        } else {
            false
        }
    }

    private val numBuildingsBuilt = ctx.data.blueprints.count { ctx.userStats.buildings.contains(it.name) }
    override fun MainRenderScope.renderContentUpper() {
        yellow { textLine("You have built $numBuildingsBuilt out of ${ctx.data.blueprints.size} buildings.") }
        textLine()

        textLine("Sorted by: ${sortingOrder.name.lowercase().capitalize()}")
        textLine()

        if (searchPrefix.isNotEmpty()) {
            black(isBright = true) { textLine("Search: " + searchPrefix.lowercase()) }
            textLine()
        }
    }

    override fun RenderScope.renderFooterUpper() {
        val sortingWord = sortingOrder.next().name.lowercase()
        text("Press "); cyan { text("TAB") }; textLine(" to sort by $sortingWord.")
        if (sortingOrder == SortingOrder.NAME) {
            text("Press "); cyan { text("A-Z") }; textLine(" to jump to buildings by name.")
        }
    }
}