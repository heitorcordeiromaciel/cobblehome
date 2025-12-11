package io.github.heitorcordeiromaciel.storage

import com.cobblemon.mod.common.pokemon.Pokemon
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

/** CLIENT-SIDE accessor for PC data received from server */
@OnlyIn(Dist.CLIENT)
object PCAccessor {

    private var cachedPCPokemon: List<Pokemon> = emptyList()

    /** Updates the cached PC data (called when receiving SendPCDataPacket) */
    fun updatePCCache(pokemon: List<Pokemon>) {
        cachedPCPokemon = pokemon
    }

    /** Gets all cached PC Pokémon */
    fun getAllPCPokemon(): List<Pokemon> {
        return cachedPCPokemon
    }

    /** Clears the PC cache */
    fun clearCache() {
        cachedPCPokemon = emptyList()
    }
}
