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

    companion object {
        const val MAX_CAPACITY = 1400 // 50 boxes * 28 slots
    }

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
        // Find first null slot within current size
        val index = pokemon.indexOfFirst { it == null }
        if (index != -1 && index < MAX_CAPACITY) {
            return HomePosition(index)
        }
        
        // If current size is less than capacity, we can append
        if (pokemon.size < MAX_CAPACITY) {
            return HomePosition(pokemon.size)
        }
        
        // Store is full
        return null
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
        if (!isValidPosition(position)) {
             Cobblemon.LOGGER.warn("Attempted to set pokemon at invalid position: ${position.index}")
             return
        }
        
        // Expand list if necessary
        while (position.index >= this.pokemon.size) {
            this.pokemon.add(null)
        }
        this.pokemon[position.index] = pokemon
        changeObservable.emit(Unit)
    }

    override fun isValidPosition(position: HomePosition): Boolean {
        return position.index >= 0 && position.index < MAX_CAPACITY
    }

    override fun saveToNBT(nbt: CompoundTag, registryAccess: RegistryAccess): CompoundTag {
        nbt.putString("UUID", uuid.toString())

        val pokemonList = net.minecraft.nbt.ListTag()
        pokemon.forEachIndexed { index, poke ->
            if (poke != null) {
                val pokemonTag = poke.saveToNBT(registryAccess)
                pokemonTag.putInt("Slot", index)
                pokemonList.add(pokemonTag)
            }
        }

        nbt.put("Pokemon", pokemonList)
        return nbt
    }

    override fun loadFromNBT(nbt: CompoundTag, registryAccess: RegistryAccess): PokemonStore<HomePosition> {
        pokemon.clear()

        val uuidString = nbt.getString("UUID")
        if (uuidString.isNotEmpty() && UUID.fromString(uuidString) != uuid) {
            Cobblemon.LOGGER.warn("HomeStore UUID mismatch during load")
        }

        val pokemonList = nbt.getList("Pokemon", 10) // 10 = CompoundTag
        
        for (i in 0 until pokemonList.size) {
            val pokemonTag = pokemonList.getCompound(i)
            val slot = pokemonTag.getInt("Slot")

            try {
                val poke = Pokemon.loadFromNBT(registryAccess, pokemonTag)

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
