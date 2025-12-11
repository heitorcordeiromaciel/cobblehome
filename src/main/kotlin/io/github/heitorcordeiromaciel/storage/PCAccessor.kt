package io.github.heitorcordeiromaciel.storage

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.storage.pc.PCStore
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.server.level.ServerPlayer

/** Provides read-only access to the player's PC storage. */
object PCAccessor {

    /** Gets the player's PC store */
    fun getPlayerPC(player: ServerPlayer): PCStore? {
        return try {
            Cobblemon.storage.getPC(player)
        } catch (e: Exception) {
            Cobblemon.LOGGER.error("Failed to access player PC", e)
            null
        }
    }

    /** Gets all Pokémon from the player's PC as a flat list */
    fun getAllPCPokemon(player: ServerPlayer): List<Pokemon> {
        val pc = getPlayerPC(player) ?: return emptyList()
        return pc.toList()
    }

    /** Gets Pokémon from a specific PC box */
    fun getBoxPokemon(player: ServerPlayer, boxIndex: Int): List<Pokemon?> {
        val pc = getPlayerPC(player) ?: return emptyList()

        if (boxIndex !in pc.boxes.indices) {
            return emptyList()
        }

        return pc.boxes[boxIndex].toList()
    }

    /** Gets the number of boxes in the player's PC */
    fun getBoxCount(player: ServerPlayer): Int {
        val pc = getPlayerPC(player) ?: return 0
        return pc.boxes.size
    }

    /** Gets the name of a specific box */
    fun getBoxName(player: ServerPlayer, boxIndex: Int): String {
        val pc = getPlayerPC(player) ?: return "Box ${boxIndex + 1}"

        if (boxIndex !in pc.boxes.indices) {
            return "Box ${boxIndex + 1}"
        }

        // Use default box name for now - Component text access is complex
        return "Box ${boxIndex + 1}"
    }
}
