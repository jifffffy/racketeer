package dev.bitspittle.racketeer.model.shop

import com.benasher44.uuid.Uuid
import dev.bitspittle.racketeer.model.card.Card
import dev.bitspittle.racketeer.model.card.CardTemplate
import dev.bitspittle.racketeer.model.random.CloneableRandom

interface Shop {
    val tier: Int
    val stock: List<Card?>
    val exclusions: List<Exclusion>
    fun addExclusion(exclusion: Exclusion)
    suspend fun upgrade(): Boolean
    suspend fun restock(restockAll: Boolean = true, additionalFilter: suspend (CardTemplate) -> Boolean = { true }): Boolean
}

class MutableShop internal constructor(
    private val random: CloneableRandom,
    private val allCards: List<CardTemplate>,
    private val shopSizes: List<Int>,
    private val tierFrequencies: List<Int>,
    private val rarityFrequencies: List<Int>,
    tier: Int,
    override val stock: MutableList<Card?>,
    override val exclusions: MutableList<Exclusion>,
) : Shop {
    constructor(random: CloneableRandom, allCards: List<CardTemplate>, shopSizes: List<Int>, tierFrequencies: List<Int>, rarityFrequencies: List<Int>) : this(
        random,
        allCards,
        shopSizes,
        tierFrequencies,
        rarityFrequencies,
        0,
        mutableListOf(),
        mutableListOf(),
    ) {
        handleRestock(restockAll = true, filterAllCards { true })
    }

    override var tier: Int = tier
        private set

    private fun handleRestock(restockAll: Boolean, possibleNewStock: List<CardTemplate>): Boolean {
        if (!restockAll && stock.size == shopSizes[tier]) return false // Shop is full; incremental restock fails

        if (restockAll) {
            stock.clear()
        }

        val numCardsToStock = shopSizes[tier] - stock.size

        // This should never happen, but fail fast in case game data is bad!
        require(possibleNewStock.size >= numCardsToStock) {
            "There are not enough cards defined to restock the shop at tier ${tier + 1}."
        }

        // Create an uber stock, which has all cards repeated a bunch of times as a lazy way to implement random
        // frequency distribution
        val uberStock = mutableListOf<CardTemplate>()
        possibleNewStock.forEach { card ->
            repeat(tierFrequencies[card.tier] * rarityFrequencies[card.rarity]) { uberStock.add(card) }
        }

        repeat(numCardsToStock) {
            val template = uberStock.random(random())
            uberStock.removeAll { it === template } // We never want to sell the same item twice
            stock.add(template.instantiate())
        }

        return true
    }

    private inline fun filterAllCards(additionalFilter: (CardTemplate) -> Boolean): List<CardTemplate> {
        return allCards
            .filter { card -> card.cost > 0 && card.tier <= this.tier && additionalFilter(card) }
    }

    override suspend fun restock(restockAll: Boolean, additionalFilter: suspend (CardTemplate) -> Boolean): Boolean {
        return handleRestock(
            restockAll,
            filterAllCards { card -> additionalFilter(card) && exclusions.none { exclude -> exclude(card) } })
    }

    override fun addExclusion(exclusion: Exclusion) { exclusions.add(exclusion) }

    fun remove(cardId: Uuid) {
        for (i in stock.indices) {
            stock[i]?.run {
                if (this.id == cardId) {
                    stock[i] = null
                    return
            }}
        }
    }

    override suspend fun upgrade(): Boolean {
        if (tier >= shopSizes.size - 1) return false

        ++tier
        // New slot should ALWAYS contain a card from the new tier
        restock(restockAll = false) { card -> card.tier == tier }
        return true
    }

    fun copy(random: CloneableRandom = this.random.copy()) = MutableShop(
        random,
        allCards,
        shopSizes,
        tierFrequencies,
        rarityFrequencies,
        tier,
        stock.toMutableList(),
        exclusions.toMutableList()
    )
}
