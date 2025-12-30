package io.github.heitorcordeiromaciel.storage

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.pokemon.Pokemon
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.heitorcordeiromaciel.config.CobbleHomeConfig
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.UUID
import net.minecraft.client.Minecraft
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
object HomeStorageManager {

    private lateinit var homeStore: HomeStore
    private lateinit var storageFile: File
    private var initialized = false

    /** Initialize on client login */
    /** Loads the home storage from disk */
    fun load() {
        initialize()

        // Always load from disk when joining a world
        if (storageFile.exists()) {
            try {
                Cobblemon.LOGGER.info("Loading Home storage from disk...")
                val loadedStore = loadFromFile()
                homeStore = loadedStore
                Cobblemon.LOGGER.info(
                        "✅ Loaded Home storage with ${homeStore.getOccupiedCount()} Pokemon"
                )
            } catch (e: Exception) {
                Cobblemon.LOGGER.error("Failed to load Home storage on login", e)
            }
        } else {
            Cobblemon.LOGGER.info("No save file found, starting with empty Home storage")
        }
    }

    private fun resolveHomeDirectory(): File {
        return if (CobbleHomeConfig.VALUES.enableGlobalHome.get()) {
            val appData = System.getenv("APPDATA")
            File(appData, "PokemonVault")
        } else {
            File(Minecraft.getInstance().gameDirectory, "config/cobblemon-vault")
        }
    }

    /** Initializes the storage manager and loads existing data */
    fun initialize() {
        if (initialized) return

        // Get client's config directory
        val baseDir = resolveHomeDirectory()

        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }

        storageFile = File(baseDir, "vault.json")

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
        if (!initialized) {
            initialize()
        }

        homeStore.add(pokemon)
        save() // Save immediately after adding
    }

    /** Removes a Pokémon from home storage */
    fun removePokemon(pokemon: Pokemon): Boolean {
        if (!initialized) {
            initialize()
        }

        val removed = homeStore.remove(pokemon)
        if (removed) {
            save() // Save immediately after removing
        } else {
            Cobblemon.LOGGER.warn("Failed to remove Pokemon from store")
        }
        return removed
    }

    /** Saves the home store to disk */
    fun save() {
        if (!initialized) {
            return
        }

        try {
            val json = JsonObject()
            val registryAccess =
                    Minecraft.getInstance().connection?.registryAccess()
                            ?: run {
                                Cobblemon.LOGGER.warn(
                                        "Cannot save Home storage: no registry access"
                                )
                                return
                            }

            homeStore.saveToJSON(json, registryAccess)

            // Ensure parent directory exists
            storageFile.parentFile?.mkdirs()

            // Write to file
            val gson = GsonBuilder().setPrettyPrinting().create()
            FileWriter(storageFile).use { writer -> gson.toJson(json, writer) }
        } catch (e: IOException) {
            Cobblemon.LOGGER.error("Failed to save CobbleHome client storage", e)
        } catch (e: Exception) {
            Cobblemon.LOGGER.error("Unexpected error saving CobbleHome storage", e)
        }
    }

    /** Loads the home store from disk */
    private fun loadFromFile(): HomeStore {
        return try {
            val registryAccess =
                    Minecraft.getInstance().connection?.registryAccess()
                            ?: throw IllegalStateException("Cannot load without registry access")

            val json =
                    FileReader(storageFile).use { reader ->
                        JsonParser.parseReader(reader).asJsonObject
                    }

            val uuidString =
                    if (json.has("UUID")) json.get("UUID").asString
                    else "00000000-0000-0000-0000-000000000001"
            val uuid = UUID.fromString(uuidString)
            val store = HomeStore(uuid)
            store.loadFromJSON(json, registryAccess)

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
