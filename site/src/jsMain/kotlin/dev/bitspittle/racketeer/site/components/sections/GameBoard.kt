package dev.bitspittle.racketeer.site.components.sections

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.UserSelect
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.layout.SimpleGrid
import com.varabyte.kobweb.silk.components.layout.numColumns
import com.varabyte.kobweb.silk.components.text.SpanText
import dev.bitspittle.racketeer.model.game.GameProperty
import dev.bitspittle.racketeer.model.game.GameStateChange
import dev.bitspittle.racketeer.model.game.isGameOver
import dev.bitspittle.racketeer.site.components.widgets.Card
import dev.bitspittle.racketeer.site.components.widgets.CardGroup
import dev.bitspittle.racketeer.site.components.widgets.CardPile
import dev.bitspittle.racketeer.site.model.GameContext
import dev.bitspittle.racketeer.site.model.runStateChangingAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

private val GAP = 20.px

@Composable
fun GameBoard(scope: CoroutineScope, ctx: GameContext, onContextUpdated: () -> Unit) {
    fun runStateChangingActions(vararg blocks: suspend () -> Unit) {
        scope.launch {
            ctx.logger.clear()

            var changed = false
            for (block in blocks) {
                if (ctx.runStateChangingAction { block() }) { changed = true }
            }

            if (changed) {
                onContextUpdated()
            }
        }
    }
    fun runStateChangingAction(block: suspend () -> Unit) = runStateChangingActions(block)

    // UserSelect.None, because the game feels cheap if you allow users to drag highlight text on stuff
    Box(Modifier.fillMaxSize().userSelect(UserSelect.None)) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier
                .align(Alignment.CenterHorizontally)
                .margin(top = 10.px, bottom = 15.px)
                .gap(30.px)
            ) {
                SpanText("Turn ${ctx.state.turn + 1}")
                SpanText(
                    ctx.describer.describeCash(ctx.state.cash) + " "
                            + ctx.describer.describeInfluence(ctx.state.influence) + " "
                            + ctx.describer.describeLuck(ctx.state.luck),
                )
                SpanText(ctx.describer.describeVictoryPoints(ctx.state.vp))
            }

            SimpleGrid(
                numColumns(2),
                Modifier
                    .fillMaxSize()
                    .gap(GAP).padding(GAP)
                    .gridTemplateColumns("auto 1fr")
            ) {
                Div() // Empty space
                Row(Modifier.gap(GAP)) {
                    CardGroup("Shop (Tier ${ctx.state.shop.tier + 1})", Modifier.flexGrow(1)) {}
                    Column(Modifier
                        .fillMaxHeight()
                        .padding(GAP).gap(GAP)
                        .border(width = 1.px, style = LineStyle.Solid, color = Colors.Black)
                    ) {
                        val shopPrice = ctx.data.shopPrices.getOrNull(ctx.state.shop.tier)
                        Button(
                            onClick = {
                                runStateChangingAction {
                                    ctx.state.apply(GameStateChange.UpgradeShop())
                                    // shopPrice to be non-null if button is enabled
                                    @Suppress("NAME_SHADOWING") val shopPrice = shopPrice!!
                                    ctx.state.apply(GameStateChange.AddGameAmount(GameProperty.INFLUENCE, -shopPrice))
                                }
                            },
                            Modifier.width(100.px).flexGrow(1),
                            enabled = shopPrice != null && ctx.state.influence >= shopPrice
                        ) {
                            Text("Expand"); Br()
                            if (shopPrice != null) {
                                Text(ctx.describer.describeInfluence(ctx.data.shopPrices[ctx.state.shop.tier]))
                            } else {
                                Text("MAX")
                            }
                        }
                        Button(
                            onClick = {
                                runStateChangingAction {
                                    ctx.state.apply(GameStateChange.RestockShop())
                                    ctx.state.apply(GameStateChange.AddGameAmount(GameProperty.LUCK, -1))
                                }
                            },
                            Modifier.width(100.px).flexGrow(1),
                            enabled = ctx.state.luck > 0
                        ) {
                            Text("Reroll"); Br()
                            Text(ctx.data.icons.luck)
                        }
                    }
                }

                CardPile(ctx, ctx.state.discard)
                CardGroup("Street") {
                    ctx.state.street.cards.forEach { card ->
                        Card(ctx, card, onClick = {})
                    }
                }

                CardPile(ctx, ctx.state.deck)
                CardGroup("Hand") {
                    ctx.state.hand.cards.forEach { card ->
                        Card(ctx, card, onClick = {
                            runStateChangingAction {
                                ctx.state.apply(GameStateChange.Play(card))
                            }
                        })
                    }
                }

                CardPile(ctx, ctx.state.jail)
                Row(Modifier.gap(GAP)) {
                    CardGroup("Buildings", Modifier.flexGrow(1)) {}
                    Button(
                        onClick = {
                            runStateChangingActions(
                                {
                                    ctx.state.apply(GameStateChange.EndTurn())
                                },
                                // Break up into two state changing actions for a better state diff report around reshuffling cards
                                {
                                    if (!ctx.state.isGameOver) {
                                        ctx.state.apply(GameStateChange.Draw())
                                    }
                                }
                            )
                        },
                        Modifier.width(300.px).fillMaxHeight(),
                        enabled = !ctx.state.isGameOver
                    ) {
                        Text("End Turn")
                    }
                }
            }

            ctx.logger.messages.forEach { message ->
                SpanText(message, Modifier.fillMaxWidth().padding(left = GAP))
            }

        }
    }
}