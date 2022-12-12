package dev.bitspittle.racketeer.site.components.widgets

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.Cursor
import com.varabyte.kobweb.compose.css.FontStyle
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Spacer
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Color.Companion.rgb
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.silk.components.style.*
import com.varabyte.kobweb.silk.components.text.SpanText
import dev.bitspittle.racketeer.model.card.Card
import dev.bitspittle.racketeer.site.G
import dev.bitspittle.racketeer.site.model.GameContext
import org.jetbrains.compose.web.css.*

private val CardStyleCommon =
    Modifier
//        .fontFamily(G.Font.NAME)
        .fontSize(G.Font.Sizes.Normal)
        .padding(topBottom = 5.px)
        .borderRadius(10.percent)
        .backgroundColor(rgb(0xc9c1c1))
        .color(Colors.Black)
        .outlineStyle(LineStyle.None)

private val CardStyleCommonHover =
    Modifier
        .boxShadow(blurRadius = 10.px, spreadRadius = 2.px, color = Colors.Yellow)
        .cursor(Cursor.Pointer)

private val CardStyleCommonFocus =
    Modifier
        .boxShadow(blurRadius = 10.px, spreadRadius = 2.px, color = Colors.Red)


val CardStyleMinimal = ComponentStyle("card-min") {
    base {
        CardStyleCommon
            .width(150.px).flexShrink(0) // Needed to prevent Row from resizing the elements
            .height(210.px)
    }

    hover {
        CardStyleCommonHover
    }

    focus {
        CardStyleCommonFocus
    }

}

val CardDescriptionStyle = ComponentStyle.base("card-desc") {
    Modifier
        .fontSize(G.Font.Sizes.Small)
        .padding(topBottom = 10.px, leftRight = 15.px)
}

val CardDescriptionFlavorVariant = CardDescriptionStyle.addVariantBase("flavor") {
    Modifier.fontStyle(FontStyle.Italic)
}

val CardDescriptionAbilityVariant = CardDescriptionStyle.addVariantBase("ability") {
    Modifier.styleModifier {
        property("text-shadow", "0px 0px 4px #000000")
    }
}

enum class CardLayout {
    MINIMAL,
    FULL
}

@Composable
fun Card(ctx: GameContext, card: Card, onClick: () -> Unit, modifier: Modifier = Modifier, layout: CardLayout = CardLayout.MINIMAL) {
    Column(CardStyleMinimal.toModifier().tabIndex(0).onClick { onClick() }.then(modifier)) {
        Box(Modifier.fillMaxWidth().height(33.px), contentAlignment = Alignment.Center) {
            SpanText(card.template.name)
        }
        Spacer()
        card.template.description.flavor?.let { flavor ->
            SpanText(flavor, CardDescriptionStyle.toModifier(CardDescriptionFlavorVariant))
        }
        SpanText(
            ctx.describer.convertIcons(card.template.description.ability),
            CardDescriptionStyle.toModifier(CardDescriptionAbilityVariant)
        )
    }
}