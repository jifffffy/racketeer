package dev.bitspittle.racketeer.model.building

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4

interface Building : Comparable<Building> {
    val blueprint: Blueprint
    val id: Uuid
    val vpBase: Int
    val vpPassive: Int
    val counter: Int
    val isActivated: Boolean
}

/**
 * @param blueprint The blueprint this building was constructed from.
 * @param id A globally unique ID which can act as this specific card's fingerprint
 */
class MutableBuilding internal constructor(
    override val blueprint: Blueprint,
    override val id: Uuid = uuid4(),
    vpBase: Int,
    vpPassive: Int,
    counter: Int,
    override var isActivated: Boolean,
) : Building {
    internal constructor(blueprint: Blueprint) : this(blueprint, uuid4(), 0, 0, 0, isActivated = false)

    fun copy(
        id: Uuid = this.id,
        vpBase: Int = this.vpBase,
        counter: Int = this.counter,
        isActivated: Boolean = this.isActivated
    ) = MutableBuilding(blueprint, id, vpBase, this.vpPassive, counter, isActivated)

    /**
     * Buildings can earn victory points over the course of the game.
     */
    override var vpBase = vpBase
        set(value) {
            field = value.coerceAtLeast(0)
        }

    /**
     * Some buildings have bonus VP amount generated by passive actions.
     */
    override var vpPassive = vpPassive
        set(value) {
            field = value.coerceAtLeast(0)
        }

    /**
     * Buildings can be given counters that can then be consumed later for any desired effect.
     */
    override var counter = counter
        set(value) {
            field = value.coerceAtLeast(0)
        }

    override fun compareTo(other: Building): Int {
        return blueprint.compareTo(other.blueprint).takeUnless { it == 0 }
            ?: id.compareTo(other.id) // This last check is meaningless but consistent
    }
}

val Building.vpTotal get() = vpBase + vpPassive
