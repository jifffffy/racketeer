package dev.bitspittle.racketeer.model.pile

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import dev.bitspittle.racketeer.model.card.Card
import dev.bitspittle.racketeer.model.card.MutableCard

/**
 * A meaningful group of cards, with a particular purpose (as opposed to just a random collection of cards)
 *
 * For example, the discard pile, or the deck.
 */
interface Pile {
    val id: Uuid
    val cards: List<Card>
}

class MutablePile internal constructor(
    override val id: Uuid, override val cards: MutableList<MutableCard>
) : Pile {
    constructor(cards: MutableList<MutableCard> = mutableListOf()) : this(uuid4(), cards)
    fun copy() = MutablePile(id, cards.map { it.copy() }.toMutableList())
}
