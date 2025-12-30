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

        // Render control buttons in the bottom area
        renderControlButtons(graphics)

        // Render Pokemon sprites based on current view
        renderPokemonSprites(graphics)

        // Render tooltips last
        renderCustomTooltips(graphics, mouseX, mouseY)
    }

    private fun renderCustomTooltips(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val hoveredSlot = slotUnderMouse ?: return
        val slotIndex = hoveredSlot.index // This is the index in menu.slots

        // --- Control Buttons Tooltips ---
        val controlStartIndex = CobbleHomeMenu.CONTROL_SLOTS_START // 84
        if (slotIndex >= controlStartIndex) {
            val controlIndex = slotIndex - controlStartIndex
            when (controlIndex) {
                0 -> graphics.renderTooltip(font, Component.literal("Previous Box"), mouseX, mouseY)
                1 -> graphics.renderTooltip(font, Component.literal("Swap View (Home/PC)"), mouseX, mouseY)
                2 -> graphics.renderTooltip(font, Component.literal("Next Box"), mouseX, mouseY)
            }
            return
        }

        // --- Pokemon Tooltips ---
        var pokemon: com.cobblemon.mod.common.pokemon.Pokemon? = null
        
        // We need to resolve which Pokemon is in this slot.
        // The menu.slots are mapped:
        // 0-53: Home Grid
        // 54-107: PC Grid
        
        if (menu.currentView == CobbleHomeMenu.ViewMode.HOME) {
            if (slotIndex < CobbleHomeMenu.HOME_GRID_SIZE) {
                // In Home View, we look at Home Slots
                val homePokemon = menu.getHomePokemon()
                // slotIndex maps 1:1 to list index for page
                pokemon = homePokemon.getOrNull(slotIndex)
            }
        } else {
             // In PC View, we look at PC Slots
             // PC Slots start at 54 in the slot list
             if (slotIndex >= CobbleHomeMenu.PC_SLOTS_START && slotIndex < CobbleHomeMenu.CONTROL_SLOTS_START) {
                 val pcIndex = slotIndex - CobbleHomeMenu.PC_SLOTS_START
                 val pcPokemon = menu.getPCPokemon()
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
            if (menu.currentView == CobbleHomeMenu.ViewMode.HOME) {
                 tooltipLines.add(Component.literal("Home Page ${menu.getCurrentBoxIndex() + 1}").withStyle(net.minecraft.ChatFormatting.GREEN))
            } else {
                 tooltipLines.add(Component.literal("PC Box ${menu.getCurrentBoxIndex() + 1}").withStyle(net.minecraft.ChatFormatting.AQUA))
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
    }

    private fun renderControlButtons(graphics: GuiGraphics) {
        val x = (width - imageWidth) / 2
        val y = (height - imageHeight) / 2
        
        // We have 3 control slots at the end of the slot list
        val controlStart = CobbleHomeMenu.CONTROL_SLOTS_START
        if (controlStart + 2 >= menu.slots.size) return
        
        // Prev Button (Slot 0 of controls)
        val prevSlot = menu.slots[controlStart]
        graphics.renderItem(ItemStack(Items.STONE_BUTTON), x + prevSlot.x, y + prevSlot.y)

        // Swap View Button (Slot 1 of controls)
        val swapSlot = menu.slots[controlStart + 1]
        graphics.renderItem(ItemStack(Items.COMPASS), x + swapSlot.x, y + swapSlot.y)

        // Next Button (Slot 2 of controls)
        val nextSlot = menu.slots[controlStart + 2]
        graphics.renderItem(ItemStack(Items.STONE_BUTTON), x + nextSlot.x, y + nextSlot.y)
    }

    private fun renderPokemonSprites(graphics: GuiGraphics) {
        val x = (width - imageWidth) / 2
        val y = (height - imageHeight) / 2
        
        if (menu.currentView == CobbleHomeMenu.ViewMode.HOME) {
            // Render Home Pokemon
            val homePokemon = menu.getHomePokemon()
            
            for (i in 0 until CobbleHomeMenu.HOME_GRID_SIZE) {
                 val slot = menu.slots[i]
                 val poke = homePokemon.getOrNull(i)
                 if (poke != null) {
                     renderSinglePokemon(graphics, poke, x + slot.x, y + slot.y)
                 }
            }
        } else {
            // Render PC Pokemon
            val pcPokemon = menu.getPCPokemon()
            // PC Slots loop
            for (i in 0 until CobbleHomeMenu.PC_GRID_SIZE) {
                 val poke = pcPokemon.getOrNull(i)
                 if (poke != null) {
                     val slotIndex = CobbleHomeMenu.PC_SLOTS_START + i
                     if (slotIndex < menu.slots.size) {
                         val slot = menu.slots[slotIndex] // Ensure correct slot
                         renderSinglePokemon(graphics, poke, x + slot.x, y + slot.y)
                     }
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
            val slotIndex = hoveredSlot.index 
            
            // Check Control Buttons
            val controlStart = CobbleHomeMenu.CONTROL_SLOTS_START
            if (slotIndex >= controlStart) {
                if (button == 0) { // Left click
                    val controlIndex = slotIndex - controlStart
                    when (controlIndex) {
                        0 -> { // Prev
                            menu.previousBox()
                            return true
                        }
                        1 -> { // Swap View
                            menu.toggleView()
                            return true
                        }
                        2 -> { // Next
                            menu.nextBox()
                            return true
                        }
                    }
                }
            }
            
            // Handle Transfers
            if (button == 0) {
                var pokemonToTransfer: com.cobblemon.mod.common.pokemon.Pokemon? = null
                var transferToPC = false
                
                if (menu.currentView == CobbleHomeMenu.ViewMode.HOME) {
                    if (slotIndex < CobbleHomeMenu.HOME_GRID_SIZE) {
                         pokemonToTransfer = menu.getHomePokemon().getOrNull(slotIndex)
                         transferToPC = true
                    }
                } else {
                    // PC View
                    if (slotIndex >= CobbleHomeMenu.PC_SLOTS_START && slotIndex < controlStart) {
                         val pcIndex = slotIndex - CobbleHomeMenu.PC_SLOTS_START
                         pokemonToTransfer = menu.getPCPokemon().getOrNull(pcIndex)
                         transferToPC = false
                    }
                }
                
                if (pokemonToTransfer != null) {
                     val packet =
                            io.github.heitorcordeiromaciel.network.packets.TransferPokemonPacket(
                                    pokemonUUID = pokemonToTransfer.uuid.toString(),
                                    fromPC = !transferToPC
                            )
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet)
                    return true
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val label = if (menu.currentView == CobbleHomeMenu.ViewMode.HOME) {
             "Vault Box ${menu.getCurrentBoxIndex() + 1}"
        } else {
             "PC Box ${menu.getCurrentBoxIndex() + 1}"
        }
        
        graphics.drawString(font, label, 8, 6, 0x404040, false)
        
        // Render label for Controls area?
        graphics.drawString(font, "Controls", 8, 129 + 9, 0x404040, false)
    }
}
