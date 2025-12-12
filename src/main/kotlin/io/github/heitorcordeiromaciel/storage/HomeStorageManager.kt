package io.github.heitorcordeiromaciel.storage

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.pokemon.Pokemon
import java.io.File
import java.io.IOException
import java.util.UUID
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent

/**
 * CLIENT-SIDE storage manager for CobbleHome. Stores Pokémon in client's
 * .minecraft/config/cobblehome/home_storage.dat
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = "cobblehome_neoforge", value = [Dist.CLIENT])
object HomeStorageManager {

    private lateinit var homeStore: HomeStore
    private lateinit var storageFile: File
    private var initialized = false

    /** Initialize on client login */
    @SubscribeEvent
    fun onClientLogin(event: ClientPlayerNetworkEvent.LoggingIn) {
        Cobblemon.LOGGER.info("=== CLIENT LOGIN EVENT ===")
        initialize()
        
        // Always load from disk when joining a world
        if (storageFile.exists()) {
            try {
                Cobblemon.LOGGER.info("Loading Home storage from disk...")
                val loadedStore = loadFromFile()
                homeStore = loadedStore
                Cobblemon.LOGGER.info("✅ Loaded Home storage with ${homeStore.getOccupiedCount()} Pokemon")
            } catch (e: Exception) {
                Cobblemon.LOGGER.error("Failed to load Home storage on login", e)
            }
        } else {
            Cobblemon.LOGGER.info("No save file found, starting with empty Home storage")
        }
    }

    /** Reset on logout */
    @SubscribeEvent
    fun onClientLogout(event: ClientPlayerNetworkEvent.LoggingOut) {
        Cobblemon.LOGGER.info("=== CLIENT LOGOUT EVENT ===")
        // No need to do anything here, data is saved on each transfer
    }

    /** Initializes the storage manager and loads existing data */
    fun initialize() {
        if (initialized) return

        // Get client's config directory
        val configDir = File(Minecraft.getInstance().gameDirectory, "config")
        val cobblehomeDir = File(configDir, "cobblehome")

        if (!cobblehomeDir.exists()) {
            cobblehomeDir.mkdirs()
        }

        storageFile = File(cobblehomeDir, "home_storage.dat")

        // Create empty store initially
        homeStore = HomeStore(UUID.fromString("00000000-0000-0000-0000-000000000001"))

        initialized = true
        Cobblemon.LOGGER.info(
                "CobbleHome client storage initialized at: ${storageFile.absolutePath}"
        )
    }

    /** Gets the home store instance */
    fun getHomeStore(): HomeStore {
        if (!initialized) {
            initialize()
        }
        return homeStore
    }

    /** Adds a Pokémon to home storage */
    fun addPokemon(pokemon: Pokemon) {
        Cobblemon.LOGGER.info("=== ADD POKEMON CALLED: ${pokemon.species.name} ===")
        if (!initialized) {
            initialize()
        }

        homeStore.add(pokemon)
        Cobblemon.LOGGER.info("Pokemon added to store, count now: ${homeStore.getOccupiedCount()}")
        save() // Save immediately after adding
    }

    /** Removes a Pokémon from home storage */
    fun removePokemon(pokemon: Pokemon): Boolean {
        Cobblemon.LOGGER.info("=== REMOVE POKEMON CALLED: ${pokemon.species.name} ===")
        if (!initialized) {
            initialize()
        }

        val removed = homeStore.remove(pokemon)
        if (removed) {
            Cobblemon.LOGGER.info("Pokemon removed from store, count now: ${homeStore.getOccupiedCount()}")
            save() // Save immediately after removing
        } else {
            Cobblemon.LOGGER.warn("Failed to remove Pokemon from store")
        }
        return removed
    }

    /** Saves the home store to disk */
    fun save() {
        Cobblemon.LOGGER.info("=== SAVE CALLED ===")
        Cobblemon.LOGGER.info("Initialized: $initialized")
        if (!initialized) {
            Cobblemon.LOGGER.warn("Save aborted: not initialized")
            return
        }

        try {
            Cobblemon.LOGGER.info("Creating NBT tag...")
            val nbt = CompoundTag()
            val registryAccess =
                    Minecraft.getInstance().connection?.registryAccess()
                            ?: run {
                                Cobblemon.LOGGER.warn("Cannot save Home storage: no registry access")
                                return
                            }

            Cobblemon.LOGGER.info("Saving to NBT...")
            homeStore.saveToNBT(nbt, registryAccess)
            
            Cobblemon.LOGGER.info("NBT keys: ${nbt.allKeys}")

            // Ensure parent directory exists
            storageFile.parentFile?.mkdirs()
            
            Cobblemon.LOGGER.info("Writing to file: ${storageFile.absolutePath}")
            // Write to file
            NbtIo.writeCompressed(nbt, storageFile.toPath())
            
            Cobblemon.LOGGER.info("✅ CobbleHome storage saved: ${homeStore.getOccupiedCount()} Pokemon to ${storageFile.absolutePath}")
            Cobblemon.LOGGER.info("File exists after save: ${storageFile.exists()}, size: ${storageFile.length()} bytes")
        } catch (e: IOException) {
            Cobblemon.LOGGER.error("Failed to save CobbleHome client storage", e)
        } catch (e: Exception) {
            Cobblemon.LOGGER.error("Unexpected error saving CobbleHome storage", e)
        }
    }

    /** Loads the home store from disk */
    private fun loadFromFile(): HomeStore {
        return try {
            val nbt = NbtIo.readCompressed(storageFile.toPath(), NbtAccounter.unlimitedHeap())
            val registryAccess =
                    Minecraft.getInstance().connection?.registryAccess()
                            ?: throw IllegalStateException("Cannot load without registry access")

            val uuidString =
                    if (nbt.contains("UUID")) nbt.getString("UUID")
                    else "00000000-0000-0000-0000-000000000001"
            val uuid = UUID.fromString(uuidString)
            val store = HomeStore(uuid)
            store.loadFromNBT(nbt, registryAccess)

            Cobblemon.LOGGER.info(
                    "CobbleHome client storage loaded successfully with ${store.getOccupiedCount()} Pokémon"
            )
            store
        } catch (e: Exception) {
            Cobblemon.LOGGER.error(
                    "Failed to load CobbleHome client storage, creating new store",
                    e
            )
            HomeStore(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        }
    }
}
