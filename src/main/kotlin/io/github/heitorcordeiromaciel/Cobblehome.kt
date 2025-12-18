package io.github.heitorcordeiromaciel

import com.cobblemon.mod.common.Cobblemon
import io.github.heitorcordeiromaciel.commands.CobbleHomeCommand
import io.github.heitorcordeiromaciel.config.CobbleHomeConfig
import io.github.heitorcordeiromaciel.ui.MenuRegistry
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.ModContainer
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Mod("cobblehome_neoforge")
class Cobblehome(container: ModContainer) {

  init {
    // Register to NeoForge event bus
    NeoForge.EVENT_BUS.register(this)

    // Register menu types to mod event bus
    MenuRegistry.MENU_TYPES.register(MOD_BUS)

    container.registerConfig(
      ModConfig.Type.CLIENT,
      CobbleHomeConfig.SPEC,
      "cobblehome/cobblehome.toml"
    )

    Cobblemon.LOGGER.info("CobbleHome initialized (client-side storage)")
  }

  @SubscribeEvent
  fun onCommandRegistration(event: RegisterCommandsEvent) {
    CobbleHomeCommand.register(event.dispatcher)
    Cobblemon.LOGGER.info("CobbleHome commands registered")
  }
}
