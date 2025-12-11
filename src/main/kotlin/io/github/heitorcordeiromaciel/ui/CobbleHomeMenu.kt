package io.github.heitorcordeiromaciel.ui

import io.github.heitorcordeiromaciel.storage.HomeStorageManager
import io.github.heitorcordeiromaciel.storage.PCAccessor
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

/** Menu handler for CobbleHome UI. Manages the server-side logic and inventory synchronization. */
class CobbleHomeMenu(
        containerId: Int,
        playerInventory: Inventory,
        private val player: ServerPlayer
) : AbstractContainerMenu(MenuRegistry.COBBLEHOME_MENU_TYPE.get(), containerId) {

    companion object {
        const val SLOTS_PER_ROW = 9
        const val ROWS = 6
        const val TOTAL_SLOTS = SLOTS_PER_ROW * ROWS
    }

    private var viewMode = ViewMode.HOME
    private val homeStore = HomeStorageManager.getHomeStore()

    init {
        // Add observer to home store
        HomeStorageManager.addObserver(player)

        // Add player inventory slots (standard 3 rows + hotbar)
        // Player inventory
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18))
            }
        }

        // Hotbar
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 198))
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        // Disable shift-clicking for now (view-only mode)
        return ItemStack.EMPTY
    }

    override fun stillValid(player: Player): Boolean {
        return true
    }

    override fun removed(player: Player) {
        super.removed(player)
        if (player is ServerPlayer) {
            HomeStorageManager.removeObserver(player.uuid)
        }
    }

    /** Toggles between HOME and PC view modes */
    fun toggleViewMode() {
        viewMode =
                when (viewMode) {
                    ViewMode.HOME -> ViewMode.PC
                    ViewMode.PC -> ViewMode.HOME
                }
        broadcastChanges()
    }

    /** Gets the current view mode */
    fun getViewMode(): ViewMode = viewMode

    /** Gets the Pokémon to display based on current view mode */
    fun getDisplayedPokemon(): List<com.cobblemon.mod.common.pokemon.Pokemon?> {
        return when (viewMode) {
            ViewMode.HOME -> {
                val slots = homeStore.getAllSlots()
                // Pad to at least TOTAL_SLOTS
                slots + List(maxOf(0, TOTAL_SLOTS - slots.size)) { null }
            }
            ViewMode.PC -> {
                val allPokemon = PCAccessor.getAllPCPokemon(player)
                // Pad to at least TOTAL_SLOTS
                allPokemon + List(maxOf(0, TOTAL_SLOTS - allPokemon.size)) { null }
            }
        }
    }

    enum class ViewMode {
        HOME,
        PC
    }
}
