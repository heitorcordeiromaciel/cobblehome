package io.github.heitorcordeiromaciel.ui

import com.cobblemon.mod.common.item.PokemonItem
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/** Screen renderer for CobbleHome UI. Handles client-side rendering and user interaction. */
class CobbleHomeScreen(menu: CobbleHomeMenu, playerInventory: Inventory, title: Component) :
        AbstractContainerScreen<CobbleHomeMenu>(menu, playerInventory, title) {

    companion object {
        private val BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblehome", "textures/gui/cobblehome.png")

        private const val TEXTURE_WIDTH = 176
        private const val TEXTURE_HEIGHT = 222

        // Control slot indices (in hotbar) - see CobbleHomeMenu constants
    }

    init {
        imageWidth = TEXTURE_WIDTH
        imageHeight = TEXTURE_HEIGHT

        // Request PC data from server
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                io.github.heitorcordeiromaciel.network.packets.RequestPCDataPacket()
        )
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)

        // Render control buttons over hotbar slots
        renderControlButtons(graphics)

        // Render Pokemon sprites in storage slots (both Home and PC)
        renderPokemonSprites(graphics)

        // Render tooltips last so they appear on top
        renderCustomTooltips(graphics, mouseX, mouseY)
    }

    private fun renderCustomTooltips(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val hoveredSlot = slotUnderMouse ?: return
        val slotIndex = hoveredSlot.index

        // Hotbar Control button tooltips
        if (slotIndex == CobbleHomeMenu.PREV_BOX_SLOT_INDEX) {
            graphics.renderTooltip(font, Component.literal("Previous PC Box"), mouseX, mouseY)
            return
        }
        if (slotIndex == CobbleHomeMenu.NEXT_BOX_SLOT_INDEX) {
            graphics.renderTooltip(font, Component.literal("Next PC Box"), mouseX, mouseY)
            return
        }
        
        // Home Control button tooltips
        if (slotIndex == CobbleHomeMenu.PREV_HOME_SLOT_INDEX) {
            graphics.renderTooltip(font, Component.literal("Previous Home Box"), mouseX, mouseY)
            return
        }
        if (slotIndex == CobbleHomeMenu.NEXT_HOME_SLOT_INDEX) {
            graphics.renderTooltip(font, Component.literal("Next Home Box"), mouseX, mouseY)
            return
        }

        // Pokemon tooltips
        // Check if it's a Pokemon slot (Top or Bottom)
        // Adjust logic: Indices 0-53 are Home Grid (Glass or Pokemon), 54+ are PC
        val homePokemon = menu.getHomePokemon()
        val pcPokemon = menu.getPCPokemon()
        
        var pokemon: com.cobblemon.mod.common.pokemon.Pokemon? = null
        
        if (slotIndex < CobbleHomeMenu.HOME_GRID_SIZE) {
            // Home Grid Area
            // Check if it's a Pokemon slot (ignore Glass)
            // But we must NOT ignore the button slots which are technically glass/virtual but handled above
            
            val slot = menu.slots[slotIndex] // Should be safe
            if (slot is PokemonStorageSlot) {
                val pokemonIndex = slot.index // Index in homeContainer
                pokemon = homePokemon.getOrNull(pokemonIndex)
            }
        } else if (slotIndex >= CobbleHomeMenu.PC_SLOTS_START_INDEX) {
            // PC Slot
            val pcIndex = slotIndex - CobbleHomeMenu.PC_SLOTS_START_INDEX
            // Check if it's a valid PC slot (0-29) and not a button slot
            if (pcIndex < CobbleHomeMenu.PC_SLOTS_PER_BOX) {
                 pokemon = pcPokemon.getOrNull(pcIndex)
            }
        }

        if (pokemon != null) {
            val tooltipLines = mutableListOf<Component>()

            // Pokemon name with shiny indicator
            val nameText =
                    if (pokemon.shiny) "★ ${pokemon.species.translatedName.string}"
                    else pokemon.species.translatedName.string
            tooltipLines.add(
                    Component.literal(nameText).withStyle {
                        it.withColor(if (pokemon.shiny) 0xFFD700 else 0xFFFFFF)
                    }
            )

            // Species
            tooltipLines.add(Component.literal("Species: ${pokemon.species.name}"))
            
            // Location hint
            if (slotIndex < CobbleHomeMenu.TOTAL_HOME_SLOTS) {
                 tooltipLines.add(Component.literal("Home Page ${menu.getCurrentHomeBox() + 1}").withStyle(net.minecraft.ChatFormatting.GREEN))
            } else {
                 tooltipLines.add(Component.literal("PC Box ${menu.getCurrentBox() + 1}").withStyle(net.minecraft.ChatFormatting.AQUA))
            }

            // Form (if not normal)
            if (pokemon.form.name != "normal") {
                tooltipLines.add(Component.literal("Form: ${pokemon.form.name}"))
            }

            // Level
            tooltipLines.add(Component.literal("Level: ${pokemon.level}"))

            // Gender
            tooltipLines.add(Component.literal("Gender: ${pokemon.gender.name}"))

            // Nature
            tooltipLines.add(Component.literal("Nature: ${pokemon.nature.name.path}"))

            // Ability
            tooltipLines.add(Component.literal("Ability: ${pokemon.ability.name}"))

            // Friendship
            tooltipLines.add(Component.literal("Friendship: ${pokemon.friendship}"))

            // Moves
            tooltipLines.add(Component.literal("Moves:"))
            pokemon.moveSet.forEach { move ->
                tooltipLines.add(Component.literal("  • ${move.name}"))
            }

            // Held item
            if (!pokemon.heldItem().isEmpty) {
                tooltipLines.add(
                        Component.literal("Held Item: ${pokemon.heldItem().hoverName.string}")
                )
            }

            // Pokeball
            tooltipLines.add(Component.literal("Ball: ${pokemon.caughtBall.name.path}"))

            graphics.renderTooltip(
                    font,
                    tooltipLines,
                    java.util.Optional.empty(),
                    mouseX,
                    mouseY
            )
            return
        }
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)

        val x = (width - imageWidth) / 2
        val y = (height - imageHeight) / 2

        // Render background texture
        graphics.blit(BACKGROUND_TEXTURE, x, y, 0, 0, imageWidth, imageHeight)
        
        // Render labels manually here if strict control is needed, but renderLabels is also called by screen
    }

    private fun renderControlButtons(graphics: GuiGraphics) {
        val x = (width - imageWidth) / 2
        val y = (height - imageHeight) / 2
        
        // Validating indices exist in range
        if (CobbleHomeMenu.PREV_BOX_SLOT_INDEX >= menu.slots.size || CobbleHomeMenu.NEXT_BOX_SLOT_INDEX >= menu.slots.size) return 
        
        // --- PC CONTROLS (Hotbar row) ---
        // PREV BOX BUTTON
        val prevSlot = menu.slots[CobbleHomeMenu.PREV_BOX_SLOT_INDEX]
        graphics.renderItem(ItemStack(Items.STONE_BUTTON), x + prevSlot.x, y + prevSlot.y)

        // NEXT BOX BUTTON
        val nextSlot = menu.slots[CobbleHomeMenu.NEXT_BOX_SLOT_INDEX]
        graphics.renderItem(ItemStack(Items.STONE_BUTTON), x + nextSlot.x, y + nextSlot.y)
        
        // --- HOME CONTROLS (Corner glass slots) ---
        if (CobbleHomeMenu.PREV_HOME_SLOT_INDEX < menu.slots.size && CobbleHomeMenu.NEXT_HOME_SLOT_INDEX < menu.slots.size) {
            val prevHomeSlot = menu.slots[CobbleHomeMenu.PREV_HOME_SLOT_INDEX]
            val nextHomeSlot = menu.slots[CobbleHomeMenu.NEXT_HOME_SLOT_INDEX]
            
            // Render arrow buttons (using Stone Button for consistency, or maybe an Arrow item if available?)
            // Using Stone Button for now.
            graphics.renderItem(ItemStack(Items.STONE_BUTTON), x + prevHomeSlot.x, y + prevHomeSlot.y)
            graphics.renderItem(ItemStack(Items.STONE_BUTTON), x + nextHomeSlot.x, y + nextHomeSlot.y)
        }
    }

    private fun renderPokemonSprites(graphics: GuiGraphics) {
        val x = (width - imageWidth) / 2
        val y = (height - imageHeight) / 2
        
        // 1. Render Home Pokemon (Top Grid with Glass Border)
        val homePokemon = menu.getHomePokemon()
        
        // Iterate through all slots in the Home Grid (54 slots)
        for (i in 0 until CobbleHomeMenu.HOME_GRID_SIZE) {
             val slot = menu.slots[i]
             
             // Check if it's a Pokemon storage slot (i.e. Inner Grid)
             if (slot is PokemonStorageSlot) {
                 val pokemonIndex = slot.index // This is the index in homeContainer (0-27)
                 val poke = homePokemon.getOrNull(pokemonIndex)
                 if (poke != null) {
                     renderSinglePokemon(graphics, poke, x + slot.x, y + slot.y)
                 }
             }
        }
        
        // 2. Render PC Pokemon (Bottom)
        val pcPokemon = menu.getPCPokemon()
        
        // PC Slots start after HOME_GRID_SIZE
        for (i in 0 until CobbleHomeMenu.PC_SLOTS_PER_BOX) {
             val poke = pcPokemon.getOrNull(i)
             if (poke != null) {
                 val slotIndex = CobbleHomeMenu.PC_SLOTS_START_INDEX + i
                 if (slotIndex < menu.slots.size) {
                     val slot = menu.slots[slotIndex]
                     renderSinglePokemon(graphics, poke, x + slot.x, y + slot.y)
                 }
             }
        }
    }
    
    private fun renderSinglePokemon(graphics: GuiGraphics, pokemon: com.cobblemon.mod.common.pokemon.Pokemon, x: Int, y: Int) {
          graphics.renderItem(com.cobblemon.mod.common.item.PokemonItem.from(pokemon.species), x, y)
    } 

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Handle slot clicks
        val hoveredSlot = slotUnderMouse

        if (hoveredSlot != null) { 
            val slotIndex = hoveredSlot.index // This is the index in menu.slots list
            
            if (slotIndex < CobbleHomeMenu.TOTAL_MENU_SLOTS) {
                if (button == 0) { // Left click logic
                    // --- PC CONTROLS ---
                    if (slotIndex == CobbleHomeMenu.PREV_BOX_SLOT_INDEX) {
                        menu.previousBox()
                        return true
                    }
                    if (slotIndex == CobbleHomeMenu.NEXT_BOX_SLOT_INDEX) {
                        menu.nextBox()
                        return true
                    }
                    
                    // --- HOME CONTROLS ---
                    if (slotIndex == CobbleHomeMenu.PREV_HOME_SLOT_INDEX) {
                        menu.previousHomeBox()
                        return true
                    }
                    if (slotIndex == CobbleHomeMenu.NEXT_HOME_SLOT_INDEX) {
                        menu.nextHomeBox()
                        return true
                    }
        
                    // --- TRANSFER LOGIC ---
                    // Click Top Slot (Home) -> Transfer to PC
                    // Click Bottom Slot (PC) -> Transfer to Home
                    
                    var pokemonToTransfer: com.cobblemon.mod.common.pokemon.Pokemon? = null
                    var transferToPC = false
                    
                    if (slotIndex < CobbleHomeMenu.HOME_GRID_SIZE) {
                        // Home Grid Area (Glass or Pokemon)
                        val slot = menu.slots[slotIndex]
                        
                        // Ignore clicks on Glass (vanilla Slots in this range are glass)
                        // ... EXCEPT for our navigation buttons which we handled above
                        
                        if (slot is PokemonStorageSlot) {
                            // This is a valid Pokemon slot
                            val pokemonIndex = slot.index // Index in homeContainer (0-27)
                            pokemonToTransfer = menu.getHomePokemon().getOrNull(pokemonIndex)
                            transferToPC = true
                        }
                    } else if (slotIndex >= CobbleHomeMenu.PC_SLOTS_START_INDEX) {
                        // PC Slot
                        val pcIndex = slotIndex - CobbleHomeMenu.PC_SLOTS_START_INDEX
                        if (pcIndex < CobbleHomeMenu.PC_SLOTS_PER_BOX) {
                            pokemonToTransfer = menu.getPCPokemon().getOrNull(pcIndex)
                            transferToPC = false
                        }
                    }
                    
                    if (pokemonToTransfer != null) {
                         // IMPORTANT: When transferring FROM Home, we need the UUID logic to be robust
                         // because we are viewing a slice.
                         val packet =
                                io.github.heitorcordeiromaciel.network.packets.TransferPokemonPacket(
                                        pokemonUUID = pokemonToTransfer.uuid.toString(),
                                        fromPC = !transferToPC
                                )
                        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet)
                    }
                }
                // ALWAYS return true for our slots to consume the event
                return true
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        graphics.drawString(font, "Home Box ${menu.getCurrentHomeBox() + 1}", 8, 6, 0x404040, false)

        val pcLabelY = 129
        val boxText = "PC Box ${menu.getCurrentBox() + 1}"
        graphics.drawString(font, boxText, 8, pcLabelY, 0x404040, false)
    }
}
