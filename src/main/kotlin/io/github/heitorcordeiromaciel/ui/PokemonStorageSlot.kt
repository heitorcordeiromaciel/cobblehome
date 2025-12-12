package io.github.heitorcordeiromaciel.ui

import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

/**
 * Custom slot for Pokemon storage that doesn't allow item placement.
 * This is a virtual slot that will display Pokemon sprites but doesn't actually hold items.
 */
class PokemonStorageSlot(
    container: Container,
    slot: Int,
    x: Int,
    y: Int
) : Slot(container, slot, x, y) {

    override fun mayPlace(stack: ItemStack): Boolean {
        // Pokemon slots don't accept items
        return false
    }

    override fun mayPickup(player: Player): Boolean {
        // Pokemon slots don't allow item pickup
        return false
    }
}
