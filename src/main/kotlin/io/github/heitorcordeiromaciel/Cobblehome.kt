package io.github.heitorcordeiromaciel

import com.cobblemon.mod.common.Cobblemon
import io.github.heitorcordeiromaciel.commands.CobbleHomeCommand
import io.github.heitorcordeiromaciel.storage.HomeStorageManager
import io.github.heitorcordeiromaciel.ui.MenuRegistry
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.Mod
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Mod("cobblehome_neoforge")
class Cobblehome {

  init {
    // Register to NeoForge event bus
    NeoForge.EVENT_BUS.register(this)

    // Register menu types to mod event bus
    MenuRegistry.MENU_TYPES.register(MOD_BUS)

    Cobblemon.LOGGER.info("CobbleHome initialized")
  }

  @SubscribeEvent
  fun onServerStarted(event: ServerStartedEvent) {
    // Initialize storage manager when server starts
    val configDir = FMLPaths.CONFIGDIR.get().toFile()
    HomeStorageManager.initialize(configDir)
    Cobblemon.LOGGER.info("CobbleHome storage manager initialized")
  }

  @SubscribeEvent
  fun onServerStopping(event: ServerStoppingEvent) {
    // Save and shutdown storage manager when server stops
    HomeStorageManager.shutdown()
    Cobblemon.LOGGER.info("CobbleHome storage manager shut down")
  }

  @SubscribeEvent
  fun onCommandRegistration(event: RegisterCommandsEvent) {
    CobbleHomeCommand.register(event.dispatcher)
    Cobblemon.LOGGER.info("CobbleHome commands registered")
  }
}
