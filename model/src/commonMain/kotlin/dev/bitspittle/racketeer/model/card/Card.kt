package dev.bitspittle.racketeer.model.card

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4

interface Card : Comparable<Card> {
    val template: CardTemplate
    val vpBase: Int
    val vpPassive: Int
    val counter: Int
    val upgrades: Set<UpgradeType>
    val id: Uuid
}

/**
 * @param template The read-only template this card is based on
 * @param id A globally unique ID which can act as this specific card's fingerprint
 */
class MutableCard internal constructor(
    override val template: CardTemplate,
    vpBase: Int,
    vpPassive: Int,
    counter: Int,
    override val upgrades: MutableSet<UpgradeType>,
    override val id: Uuid = uuid4(),
) : Card {
    internal constructor(template: CardTemplate) : this(
        template, template.vp, 0, 0,
        template.upgrades.map { upgradeStr ->
            UpgradeType.values().first { it.name.compareTo(upgradeStr, ignoreCase = true) == 0 }
        }.toMutableSet(),
        uuid4(),
    )

    /** Create a clone of some target card */
    constructor(other: Card): this(other.template, other.vpBase, other.vpPassive, other.counter, other.upgrades.toMutableSet())

    /**
     * Cards can earn victory points over the course of the game via upgrades and rewards from other cards.
     */
    override var vpBase = vpBase
        set(value) {
            field = value.coerceAtLeast(0)
        }

    /**
     * Some cards have bonus VP amount generated by passive actions.
     */
    override var vpPassive = vpPassive
        set(value) {
            field = value.coerceAtLeast(0)
        }

    /**
     * Cards can be given counters that can then be spent later for any desired effect.
     */
    override var counter = counter
        set(value) {
            field = value.coerceAtLeast(0)
        }

    fun copy(
        id: Uuid = this.id,
        vpBase: Int = this.vpBase,
        vpPassive: Int = this.vpPassive,
        counter: Int = this.counter,
        upgrades: Set<UpgradeType> = this.upgrades
    ) = MutableCard(template, vpBase, vpPassive, counter, upgrades.toMutableSet(), id)

    override fun compareTo(other: Card): Int {
        return template.compareTo(other.template).takeUnless { it == 0 }
            ?: vpTotal.compareTo(other.vpTotal).takeUnless { it == 0 }
            ?: upgrades.size.compareTo(other.upgrades.size).takeUnless { it == 0 }
            ?: counter.compareTo(other.counter).takeUnless { it == 0 }
            ?: id.compareTo(other.id) // This last check is meaningless but consistent
    }
}

val Card.isDexterous get() = this.upgrades.contains(UpgradeType.CASH)
val Card.isArtful get() = this.upgrades.contains(UpgradeType.INFLUENCE)
val Card.isLucky get() = this.upgrades.contains(UpgradeType.LUCK)
val Card.isVeteran get() = this.upgrades.contains(UpgradeType.VETERAN)
val Card.vpTotal get() = vpBase + vpPassive
