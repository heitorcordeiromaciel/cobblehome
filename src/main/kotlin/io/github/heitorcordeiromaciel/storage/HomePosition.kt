package io.github.heitorcordeiromaciel.storage

import com.cobblemon.mod.common.api.storage.StorePosition

/** Position class for HomeStore. Simple index-based positioning for the bottomless home storage. */
data class HomePosition(val index: Int) : StorePosition {
    override fun toString(): String = "HomePosition($index)"
}
