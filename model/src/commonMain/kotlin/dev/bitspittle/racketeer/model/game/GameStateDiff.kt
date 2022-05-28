package dev.bitspittle.racketeer.model.game

import dev.bitspittle.limp.types.ListStrategy
import dev.bitspittle.limp.types.Logger
import dev.bitspittle.racketeer.model.building.BuildingProperty
import dev.bitspittle.racketeer.model.card.Card
import dev.bitspittle.racketeer.model.card.CardProperty
import dev.bitspittle.racketeer.model.shop.remaining
import dev.bitspittle.racketeer.model.text.Describer

/** Create a diff between two snapshots of a game state in time, useful for reporting changes to the user */
@Suppress("JoinDeclarationAndAssignment")
class GameStateDiff(val before: GameState, val after: GameState) {
    val changes = after.history.drop(before.history.size)
}

fun GameStateDiff.hasNoChanges() = changes.isEmpty()

fun GameStateDiff.reportTo(data: GameData, describer: Describer, logger: Logger) {
    GameStateDiffReporter(data, describer, this).reportTo(logger)
}

private class GameStateDiffReporter(
    private val data: GameData,
    private val describer: Describer,
    private val diff: GameStateDiff
) {
    private fun ListStrategy.toDesc(): String {
        return when (this) {
            ListStrategy.FRONT -> "to the front of"
            ListStrategy.BACK -> "into"
            ListStrategy.RANDOM -> "randomly into"
        }
    }

    private fun StringBuilder.report(change: GameStateChange.ShuffleDiscardIntoDeck) = change.apply {
        val discardDesc = describer.describePile(diff.before, diff.before.discard)
        val deckDesc = describer.describePile(diff.after, diff.after.deck)
        reportLine("${discardDesc.capitalize()} (${diff.before.discard.cards.size}) was reshuffled into $deckDesc to refill it.")
    }

    private fun StringBuilder.report(change: GameStateChange.Draw) = change.apply {
        val count = count!! // Count will always be set AFTER a Draw change is applied
        if (count > 0) {
            val deckDesc = describer.describePile(diff.before, diff.before.deck)
            val handDesc = describer.describePile(diff.after, diff.after.hand)
            if (count > 1) {
                reportLine("$count cards were drawn from $deckDesc into $handDesc.")
            } else {
                check(count == 1)
                reportLine("A card was drawn from $deckDesc into $handDesc.")
            }
        }
    }

    private fun StringBuilder.reportIfLastShopCard(card: Card) {
        if (diff.before.shop.stock.filterNotNull().any { it.id == card.id } &&
                diff.after.shop.remaining(card.template, data.rarities) == 0) {
            reportLine("The shop will not sell any more copied of ${card.template.name}.")
        }
    }

    private fun StringBuilder.report(change: GameStateChange.MoveCards) = change.apply {
        if (intoPile == diff.after.graveyard) {
            reportLine("${cards.size} cards were removed from the game.")
        } else {
            val pileToDesc = describer.describePile(diff.after, intoPile)
            cards
                .groupBy { card -> diff.before.pileFor(card) }
                // Ignore requests which end up moving a card back into its own pile; it looks weird.
                // This could happen as part of a bigger collection of cards across piles being moved.
                .filter { (pile, _) -> pile == null || pile.id != intoPile.id }
                .forEach { (pile, cards) ->
                    if (cards.size > 1) {
                        if (pile != null) {
                            val pileFromDesc = describer.describePile(diff.before, pile)
                            reportLine("${cards.size} cards moved from $pileFromDesc ${listStrategy.toDesc()} $pileToDesc.")
                        } else {
                            reportLine("${cards.size} cards were created and moved ${listStrategy.toDesc()} $pileToDesc.")
                        }
                        cards.forEach { card -> reportIfLastShopCard(card) }
                    } else {
                        report(GameStateChange.MoveCard(cards[0], intoPile, listStrategy))
                    }
                }
        }
    }

    private fun StringBuilder.report(change: GameStateChange.MoveCard) = change.apply {
        val cardTitle = card.template.name

        if (intoPile == diff.after.graveyard) {
            reportLine("$cardTitle was removed from the game.")
        } else {
            val pileToDesc = describer.describePile(diff.after, intoPile)

            val pileFrom = diff.before.pileFor(card)
            if (pileFrom != null) {
                // Ignore requests which end up moving a card within its own pile; it looks weird
                if (pileFrom.id != intoPile.id) {
                    val pileFromDesc = describer.describePile(diff.before, pileFrom)
                    reportLine("$cardTitle was moved from $pileFromDesc ${listStrategy.toDesc()} $pileToDesc.")
                }
            } else {
                reportLine("$cardTitle was created and moved ${listStrategy.toDesc()} $pileToDesc.")
            }
            reportIfLastShopCard(card)
        }
    }

    private fun StringBuilder.report(change: GameStateChange.Shuffle) = change.apply {
        val pileDesc = describer.describePile(diff.after, pile)
        reportLine("${pileDesc.capitalize()} was shuffled.")
    }

    private fun StringBuilder.report(change: GameStateChange.AddCardAmount) = change.apply {
        // Only report changes for owned cards. If a card is in the store or jail, the user shouldn't know about what's happening to it.
        if (!card.isOwned(diff.after)) return@apply

        when (property) {
            CardProperty.COUNTER -> {
                when {
                    amount > 0 -> reportLine("${card.template.name} added $amount counter(s).")
                    amount < 0 -> reportLine("${card.template.name} removed ${-amount} counter(s).")
                }
            }
            CardProperty.VP -> {
                when {
                    amount > 0 -> reportLine("${card.template.name} added ${describer.describeVictoryPoints(amount)}.")
                    amount < 0 -> reportLine("${card.template.name} lost ${describer.describeVictoryPoints(-amount)}.")
                }
            }
            CardProperty.VP_PASSIVE -> {
                when {
                    amount > 0 -> reportLine("${card.template.name} increased by ${describer.describeVictoryPoints(amount)}.")
                    amount < 0 -> reportLine("${card.template.name} decreased by ${describer.describeVictoryPoints(-amount)}.")
                }
            }
            else -> error("Unexpected card property: ${property}.")
        }
    }

    private fun StringBuilder.report(change: GameStateChange.UpgradeCard) = change.apply {
        reportLine("${card.template.name} was upgraded, adding: ${describer.describeUpgradeTitle(upgradeType, icons = false)}.")
    }

    private fun StringBuilder.report(change: GameStateChange.AddBuildingAmount) = change.apply {
        val name = building.blueprint.name
        when (property) {
            BuildingProperty.COUNTER -> {
                when {
                    amount > 0 -> reportLine("$name added $amount counter(s).")
                    amount < 0 -> reportLine("$name removed ${-amount} counter(s).")
                }
            }
            BuildingProperty.VP -> {
                when {
                    amount > 0 -> reportLine("$name added ${describer.describeVictoryPoints(amount)}.")
                    amount < 0 -> reportLine("$name lost ${describer.describeVictoryPoints(-amount)}.")
                }
            }
            BuildingProperty.VP_PASSIVE -> {
                when {
                    amount > 0 -> reportLine("$name increased by ${describer.describeVictoryPoints(amount)}.")
                    amount < 0 -> reportLine("$name decreased by ${describer.describeVictoryPoints(-amount)}.")
                }
            }
            else -> error("Unexpected building property: ${property}.")
        }
    }

    private fun StringBuilder.report(change: GameStateChange.AddEffect) = change.apply {
        reportLine("You added the following effect onto the street:\n  ${effect.desc ?: effect.warningExpr}")
    }

    private fun StringBuilder.report(change: GameStateChange.RestockShop) = change.apply {
        reportLine(buildString {
            append("The shop was restocked.")
            if (diff.after.shop.stock.any { it == null }) {
                append(" Due to supply issues, some shelves are empty.")
            }
        })
    }

    private fun StringBuilder.report(change: GameStateChange.UpgradeShop) = change.apply {
        reportLine("The shop was upgraded.")
    }

    private fun StringBuilder.report(change: GameStateChange.AddBlueprint) = change.apply {
        reportLine("You added the following blueprint: ${change.blueprint.name}.")
    }

    private fun StringBuilder.report(change: GameStateChange.Build) = change.apply {
        val blueprint = diff.before.blueprints[change.blueprintIndex]
        reportLine("${blueprint.name} was built.")
    }

    fun reportTo(logger: Logger) {
        val report = buildString {
            val changes = diff.changes.toMutableList()

            // Convert "MoveCards" to "MoveCard" when possible, it reads better
            for (i in changes.indices) {
                val change = changes[i]
                if (change is GameStateChange.MoveCards && change.cards.size == 1) {
                    changes[i] = GameStateChange.MoveCard(change.cards[0], change.intoPile, change.listStrategy)
                }
            }

            // Sometimes cards can bounce around quickly due to init actions. Lets marge those together...
            changes
                .filterIsInstance<GameStateChange.MoveCard>()
                .groupBy { it.card }
                .filter { (_, cardMoves) -> cardMoves.size > 1 }
                .forEach { (_, cardMoves) ->
                    val transientMoves = cardMoves.dropLast(1)
                    changes.removeAll { transientMoves.contains(it) }
                }

            changes.forEach { change ->
                when (change) {
                    is GameStateChange.GameStarted -> Unit // Marker game state, no need to report
                    is GameStateChange.ShuffleDiscardIntoDeck -> report(change)
                    is GameStateChange.Draw -> report(change)
                    is GameStateChange.Play -> Unit // No need to report, obvious from user actions
                    is GameStateChange.MoveCards -> report(change)
                    is GameStateChange.MoveCard -> report(change)
                    is GameStateChange.Shuffle -> report(change)
                    is GameStateChange.AddCardAmount -> report(change)
                    is GameStateChange.UpgradeCard -> report(change)
                    is GameStateChange.AddBuildingAmount -> report(change)
                    is GameStateChange.AddGameAmount -> Unit // Reported below, in aggregate
                    is GameStateChange.SetGameData -> Unit // Game data is only for use by the designers, don't report it
                    is GameStateChange.AddEffect -> report(change)
                    is GameStateChange.RestockShop -> report(change)
                    is GameStateChange.UpgradeShop -> report(change)
                    is GameStateChange.AddBlueprint -> report(change)
                    is GameStateChange.Build -> report(change)
                    is GameStateChange.Activate -> Unit // No need to report, obvious from user actions
                    is GameStateChange.EndTurn -> Unit // No need to report, obvious from user actions
                    is GameStateChange.GameOver -> Unit // Marker game state, no need to report
                }
            }

            // Game resource changes feel better reported in aggregate

            // Hand size message looks better when presented as a diff (absolute is more interesting than relative):
            (diff.after.handSize - diff.before.handSize).let { handSizeDiff ->
                when {
                    handSizeDiff > 0 -> reportLine("Your hand size grew from ${diff.before.handSize} to ${diff.after.handSize} cards.")
                    handSizeDiff < 0 -> reportLine("Your hand size shrunk from ${diff.before.handSize} to ${diff.after.handSize} cards.")
                }
            }

            (diff.after.cash - diff.before.cash).let { amount ->
                when {
                    amount > 0 -> reportLine("You earned ${describer.describeCash(amount)}.")
                    amount < 0 -> reportLine("You spent ${describer.describeCash(-amount)}.")
                }
            }

            (diff.after.influence - diff.before.influence).let { amount ->
                when {
                    amount > 0 -> reportLine("You earned ${describer.describeInfluence(amount)}.")
                    amount < 0 -> reportLine("You spent ${describer.describeInfluence(-amount)}.")
                }
            }

            (diff.after.luck - diff.before.luck).let { amount ->
                when {
                    amount > 0 -> reportLine("You earned ${describer.describeLuck(amount)}.")
                    amount < 0 -> reportLine("You spent ${describer.describeLuck(-amount)}.")
                }
            }

            // VP is passively calculated, so we have to manually check it
            (diff.after.vp - diff.before.vp).let { vpDiff ->
                when {
                    vpDiff > 0 -> reportLine("You gained ${describer.describeVictoryPoints(vpDiff)}.")
                    vpDiff < 0 -> reportLine("You lost ${describer.describeVictoryPoints(-vpDiff)}.")
                }
            }
        }

        if (report.isNotEmpty()) {
            logger.info(report)
        }
    }

    private fun StringBuilder.reportLine(message: String) {
        if (this.isNotEmpty()) {
            appendLine()
        }
        append("- $message")
    }
}
