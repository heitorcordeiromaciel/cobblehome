package io.github.heitorcordeiromaciel.ui

import io.github.heitorcordeiromaciel.storage.HomeStorageManager
import io.github.heitorcordeiromaciel.storage.PCAccessor
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

/** Menu handler for CobbleHome UI. Manages the client-side logic and inventory synchronization. */
class CobbleHomeMenu(
        containerId: Int,
        playerInventory: Inventory // We keep this to satisfy constructor, but don't bind slots
) : AbstractContainerMenu(MenuRegistry.COBBLEHOME_MENU_TYPE.get(), containerId) {

    enum class ViewMode {
        HOME,
        PC
    }

    companion object {
        const val SLOTS_PER_ROW = 9
        // Home Storage (Top)
        const val HOME_ROWS = 6
        const val HOME_GRID_SIZE = SLOTS_PER_ROW * HOME_ROWS // 54 total grid slots

        const val TOTAL_HOME_SLOTS_PER_PAGE = HOME_GRID_SIZE
        const val TOTAL_HOME_SLOTS = 2700 // Matches HomeStore.MAX_CAPACITY (50 boxes * 54)

        // PC Storage
        const val LOGICAL_PC_BOX_SIZE = 30 // Standard Cobblemon PC Box Size
        const val PC_GRID_SIZE = 54 // We want to show 54 slots visually to match Home

        // Bottom Menu Area (Formerly Player Inventory)
        const val CONTROL_ROW_Y = 140 + 18 

        // Slot Ranges in `slots` list:
        // 0..53 : Home Grid Slots
        // 54..107 : PC Grid Slots (54 slots)
        // 108..110 : Control Buttons

        const val HOME_SLOTS_START = 0
        const val PC_SLOTS_START = HOME_GRID_SIZE
        const val CONTROL_SLOTS_START = PC_SLOTS_START + PC_GRID_SIZE // 54 + 54 = 108
    }

    var currentView: ViewMode = ViewMode.HOME
        private set

    private var currentPCBox = 0 // Track which PC box we're viewing
    private var currentHomeBox = 0 // Track which Home box we're viewing (0-49)

    // Virtual container for Home storage slots
    private val homeContainer = SimpleContainer(TOTAL_HOME_SLOTS)

    // Virtual container for PC storage slots - sized for visual grid
    private val pcContainer = SimpleContainer(PC_GRID_SIZE)

    // Virtual container for Control Buttons
    private val controlContainer = SimpleContainer(3)

    init {
        // 1. Initialize HOME SLOTS (Indices 0-53)
        for (row in 0 until HOME_ROWS) {
            for (col in 0 until SLOTS_PER_ROW) {
                val gridIndex = row * 9 + col
                // Add slot (initially visible)
                addSlot(PokemonStorageSlot(
                    homeContainer,
                    gridIndex,
                    8 + col * 18,
                    18 + row * 18
                ))
            }
        }

        // 2. Initialize PC SLOTS (Indices 54-107)
        // Initially Off-Screen
        // We initialize 54 slots to match Home Grid layout (9x6)
        for (row in 0 until HOME_ROWS) {
            for (col in 0 until SLOTS_PER_ROW) {
                val i = row * 9 + col // 0 to 53
                addSlot(PokemonStorageSlot(
                    pcContainer,
                    i,
                    -10000, // Hidden initially
                    -10000  // Hidden initially
                ))
            }
        }

        // 3. Initialize Control Slots (Indices 108-110)
        // Replaces part of the bottom inventory area.
        val controlY = 150 
        val midX = 176 / 2

        // Prev Button
        addSlot(object : Slot(controlContainer, 0, midX - 40, controlY) {
            override fun mayPlace(stack: ItemStack): Boolean = false
            override fun mayPickup(player: Player): Boolean = false
        })

        // Swap View Button (Center)
        addSlot(object : Slot(controlContainer, 1, midX - 9, controlY) {
            override fun mayPlace(stack: ItemStack): Boolean = false
            override fun mayPickup(player: Player): Boolean = false
        })

        // Next Button
        addSlot(object : Slot(controlContainer, 2, midX + 40 - 18, controlY) { // -18 to align left edge
            override fun mayPlace(stack: ItemStack): Boolean = false
            override fun mayPickup(player: Player): Boolean = false
        })
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        return ItemStack.EMPTY
    }

    override fun stillValid(player: Player): Boolean {
        return true
    }

    fun toggleView() {
        currentView = if (currentView == ViewMode.HOME) ViewMode.PC else ViewMode.HOME
        updateSlotPositions()
    }

    private fun updateSlotPositions() {
        // Safe check for ranges
        if (CONTROL_SLOTS_START > slots.size) return

        val homeSlots = slots.subList(HOME_SLOTS_START, PC_SLOTS_START)
        val pcSlots = slots.subList(PC_SLOTS_START, CONTROL_SLOTS_START)

        if (currentView == ViewMode.HOME) {
            // Show Home, Hide PC
            homeSlots.forEachIndexed { index, slot ->
                val row = index / 9
                val col = index % 9
                slot.x = 8 + col * 18
                slot.y = 18 + row * 18
            }
            pcSlots.forEach { it.x = -10000; it.y = -10000 }
        } else {
            // Show PC, Hide Home
            homeSlots.forEach { it.x = -10000; it.y = -10000 }
            
            // Show PC Slots using same 9x6 layout
             pcSlots.forEachIndexed { index, slot ->
                val row = index / 9
                val col = index % 9
                slot.x = 8 + col * 18
                slot.y = 18 + row * 18 
            }
        }
    }

    /** Navigate to previous PC box */
    fun previousBox() {
        if (currentView == ViewMode.HOME) {
            previousHomeBox()
        } else {
            if (currentPCBox > 0) {
                currentPCBox--
                broadcastChanges()
            }
        }
    }

    /** Navigate to next PC box */
    fun nextBox() {
        if (currentView == ViewMode.HOME) {
            nextHomeBox()
        } else {
            if (currentPCBox < 49) { // Max 50 boxes
                currentPCBox++
                broadcastChanges()
            }
        }
    }

    // --- HOME PAGINATION ---
    private fun previousHomeBox() {
        if (currentHomeBox > 0) {
            currentHomeBox--
        } else {
            currentHomeBox = 49
        }
        broadcastChanges()
    }

    private fun nextHomeBox() {
        if (currentHomeBox < 49) {
            currentHomeBox++
        } else {
            currentHomeBox = 0
        }
        broadcastChanges()
    }

    fun getCurrentBoxIndex(): Int = if (currentView == ViewMode.HOME) currentHomeBox else currentPCBox

    /** Gets the Pokémon to display for Home Storage (Top) - Current Page */
    fun getHomePokemon(): List<com.cobblemon.mod.common.pokemon.Pokemon?> {
        val allSlots = HomeStorageManager.getHomeStore().getAllSlots()

        val start = currentHomeBox * TOTAL_HOME_SLOTS_PER_PAGE
        val end = start + TOTAL_HOME_SLOTS_PER_PAGE

        // Slice the list for the current page
        val pageList =
                if (start < allSlots.size) {
                    allSlots.subList(start, end.coerceAtMost(allSlots.size))
                } else {
                    emptyList()
                }

        return pageList + List(maxOf(0, TOTAL_HOME_SLOTS_PER_PAGE - pageList.size)) { null }
    }

    /** Gets the Pokémon to display for PC Storage (Bottom) - Current Box */
    fun getPCPokemon(): List<com.cobblemon.mod.common.pokemon.Pokemon?> {
        val allPokemon = PCAccessor.getAllPCPokemon()

        val boxStart = currentPCBox * LOGICAL_PC_BOX_SIZE // Standard 30 per box
        val boxEnd = boxStart + LOGICAL_PC_BOX_SIZE

        val pcList =
                if (boxStart < allPokemon.size) {
                    allPokemon.subList(boxStart, boxEnd.coerceAtMost(allPokemon.size))
                } else {
                    emptyList()
                }

        // Pad to PC_GRID_SIZE (54)
        return pcList + List(maxOf(0, PC_GRID_SIZE - pcList.size)) { null }
    }
}
