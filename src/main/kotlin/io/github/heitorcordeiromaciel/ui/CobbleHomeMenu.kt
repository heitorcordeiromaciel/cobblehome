package io.github.heitorcordeiromaciel.ui

import io.github.heitorcordeiromaciel.storage.HomeStorageManager
import io.github.heitorcordeiromaciel.storage.PCAccessor
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

/** Menu handler for CobbleHome UI. Manages the client-side logic and inventory synchronization. */
@OnlyIn(Dist.CLIENT)
class CobbleHomeMenu(
        containerId: Int,
        private val playerInventory: Inventory
) : AbstractContainerMenu(MenuRegistry.COBBLEHOME_MENU_TYPE.get(), containerId) {

    companion object {
        const val SLOTS_PER_ROW = 9
        const val ROWS = 5  // Changed from 6 to 5 for 30 slots total
        const val TOTAL_SLOTS = SLOTS_PER_ROW * ROWS // 45 slots (5 rows x 9)
        const val POKEMON_SLOTS_START = 0
        const val PLAYER_INV_START = TOTAL_SLOTS
        const val HOTBAR_START = PLAYER_INV_START + 27
    }

    private var viewMode = ViewMode.HOME
    private var currentPCBox = 0 // Track which PC box we're viewing
    
    // Virtual container for Pokemon storage slots (doesn't actually hold items)
    private val pokemonContainer = SimpleContainer(TOTAL_SLOTS)

    init {
        // Add Pokemon storage slots (6 rows x 9 columns)
        for (row in 0 until ROWS) {
            for (col in 0 until SLOTS_PER_ROW) {
                val index = row * SLOTS_PER_ROW + col
                addSlot(PokemonStorageSlot(pokemonContainer, index, 8 + col * 18, 18 + row * 18))
            }
        }

        // Add player inventory slots (3 rows)
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18))
            }
        }

        // Add hotbar slots
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 198))
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        // Disable shift-clicking
        return ItemStack.EMPTY
    }

    override fun stillValid(player: Player): Boolean {
        return true
    }

    override fun removed(player: net.minecraft.world.entity.player.Player) {
        super.removed(player)
    }

    /** Toggles between HOME and PC view modes */
    fun toggleViewMode() {
        viewMode = when (viewMode) {
            ViewMode.HOME -> ViewMode.PC
            ViewMode.PC -> ViewMode.HOME
        }
        currentPCBox = 0 // Reset to first box when switching views
        broadcastChanges()
    }

    /** Gets the current view mode */
    fun getViewMode(): ViewMode = viewMode
    
    /** Navigate to previous PC box */
    fun previousBox() {
        if (viewMode == ViewMode.PC && currentPCBox > 0) {
            currentPCBox--
            broadcastChanges()
        }
    }
    
    /** Navigate to next PC box */
    fun nextBox() {
        if (viewMode == ViewMode.PC) {
            // TODO: Add max box limit based on actual PC box count
            currentPCBox++
            broadcastChanges()
        }
    }
    
    /** Get current PC box number */
    fun getCurrentBox(): Int = currentPCBox

    /** Gets the Pokémon to display based on current view mode */
    fun getDisplayedPokemon(): List<com.cobblemon.mod.common.pokemon.Pokemon?> {
        return when (viewMode) {
            ViewMode.HOME -> {
                val slots = HomeStorageManager.getHomeStore().getAllSlots()
                com.cobblemon.mod.common.Cobblemon.LOGGER.debug(
                        "CobbleHomeMenu: HOME view - ${slots.count { it != null }} Pokémon"
                )
                // Pad to at least TOTAL_SLOTS
                slots + List(maxOf(0, TOTAL_SLOTS - slots.size)) { null }
            }
            ViewMode.PC -> {
                val allPokemon = PCAccessor.getAllPCPokemon()
                com.cobblemon.mod.common.Cobblemon.LOGGER.debug(
                        "CobbleHomeMenu: PC view - ${allPokemon.size} Pokémon from cache"
                )
                // Get Pokemon for current box (54 per box)
                val boxStart = currentPCBox * TOTAL_SLOTS
                val boxEnd = boxStart + TOTAL_SLOTS
                val boxPokemon = allPokemon.subList(
                    boxStart.coerceAtMost(allPokemon.size),
                    boxEnd.coerceAtMost(allPokemon.size)
                )
                // Pad to TOTAL_SLOTS
                boxPokemon + List(maxOf(0, TOTAL_SLOTS - boxPokemon.size)) { null }
            }
        }
    }

    enum class ViewMode {
        HOME,
        PC
    }
}
