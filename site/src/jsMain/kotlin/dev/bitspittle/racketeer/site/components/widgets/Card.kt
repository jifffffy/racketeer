package dev.bitspittle.racketeer.site.components.widgets

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.asAttributesBuilder
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.graphics.Color.Companion.rgb
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.graphics.toCssColor
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.style.*
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.theme.SilkTheme
import dev.bitspittle.racketeer.site.GAME_TEXT_FONT
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

private val CardStyleCommon =
    Modifier
        .fontFamily(GAME_TEXT_FONT)
        .fontSize(20.px)
        .borderRadius(10.percent)
        .backgroundColor(rgb(0xc9c1c1))
        .color(Colors.Black)

val CardStyleMinimum = ComponentStyle.base("card-min") {
    CardStyleCommon
        .width(150.px).height(210.px)
}

val PortraitStyle = ComponentStyle.base("portrait") {
    Modifier
        .border(width = 5.px, style = LineStyle.Solid, color = rgb(0x968f8f).toCssColor())
        .borderRadius(2.percent)
        .backgroundColor(Colors.White)

}

enum class CardLayout {
    MINIMAL,
    FULL
}

@Composable
fun Card(modifier: Modifier = Modifier, layout: CardLayout = CardLayout.MINIMAL) {
    val finalModifier = CardStyleMinimum.toModifier().then(modifier)

    Column(finalModifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(33.px), contentAlignment = Alignment.Center) {
            SpanText("Embezzler")
        }
        Box(PortraitStyle.toModifier()) {
            Img("/portraits/cards/Embezzler.png", attrs = Modifier
                .size(128.px)
                .styleModifier {
                    property("image-rendering", "pixelated")
                }
                .asAttributesBuilder())
        }
        Box(Modifier.fillMaxWidth().height(33.px), contentAlignment = Alignment.TopCenter) {
            SpanText("\uD83D\uDCB0 \uD83E\uDD1D",
                // Vertically aligning text isn't quite working so I'll use margin to tweak the look
                Modifier
                    .margin(7.px)
                    .styleModifier {
                    property("text-shadow", "0px 0px 4px #000000")
                })
        }
    }
}