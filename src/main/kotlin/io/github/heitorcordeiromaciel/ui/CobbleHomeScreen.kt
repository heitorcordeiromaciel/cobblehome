package io.github.heitorcordeiromaciel.ui

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

/** Screen renderer for CobbleHome UI. Handles client-side rendering and user interaction. */
class CobbleHomeScreen(menu: CobbleHomeMenu, playerInventory: Inventory, title: Component) :
        AbstractContainerScreen<CobbleHomeMenu>(menu, playerInventory, title) {

    companion object {
        private val BACKGROUND_TEXTURE =
                ResourceLocation.fromNamespaceAndPath(
                        "cobblehome_neoforge",
                        "textures/gui/cobblehome.png"
                )

        private const val TEXTURE_WIDTH = 176
        private const val TEXTURE_HEIGHT = 222

        private const val TOGGLE_BUTTON_X = 7
        private const val TOGGLE_BUTTON_Y = 7
        private const val TOGGLE_BUTTON_WIDTH = 60
        private const val TOGGLE_BUTTON_HEIGHT = 20
    }

    init {
        imageWidth = TEXTURE_WIDTH
        imageHeight = TEXTURE_HEIGHT
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        renderTooltip(graphics, mouseX, mouseY)
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)

        val x = (width - imageWidth) / 2
        val y = (height - imageHeight) / 2

        // Render background texture
        graphics.blit(BACKGROUND_TEXTURE, x, y, 0, 0, imageWidth, imageHeight)

        // Render toggle button
        renderToggleButton(graphics, x, y, mouseX, mouseY)

        // Render Pokémon sprites
        renderPokemonGrid(graphics, x, y)
    }

    private fun renderToggleButton(
            graphics: GuiGraphics,
            screenX: Int,
            screenY: Int,
            mouseX: Int,
            mouseY: Int
    ) {
        val buttonX = screenX + TOGGLE_BUTTON_X
        val buttonY = screenY + TOGGLE_BUTTON_Y

        // Check if mouse is hovering over button
        val isHovered =
                mouseX >= buttonX &&
                        mouseX < buttonX + TOGGLE_BUTTON_WIDTH &&
                        mouseY >= buttonY &&
                        mouseY < buttonY + TOGGLE_BUTTON_HEIGHT

        // Draw button background
        val color = if (isHovered) 0x80FFFFFF.toInt() else 0x80000000.toInt()
        graphics.fill(
                buttonX,
                buttonY,
                buttonX + TOGGLE_BUTTON_WIDTH,
                buttonY + TOGGLE_BUTTON_HEIGHT,
                color
        )

        // Draw button text
        val buttonText =
                when (menu.getViewMode()) {
                    CobbleHomeMenu.ViewMode.HOME -> "View: Home"
                    CobbleHomeMenu.ViewMode.PC -> "View: PC"
                }

        graphics.drawString(font, buttonText, buttonX + 5, buttonY + 6, 0xFFFFFF, false)
    }

    private fun renderPokemonGrid(graphics: GuiGraphics, screenX: Int, screenY: Int) {
        val pokemon = menu.getDisplayedPokemon()
        val startX = screenX + 8
        val startY = screenY + 32
        val slotSize = 18

        // For now, just render placeholder slots
        // In a full implementation, we would render Pokémon sprites here
        // using Cobblemon's rendering utilities

        for (row in 0 until CobbleHomeMenu.ROWS) {
            for (col in 0 until CobbleHomeMenu.SLOTS_PER_ROW) {
                val index = row * CobbleHomeMenu.SLOTS_PER_ROW + col
                if (index < pokemon.size) {
                    val poke = pokemon[index]
                    val slotX = startX + col * slotSize
                    val slotY = startY + row * slotSize

                    if (poke != null) {
                        // Draw a simple indicator for now (colored square)
                        graphics.fill(
                                slotX + 1,
                                slotY + 1,
                                slotX + slotSize - 1,
                                slotY + slotSize - 1,
                                0xFF4CAF50.toInt()
                        )

                        // TODO: Render actual Pokémon sprite using Cobblemon's rendering system
                        // This would involve:
                        // 1. Getting the Pokémon's species
                        // 2. Loading the appropriate model/texture
                        // 3. Rendering it in the slot
                    } else {
                        // Empty slot - draw border
                        graphics.fill(slotX, slotY, slotX + slotSize, slotY + 1, 0xFF555555.toInt())
                        graphics.fill(
                                slotX,
                                slotY + slotSize - 1,
                                slotX + slotSize,
                                slotY + slotSize,
                                0xFF555555.toInt()
                        )
                        graphics.fill(slotX, slotY, slotX + 1, slotY + slotSize, 0xFF555555.toInt())
                        graphics.fill(
                                slotX + slotSize - 1,
                                slotY,
                                slotX + slotSize,
                                slotY + slotSize,
                                0xFF555555.toInt()
                        )
                    }
                }
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val x = (width - imageWidth) / 2
        val y = (height - imageHeight) / 2

        val buttonX = x + TOGGLE_BUTTON_X
        val buttonY = y + TOGGLE_BUTTON_Y

        // Check if toggle button was clicked
        if (mouseX >= buttonX &&
                        mouseX < buttonX + TOGGLE_BUTTON_WIDTH &&
                        mouseY >= buttonY &&
                        mouseY < buttonY + TOGGLE_BUTTON_HEIGHT
        ) {
            menu.toggleViewMode()
            return true
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // Render title
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false)

        // Don't render inventory label to save space
    }
}
