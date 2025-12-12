package io.github.heitorcordeiromaciel.network.packets

import com.cobblemon.mod.common.Cobblemon
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.handling.IPayloadContext

/**
 * Packet sent from client to server to transfer a Pokemon from Home to PC.
 * Contains the serialized Pokemon data.
 */
data class TransferToPCPacket(
    val pokemonData: SendPCDataPacket.SerializedPokemon
) : CustomPacketPayload {

    companion object {
        val TYPE: CustomPacketPayload.Type<TransferToPCPacket> =
            CustomPacketPayload.Type(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    "cobblehome_neoforge",
                    "transfer_to_pc"
                )
            )

        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, TransferToPCPacket> =
            StreamCodec.composite(
                SendPCDataPacket.SerializedPokemon.STREAM_CODEC,
                TransferToPCPacket::pokemonData,
                ::TransferToPCPacket
            )

        fun handle(packet: TransferToPCPacket, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player()
                
                if (player is ServerPlayer) {
                    try {
                        // Deserialize Pokemon
                        val inputStream = java.io.ByteArrayInputStream(packet.pokemonData.nbtData)
                        val nbt = net.minecraft.nbt.NbtIo.readCompressed(
                            inputStream,
                            net.minecraft.nbt.NbtAccounter.unlimitedHeap()
                        )
                        val pokemon = com.cobblemon.mod.common.pokemon.Pokemon.loadFromNBT(
                            player.server.registryAccess(),
                            nbt
                        )
                        
                        // Add to PC
                        val pc = Cobblemon.storage.getPC(player)
                        pc.add(pokemon)
                        
                        Cobblemon.LOGGER.info("Added ${pokemon.species.name} to PC for ${player.name.string}")
                        
                        // Send updated PC data back to client
                        val response = SendPCDataPacket.fromPCStore(pc, player.server)
                        context.reply(response)
                    } catch (e: Exception) {
                        Cobblemon.LOGGER.error("Failed to transfer Pokemon to PC", e)
                    }
                }
            }
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}
