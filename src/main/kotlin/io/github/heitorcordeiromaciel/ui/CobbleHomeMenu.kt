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
@OnlyIn(Dist.CLIENT)
class CobbleHomeMenu(
        containerId: Int,
        playerInventory: Inventory // We keep this to satisfy constructor, but don't bind slots
) : AbstractContainerMenu(MenuRegistry.COBBLEHOME_MENU_TYPE.get(), containerId) {

    companion object {
        const val SLOTS_PER_ROW = 9
        // Home Storage (Top)
        const val HOME_ROWS = 6
        const val HOME_GRID_SIZE =
                SLOTS_PER_ROW * HOME_ROWS // 54 total grid slots (including glass)
        // Inner storage grid is 7 cols x 4 rows
        const val HOME_INNER_COLS = 7
        const val HOME_INNER_ROWS = 4
        const val TOTAL_HOME_SLOTS_PER_PAGE = HOME_GRID_SIZE // 54 total grid slots per page
        const val TOTAL_HOME_SLOTS = 2700 // Matches HomeStore.MAX_CAPACITY (50 boxes * 54)
        // PC Storage (Bottom - replaces player inventory area)
        const val PC_ROWS = 3 // 3 main rows + 1 hotbar row = 4 total available
        // PC slots start after the entire Home Grid (54 slots)
        const val PC_SLOTS_START_INDEX = HOME_GRID_SIZE
        const val PC_SLOTS_PER_BOX = 30

        // Total slots in the menu (Home Grid + PC area)
        // PC area is 4 rows * 9 cols = 36 slots
        const val TOTAL_MENU_SLOTS = HOME_GRID_SIZE + (4 * SLOTS_PER_ROW)

        // Control buttons will live in the last few slots of the PC area (hotbar row)
        const val HOTBAR_ROW_START_INDEX = HOME_GRID_SIZE + (3 * SLOTS_PER_ROW)
        const val PREV_BOX_SLOT_INDEX = HOTBAR_ROW_START_INDEX + 3
        const val NEXT_BOX_SLOT_INDEX = HOTBAR_ROW_START_INDEX + 5

        // Home Navigation Indices (within the 54-slot grid)
        // Row 5, Col 0 (Bottom Left) -> Prev Page
        const val PREV_HOME_SLOT_INDEX = 45
        // Row 5, Col 8 (Bottom Right) -> Next Page
        const val NEXT_HOME_SLOT_INDEX = 53
    }

    private var currentPCBox = 0 // Track which PC box we're viewing
    private var currentHomeBox = 0 // Track which Home box we're viewing (0-49)

    // Virtual container for Home storage slots (active pokemon - single page view)
    private val homeContainer = SimpleContainer(TOTAL_HOME_SLOTS) // We use index mapping

    // Virtual container for Glass Panes (border)
    private val glassContainer =
            SimpleContainer(1).apply {
                val glassStack = ItemStack(Items.GRAY_STAINED_GLASS_PANE)
                setItem(0, glassStack)
            }

    // Virtual container for empty slots (navigation buttons)
    private val navButtonContainer = SimpleContainer(1) // Empty by default

    // Virtual container for PC storage slots (replacing inventory)
    private val pcContainer = SimpleContainer(36)

    init {
        // Grid 9x6
        for (row in 0 until HOME_ROWS) {
            for (col in 0 until SLOTS_PER_ROW) {
                // Border Logic:
                val isBorder = row == 0 || row == 5 || col == 0 || col == 8
                val gridIndex = row * 9 + col

                if (gridIndex == PREV_HOME_SLOT_INDEX || gridIndex == NEXT_HOME_SLOT_INDEX) {
                    // Navigation Button Slots - Bind to Empty Container so manual button render
                    // works without glass overlay
                    addSlot(
                            object : Slot(navButtonContainer, 0, 8 + col * 18, 18 + row * 18) {
                                override fun mayPlace(stack: ItemStack): Boolean = false
                                override fun mayPickup(player: Player): Boolean = false
                            }
                    )
                } else if (isBorder) {
                    // Glass Border
                    addSlot(
                            object : Slot(glassContainer, 0, 8 + col * 18, 18 + row * 18) {
                                override fun mayPlace(stack: ItemStack): Boolean = false
                                override fun mayPickup(player: Player): Boolean = false
                            }
                    )
                } else {
                    // Active Storage Slot (Inner 7x4)
                    // We bind these to the exact gridIndex of the homeContainer for a 1:1 mapping.
                    // This way, storage slot 10 in a page is always the first inner slot.
                    addSlot(
                            PokemonStorageSlot(
                                    homeContainer,
                                    gridIndex,
                                    8 + col * 18,
                                    18 + row * 18
                            )
                    )
                }
            }
        }

        // --- BOTTOM SECTION: PC STORAGE ---
        // ... (unchanged)
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                val index = col + row * 9
                addSlot(PokemonStorageSlot(pcContainer, index, 8 + col * 18, 140 + row * 18))
            }
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        return ItemStack.EMPTY
    }

    override fun stillValid(player: Player): Boolean {
        return true
    }

    override fun removed(player: net.minecraft.world.entity.player.Player) {
        super.removed(player)
    }

    /** Navigate to previous PC box */
    fun previousBox() {
        if (currentPCBox > 0) {
            currentPCBox--
            broadcastChanges()
        }
    }

    /** Navigate to next PC box */
    fun nextBox() {
        if (currentPCBox < 49) { // Max 50 boxes (0-49)
            currentPCBox++
            broadcastChanges()
        }
    }

    fun getCurrentBox(): Int = currentPCBox

    // --- HOME PAGINATION ---
    fun previousHomeBox() {
        if (currentHomeBox > 0) {
            currentHomeBox--
        } else {
            currentHomeBox = 49 // Loop to last page
        }
        broadcastChanges()
    }

    fun nextHomeBox() {
        if (currentHomeBox < 49) {
            currentHomeBox++
        } else {
            currentHomeBox = 0 // Loop to first page
        }
        broadcastChanges()
    }

    fun getCurrentHomeBox(): Int = currentHomeBox

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

        val boxStart = currentPCBox * PC_SLOTS_PER_BOX // Standard 30 per box
        val boxEnd = boxStart + PC_SLOTS_PER_BOX

        val pcList =
                if (boxStart < allPokemon.size) {
                    allPokemon.subList(boxStart, boxEnd.coerceAtMost(allPokemon.size))
                } else {
                    emptyList()
                }

        return pcList + List(maxOf(0, 36 - pcList.size)) { null }
    }
}
