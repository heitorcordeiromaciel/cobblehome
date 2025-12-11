package io.github.heitorcordeiromaciel.server

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.pokemon.Pokemon
import java.io.ByteArrayOutputStream
import java.util.UUID
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.server.level.ServerPlayer

/** Server-side validation for Pokémon transfers */
object TransferValidator {

    data class TransferResult(
            val success: Boolean,
            val message: String,
            val pokemonData: ByteArray? = null
    )

    /** Validate and perform transfer from PC to Home */
    fun transferToHome(player: ServerPlayer, pokemonUUID: UUID): TransferResult {
        try {
            // Get player's PC
            val pc = Cobblemon.storage.getPC(player)

            // Find Pokémon in PC
            val pokemon = pc.find { it.uuid == pokemonUUID }

            if (pokemon == null) {
                return TransferResult(false, "Pokémon not found in PC")
            }

            // Remove from PC
            val removed = pc.remove(pokemon)

            if (!removed) {
                return TransferResult(false, "Failed to remove Pokémon from PC")
            }

            // Serialize Pokémon for client
            val nbt = CompoundTag()
            val registryAccess = player.server.registryAccess()
            pokemon.saveToNBT(registryAccess, nbt)

            val outputStream = ByteArrayOutputStream()
            NbtIo.writeCompressed(nbt, outputStream)

            Cobblemon.LOGGER.info(
                    "Transferred Pokémon ${pokemon.species.name} (${pokemon.uuid}) from PC to Home for player ${player.name.string}"
            )

            return TransferResult(
                    success = true,
                    message = "Successfully transferred to Home",
                    pokemonData = outputStream.toByteArray()
            )
        } catch (e: Exception) {
            Cobblemon.LOGGER.error("Error during transfer to Home", e)
            return TransferResult(false, "Internal error: ${e.message}")
        }
    }

    /** Validate and perform transfer from Home to PC */
    fun transferToPC(player: ServerPlayer, pokemon: Pokemon): TransferResult {
        try {
            // Get player's PC
            val pc = Cobblemon.storage.getPC(player)

            // Check if PC has space
            val availablePosition = pc.getFirstAvailablePosition()

            if (availablePosition == null) {
                return TransferResult(false, "PC is full")
            }

            // Check for duplication attempt
            val existing = pc.find { it.uuid == pokemon.uuid }
            if (existing != null) {
                return TransferResult(false, "Pokémon already exists in PC (duplication prevented)")
            }

            // Add to PC
            val added = pc.add(pokemon)

            if (!added) {
                return TransferResult(false, "Failed to add Pokémon to PC")
            }

            Cobblemon.LOGGER.info(
                    "Transferred Pokémon ${pokemon.species.name} (${pokemon.uuid}) from Home to PC for player ${player.name.string}"
            )

            return TransferResult(success = true, message = "Successfully transferred to PC")
        } catch (e: Exception) {
            Cobblemon.LOGGER.error("Error during transfer to PC", e)
            return TransferResult(false, "Internal error: ${e.message}")
        }
    }
}
