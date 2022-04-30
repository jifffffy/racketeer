package dev.bitspittle.racketeer.console.utils

import dev.bitspittle.racketeer.model.card.CardTemplate


class CardSearcher(cards: List<CardTemplate>) {
    val cards = cards.sortedBy { it.name }

    fun search(searchPrefix: String): Int {
        val foundCard =
            cards
                .asSequence()
                .filter { card -> card.name.startsWith(searchPrefix, ignoreCase = true) }
                .firstOrNull()
                ?: cards.reversed()
                    .asSequence()
                    .filter { card -> searchPrefix > card.name.lowercase() }
                    .firstOrNull()

        return foundCard?.let { cards.indexOf(it) } ?: 0
    }
}