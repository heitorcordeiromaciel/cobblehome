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
        initialize()
    }

    /** Save and shutdown on client logout */
    @SubscribeEvent
    fun onClientLogout(event: ClientPlayerNetworkEvent.LoggingOut) {
        // Only save if we still have registry access
        if (Minecraft.getInstance().connection != null) {
            save()
        }
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

        // Load or create store
        homeStore =
                if (storageFile.exists()) {
                    loadFromFile()
                } else {
                    // Create new store with a fixed UUID for cross-world persistence
                    HomeStore(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                }

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
        save()
    }

    /** Removes a Pokémon from home storage */
    fun removePokemon(pokemon: Pokemon): Boolean {
        if (!initialized) {
            initialize()
        }

        val removed = homeStore.remove(pokemon)
        if (removed) {
            save()
        }
        return removed
    }

    /** Saves the home store to disk */
    fun save() {
        if (!initialized) return

        try {
            val nbt = CompoundTag()
            val registryAccess =
                    Minecraft.getInstance().connection?.registryAccess()
                            ?: throw IllegalStateException("Cannot save without registry access")

            homeStore.saveToNBT(nbt, registryAccess)

            // Ensure parent directory exists
            storageFile.parentFile?.mkdirs()

            // Write to file
            NbtIo.writeCompressed(nbt, storageFile.toPath())
            Cobblemon.LOGGER.debug("CobbleHome client storage saved successfully")
        } catch (e: IOException) {
            Cobblemon.LOGGER.error("Failed to save CobbleHome client storage", e)
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
