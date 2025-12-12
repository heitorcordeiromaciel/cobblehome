package io.github.heitorcordeiromaciel.storage

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.reactive.Observable
import com.cobblemon.mod.common.api.reactive.SimpleObservable
import com.cobblemon.mod.common.api.storage.PokemonStore
import com.cobblemon.mod.common.api.storage.StoreCoordinates
import com.cobblemon.mod.common.pokemon.Pokemon
import com.google.gson.JsonObject
import java.util.UUID
import net.minecraft.core.RegistryAccess
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerPlayer

/**
 * Custom PokemonStore implementation for CobbleHome. Stores Pokémon in a bottomless list that
 * persists across worlds.
 */
class HomeStore(override val uuid: UUID) : PokemonStore<HomePosition>() {

    private val pokemon = mutableListOf<Pokemon?>()
    private val changeObservable = SimpleObservable<Unit>()
    private val observerUUIDs = mutableSetOf<UUID>()

    override fun iterator(): Iterator<Pokemon> = pokemon.filterNotNull().iterator()

    override fun get(position: HomePosition): Pokemon? {
        return if (position.index in pokemon.indices) {
            pokemon[position.index]
        } else {
            null
        }
    }

    override fun getFirstAvailablePosition(): HomePosition? {
        // Find first null slot
        val index = pokemon.indexOfFirst { it == null }
        if (index != -1) {
            return HomePosition(index)
        }
        // If no null slots, add to end
        return HomePosition(pokemon.size)
    }

    override fun getObservingPlayers(): Iterable<ServerPlayer> {
        return Cobblemon.implementation.server()?.playerList?.players?.filter {
            it.uuid in observerUUIDs
        }
                ?: emptyList()
    }

    override fun sendTo(player: ServerPlayer) {
        // For now, we'll handle UI updates differently
        // This would typically send initialization packets
    }

    override fun onPokemonChanged(pokemon: Pokemon) {
        changeObservable.emit(Unit)
    }

    override fun initialize() {
        pokemon.forEachIndexed { index, poke ->
            poke?.storeCoordinates?.set(StoreCoordinates(this, HomePosition(index)))
        }
    }

    override fun setAtPosition(position: HomePosition, pokemon: Pokemon?) {
        // Expand list if necessary
        while (position.index >= this.pokemon.size) {
            this.pokemon.add(null)
        }
        this.pokemon[position.index] = pokemon
        changeObservable.emit(Unit)
    }

    override fun isValidPosition(position: HomePosition): Boolean {
        return position.index >= 0
    }

    override fun saveToNBT(nbt: CompoundTag, registryAccess: RegistryAccess): CompoundTag {
        Cobblemon.LOGGER.info("=== HomeStore.saveToNBT CALLED ===")
        Cobblemon.LOGGER.info("Saving ${getOccupiedCount()} Pokemon out of ${pokemon.size} slots")
        
        nbt.putString("UUID", uuid.toString())

        val pokemonList = net.minecraft.nbt.ListTag()
        pokemon.forEachIndexed { index, poke ->
            if (poke != null) {
                Cobblemon.LOGGER.info("Saving Pokemon at slot $index: ${poke.species.name}")
                val pokemonTag = poke.saveToNBT(registryAccess)
                pokemonTag.putInt("Slot", index)
                pokemonList.add(pokemonTag)
            }
        }

        nbt.put("Pokemon", pokemonList)
        Cobblemon.LOGGER.info("Saved ${pokemonList.size} Pokemon to NBT")
        return nbt
    }

    override fun loadFromNBT(nbt: CompoundTag, registryAccess: RegistryAccess): PokemonStore<HomePosition> {
        Cobblemon.LOGGER.info("=== HomeStore.loadFromNBT CALLED ===")
        Cobblemon.LOGGER.info("NBT keys: ${nbt.allKeys}")
        
        pokemon.clear()

        val uuidString = nbt.getString("UUID")
        Cobblemon.LOGGER.info("Loading UUID: $uuidString")
        if (uuidString.isNotEmpty() && UUID.fromString(uuidString) != uuid) {
            Cobblemon.LOGGER.warn("HomeStore UUID mismatch during load")
        }

        val pokemonList = nbt.getList("Pokemon", 10) // 10 = CompoundTag
        Cobblemon.LOGGER.info("Pokemon list size: ${pokemonList.size}")
        
        for (i in 0 until pokemonList.size) {
            val pokemonTag = pokemonList.getCompound(i)
            val slot = pokemonTag.getInt("Slot")
            Cobblemon.LOGGER.info("Loading Pokemon from slot $slot")

            try {
                val poke = Pokemon.loadFromNBT(registryAccess, pokemonTag)
                Cobblemon.LOGGER.info("Loaded Pokemon: ${poke.species.name}")

                // Expand list if necessary
                while (slot >= pokemon.size) {
                    pokemon.add(null)
                }
                pokemon[slot] = poke
            } catch (e: Exception) {
                Cobblemon.LOGGER.error("Failed to load Pokemon from slot $slot", e)
                handleInvalidSpeciesNBT(pokemonTag)
            }
        }

        Cobblemon.LOGGER.info("Loaded ${getOccupiedCount()} Pokemon into store")
        initialize()
        return this
    }

    override fun saveToJSON(json: JsonObject, registryAccess: RegistryAccess): JsonObject {
        // Not implementing JSON for now, NBT is sufficient
        return json
    }

    override fun loadFromJSON(
            json: JsonObject,
            registryAccess: RegistryAccess
    ): PokemonStore<HomePosition> {
        // Not implementing JSON for now, NBT is sufficient
        return this
    }

    override fun savePositionToNBT(position: HomePosition, nbt: CompoundTag) {
        nbt.putInt("Index", position.index)
    }

    override fun loadPositionFromNBT(nbt: CompoundTag): StoreCoordinates<HomePosition> {
        val index = nbt.getInt("Index")
        return StoreCoordinates(this, HomePosition(index))
    }

    override fun getAnyChangeObservable(): Observable<Unit> = changeObservable

    fun addObserver(playerUUID: UUID) {
        observerUUIDs.add(playerUUID)
    }

    fun removeObserver(playerUUID: UUID) {
        observerUUIDs.remove(playerUUID)
    }

    /** Gets all Pokémon in the store (including nulls for empty slots) */
    fun getAllSlots(): List<Pokemon?> = pokemon.toList()

    /** Gets the total number of slots (including empty ones) */
    fun getSlotCount(): Int = pokemon.size

    /** Gets the number of Pokémon actually stored */
    fun getOccupiedCount(): Int = pokemon.count { it != null }
}
