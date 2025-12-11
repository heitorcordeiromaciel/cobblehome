package io.github.heitorcordeiromaciel.storage

import com.cobblemon.mod.common.Cobblemon
import java.io.File
import java.io.IOException
import java.util.UUID
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.server.level.ServerPlayer

/**
 * Manages the persistence of HomeStore to disk. Stores data in
 * .minecraft/config/cobblehome/home_storage.dat
 */
object HomeStorageManager {

    private lateinit var homeStore: HomeStore
    private lateinit var storageFile: File
    private var initialized = false

    /** Initializes the storage manager and loads existing data */
    fun initialize(configDir: File) {
        if (initialized) return

        // Create config directory
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
        Cobblemon.LOGGER.info("CobbleHome storage initialized at: ${storageFile.absolutePath}")
    }

    /** Gets the home store instance */
    fun getHomeStore(): HomeStore {
        if (!initialized) {
            throw IllegalStateException("HomeStorageManager not initialized")
        }
        return homeStore
    }

    /** Saves the home store to disk */
    fun save() {
        if (!initialized) return

        try {
            val nbt = CompoundTag()
            val registryAccess =
                    Cobblemon.implementation.server()?.registryAccess()
                            ?: throw IllegalStateException(
                                    "Cannot save without server registry access"
                            )

            homeStore.saveToNBT(nbt, registryAccess)

            // Ensure parent directory exists
            storageFile.parentFile?.mkdirs()

            // Write to file
            NbtIo.writeCompressed(nbt, storageFile.toPath())
            Cobblemon.LOGGER.debug("CobbleHome storage saved successfully")
        } catch (e: IOException) {
            Cobblemon.LOGGER.error("Failed to save CobbleHome storage", e)
        }
    }

    /** Loads the home store from disk */
    private fun loadFromFile(): HomeStore {
        return try {
            val nbt =
                    NbtIo.readCompressed(
                            storageFile.toPath(),
                            net.minecraft.nbt.NbtAccounter.unlimitedHeap()
                    )
            val registryAccess =
                    Cobblemon.implementation.server()?.registryAccess()
                            ?: throw IllegalStateException(
                                    "Cannot load without server registry access"
                            )

            val uuidString =
                    if (nbt.contains("UUID")) nbt.getString("UUID")
                    else "00000000-0000-0000-0000-000000000001"
            val uuid = UUID.fromString(uuidString)
            val store = HomeStore(uuid)
            store.loadFromNBT(nbt, registryAccess)

            Cobblemon.LOGGER.info(
                    "CobbleHome storage loaded successfully with ${store.getOccupiedCount()} Pokémon"
            )
            store
        } catch (e: Exception) {
            Cobblemon.LOGGER.error("Failed to load CobbleHome storage, creating new store", e)
            HomeStore(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        }
    }

    /** Adds a player as an observer to the home store */
    fun addObserver(player: ServerPlayer) {
        homeStore.addObserver(player.uuid)
    }

    /** Removes a player as an observer from the home store */
    fun removeObserver(playerUUID: UUID) {
        homeStore.removeObserver(playerUUID)
    }

    /** Cleans up resources */
    fun shutdown() {
        if (initialized) {
            save()
            initialized = false
        }
    }
}
