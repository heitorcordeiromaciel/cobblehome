package io.github.heitorcordeiromaciel.ui

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Items
import net.minecraft.world.item.ItemStack

/** Screen renderer for CobbleHome UI. Handles client-side rendering and user interaction. */
class CobbleHomeScreen(menu: CobbleHomeMenu, playerInventory: Inventory, title: Component) :
        AbstractContainerScreen<CobbleHomeMenu>(menu, playerInventory, title) {

    companion object {
        private val BACKGROUND_TEXTURE =
                ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png")

        private const val TEXTURE_WIDTH = 176
        private const val TEXTURE_HEIGHT = 222
        
        // Control slot indices (in hotbar)
        private const val PREV_BOX_SLOT = 3  // 4th hotbar slot (index 3)
        private const val TOGGLE_SLOT = 4     // 5th hotbar slot (middle)
        private const val NEXT_BOX_SLOT = 5  // 6th hotbar slot (index 5)
    }

    init {
        imageWidth = TEXTURE_WIDTH
        imageHeight = TEXTURE_HEIGHT

        // Request PC data from server
        com.cobblemon.mod.common.Cobblemon.LOGGER.info("CobbleHomeScreen: Requesting PC data from server")
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                io.github.heitorcordeiromaciel.network.packets.RequestPCDataPacket()
        )
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        
        // Render control buttons over hotbar slots
        renderControlButtons(graphics)
        
        // Render Pokemon sprites in storage slots
        renderPokemonSprites(graphics)
        
        // Render tooltips last so they appear on top
        renderCustomTooltips(graphics, mouseX, mouseY)
    }
    
    private fun renderCustomTooltips(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val hoveredSlot = slotUnderMouse ?: return
        val slotIndex = hoveredSlot.index
        
        // Control button tooltips
        if (slotIndex == CobbleHomeMenu.HOTBAR_START + PREV_BOX_SLOT) {
            graphics.renderTooltip(font, Component.literal("Previous Box"), mouseX, mouseY)
            return
        }
        if (slotIndex == CobbleHomeMenu.HOTBAR_START + TOGGLE_SLOT) {
            val viewText = when (menu.getViewMode()) {
                CobbleHomeMenu.ViewMode.HOME -> "Switch to PC View"
                CobbleHomeMenu.ViewMode.PC -> "Switch to Home View"
            }
            graphics.renderTooltip(font, Component.literal(viewText), mouseX, mouseY)
            return
        }
        if (slotIndex == CobbleHomeMenu.HOTBAR_START + NEXT_BOX_SLOT) {
            graphics.renderTooltip(font, Component.literal("Next Box"), mouseX, mouseY)
            return
        }
        
        // Pokemon tooltips
        if (slotIndex < CobbleHomeMenu.TOTAL_SLOTS) {
            val pokemon = menu.getDisplayedPokemon().getOrNull(slotIndex)
            if (pokemon != null) {
                val tooltipLines = mutableListOf<Component>()
                
                // Pokemon name with shiny indicator
                val nameText = if (pokemon.shiny) "★ ${pokemon.species.translatedName.string}" else pokemon.species.translatedName.string
                tooltipLines.add(Component.literal(nameText).withStyle { 
                    it.withColor(if (pokemon.shiny) 0xFFD700 else 0xFFFFFF)
                })
                
                // Species
                tooltipLines.add(Component.literal("Species: ${pokemon.species.name}"))
                
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
                
                // HP
                tooltipLines.add(Component.literal("HP: ${pokemon.currentHealth}/${pokemon.maxHealth}"))
                
                // Friendship
                tooltipLines.add(Component.literal("Friendship: ${pokemon.friendship}"))
                
                // Moves
                tooltipLines.add(Component.literal("Moves:"))
                pokemon.moveSet.forEach { move ->
                    tooltipLines.add(Component.literal("  • ${move.name}"))
                }
                
                // Held item
                if (!pokemon.heldItem().isEmpty) {
                    tooltipLines.add(Component.literal("Held Item: ${pokemon.heldItem().hoverName.string}"))
                }
                
                // Pokeball
                tooltipLines.add(Component.literal("Ball: ${pokemon.caughtBall.name.path}"))
                
                graphics.renderTooltip(font, tooltipLines, java.util.Optional.empty(), mouseX, mouseY)
                return
            }
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
        
        // Render prev box button (stone block in slot 4)
        val prevSlot = menu.slots[CobbleHomeMenu.HOTBAR_START + PREV_BOX_SLOT]
        graphics.renderItem(ItemStack(Items.STONE), x + prevSlot.x, y + prevSlot.y)
        
        // Render toggle button (wooden sign in slot 5 - middle)
        val toggleSlot = menu.slots[CobbleHomeMenu.HOTBAR_START + TOGGLE_SLOT]
        graphics.renderItem(ItemStack(Items.OAK_SIGN), x + toggleSlot.x, y + toggleSlot.y)
        
        // Render next box button (stone block in slot 6)
        val nextSlot = menu.slots[CobbleHomeMenu.HOTBAR_START + NEXT_BOX_SLOT]
        graphics.renderItem(ItemStack(Items.STONE), x + nextSlot.x, y + nextSlot.y)
    }
    
    private fun renderPokemonSprites(graphics: GuiGraphics) {
        val x = (width - imageWidth) / 2
        val y = (height - imageHeight) / 2
        val pokemon = menu.getDisplayedPokemon()
        
        // Render Pokemon in storage slots
        for (i in 0 until CobbleHomeMenu.TOTAL_SLOTS) {
            val poke = pokemon.getOrNull(i)
            if (poke != null) {
                val slot = menu.slots[i]
                val slotX = x + slot.x
                val slotY = y + slot.y
                
                // Simple colored indicator based on Pokemon type
                val typeColor = when (poke.primaryType.name.lowercase()) {
                    "fire" -> 0xFFFF4500.toInt()
                    "water" -> 0xFF1E90FF.toInt()
                    "grass" -> 0xFF32CD32.toInt()
                    "electric" -> 0xFFFFD700.toInt()
                    "psychic" -> 0xFFFF1493.toInt()
                    "dragon" -> 0xFF8B00FF.toInt()
                    "dark" -> 0xFF2F4F4F.toInt()
                    "fairy" -> 0xFFFFB6C1.toInt()
                    "fighting" -> 0xFFC85A54.toInt()
                    "flying" -> 0xFF87CEEB.toInt()
                    "poison" -> 0xFF9932CC.toInt()
                    "ground" -> 0xFFD2691E.toInt()
                    "rock" -> 0xFF8B4513.toInt()
                    "bug" -> 0xFF9ACD32.toInt()
                    "ghost" -> 0xFF9370DB.toInt()
                    "steel" -> 0xFFB0C4DE.toInt()
                    "ice" -> 0xFF00CED1.toInt()
                    else -> 0xFF4CAF50.toInt()
                }
                
                // Draw colored square
                graphics.fill(
                    slotX + 1,
                    slotY + 1,
                    slotX + 15,
                    slotY + 15,
                    typeColor
                )
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Handle slot clicks
        val hoveredSlot = slotUnderMouse
        
        if (hoveredSlot != null && button == 0) { // Left click
            val slotIndex = hoveredSlot.index
            
            // Check if clicking a control button in hotbar
            if (slotIndex == CobbleHomeMenu.HOTBAR_START + PREV_BOX_SLOT) {
                menu.previousBox()
                return true
            }
            if (slotIndex == CobbleHomeMenu.HOTBAR_START + TOGGLE_SLOT) {
                menu.toggleViewMode()
                return true
            }
            if (slotIndex == CobbleHomeMenu.HOTBAR_START + NEXT_BOX_SLOT) {
                menu.nextBox()
                return true
            }
            
            // Check if clicking a Pokemon storage slot
            if (slotIndex < CobbleHomeMenu.TOTAL_SLOTS) {
                val pokemon = menu.getDisplayedPokemon().getOrNull(slotIndex)
                if (pokemon != null) {
                    // Send transfer packet
                    val fromPC = menu.getViewMode() == CobbleHomeMenu.ViewMode.PC
                    val packet = io.github.heitorcordeiromaciel.network.packets.TransferPokemonPacket(
                        pokemonUUID = pokemon.uuid.toString(),
                        fromPC = fromPC
                    )
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet)
                    
                    com.cobblemon.mod.common.Cobblemon.LOGGER.info(
                        "Requesting transfer: ${pokemon.species.name} from ${if (fromPC) "PC" else "Home"} to ${if (fromPC) "Home" else "PC"}"
                    )
                    return true
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }
    
    override fun renderSlot(graphics: GuiGraphics, slot: net.minecraft.world.inventory.Slot) {
        val slotIndex = slot.index
        
        // Hide player inventory items (but still render the control buttons)
        if (slotIndex >= CobbleHomeMenu.PLAYER_INV_START) {
            // Only render control button slots
            if (slotIndex == CobbleHomeMenu.HOTBAR_START + PREV_BOX_SLOT ||
                slotIndex == CobbleHomeMenu.HOTBAR_START + TOGGLE_SLOT ||
                slotIndex == CobbleHomeMenu.HOTBAR_START + NEXT_BOX_SLOT) {
                // Control buttons are rendered separately, don't render the actual items
                return
            }
            // Don't render other player inventory items
            return
        }
        
        // Render Pokemon storage slots normally (they're empty ItemStacks anyway)
        super.renderSlot(graphics, slot)
    }

    override fun renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // Render title
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false)
        
        // Render current box number if in PC view
        if (menu.getViewMode() == CobbleHomeMenu.ViewMode.PC) {
            val boxText = "Box ${menu.getCurrentBox() + 1}"
            graphics.drawString(font, boxText, imageWidth - font.width(boxText) - 7, titleLabelY, 0x404040, false)
        }
    }
}
