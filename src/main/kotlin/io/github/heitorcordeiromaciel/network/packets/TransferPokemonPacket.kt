package io.github.heitorcordeiromaciel.network.packets

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.storage.pc.PCStore
import io.github.heitorcordeiromaciel.storage.HomeStorageManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.util.UUID

/**
 * Packet sent from client to server to transfer a Pokemon between PC and Home storage.
 */
data class TransferPokemonPacket(
    val pokemonUUID: String,
    val fromPC: Boolean  // true = PC to Home, false = Home to PC
) : CustomPacketPayload {

    companion object {
        val TYPE: CustomPacketPayload.Type<TransferPokemonPacket> =
            CustomPacketPayload.Type(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    "cobblehome_neoforge",
                    "transfer_pokemon"
                )
            )

        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, TransferPokemonPacket> =
            StreamCodec.composite(
                net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8,
                TransferPokemonPacket::pokemonUUID,
                net.minecraft.network.codec.ByteBufCodecs.BOOL,
                TransferPokemonPacket::fromPC,
                ::TransferPokemonPacket
            )

        fun handle(packet: TransferPokemonPacket, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player()
                
                if (player is ServerPlayer) {
                    try {
                        val uuid = UUID.fromString(packet.pokemonUUID)
                        
                        if (packet.fromPC) {
                            // Transfer from PC to Home
                            val pc = Cobblemon.storage.getPC(player)
                            val pokemon = pc.toList().find { it.uuid == uuid }
                            
                            if (pokemon != null) {
                                // Remove from PC
                                pc.remove(pokemon)
                                
                                // Send to client to add to Home storage
                                val response = TransferPokemonResponsePacket(
                                    pokemonUUID = packet.pokemonUUID,
                                    toHome = true,
                                    pokemonData = SendPCDataPacket.SerializedPokemon(
                                        uuid = pokemon.uuid.toString(),
                                        nbtData = run {
                                            val nbt = pokemon.saveToNBT(player.server.registryAccess())
                                            val outputStream = java.io.ByteArrayOutputStream()
                                            net.minecraft.nbt.NbtIo.writeCompressed(nbt, outputStream)
                                            outputStream.toByteArray()
                                        }
                                    )
                                )
                                context.reply(response)
                                
                                Cobblemon.LOGGER.info("Transferred ${pokemon.species.name} from PC to Home for ${player.name.string}")
                            }
                        } else {
                            // Transfer from Home to PC
                            // Client will send the Pokemon data, we just need to acknowledge
                            // For now, we'll handle it by having client remove from Home
                            // and server add to PC
                            
                            // Send response to client to remove from Home
                            val response = TransferPokemonResponsePacket(
                                pokemonUUID = packet.pokemonUUID,
                                toHome = false,
                                pokemonData = SendPCDataPacket.SerializedPokemon(
                                    uuid = packet.pokemonUUID,
                                    nbtData = ByteArray(0) // Empty, client will handle removal
                                )
                            )
                            context.reply(response)
                            
                            Cobblemon.LOGGER.info("Received Home to PC transfer request for ${player.name.string}")
                        }
                    } catch (e: Exception) {
                        Cobblemon.LOGGER.error("Failed to transfer Pokemon", e)
                    }
                }
            }
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}
