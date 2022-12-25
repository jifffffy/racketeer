package dev.bitspittle.racketeer.site.components.widgets

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.Overflow
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.ColumnScope
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.foundation.layout.RowScope
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.components.overlay.Overlay
import com.varabyte.kobweb.silk.components.style.*
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.theme.toSilkPalette
import dev.bitspittle.racketeer.site.G
import org.jetbrains.compose.web.css.*

val ModalStyle = ComponentStyle.base("modal") {
    Modifier
        .minWidth(300.px)
        .backgroundColor(colorMode.toSilkPalette().background)
        .margin(top = 15.percent)
        .padding(20.px)
        .gap(10.px)
        .borderRadius(2.percent)
}

val ModalContentColumnStyle = ComponentStyle.base("modal-content-col") {
    Modifier
        .fillMaxWidth()
        .gap(10.px)
        .padding(5.px) // Avoid outlines clipping against the side / add space between buttons and scrollbar
        .maxHeight(500.px)
        .overflowY(Overflow.Auto)
}

val ModalTitleStyle = ComponentStyle.base("modal-title") {
    Modifier
        .fontSize(G.Font.Sizes.Normal)
        .fontWeight(FontWeight.Bold)
        .margin(bottom = 30.px)
}

val ModalButtonRowStyle = ComponentStyle("modal-button-row") {
    base {
        Modifier.fillMaxWidth().margin(top = 20.px).gap(10.px)
    }

    cssRule(" *") {
        Modifier.flexGrow(1)
    }
}


@Composable
fun Modal(
    overlayModifier: Modifier = Modifier,
    dialogModifier: Modifier = Modifier,
    title: String? = null,
    bottomRow: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Overlay(overlayModifier) {
        Column(ModalStyle.toModifier().then(dialogModifier)) {
            if (title != null) {
                SpanText(title, ModalTitleStyle.toModifier().align(Alignment.CenterHorizontally))
            }
            Column(ModalContentColumnStyle.toModifier()) {
                content()
            }
            if (bottomRow != null) {
                Row(ModalButtonRowStyle.toModifier()) {
                    bottomRow()
                }
            }
        }
    }
}
