package io.github.heitorcordeiromaciel.network.packets

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.pokemon.Pokemon
import io.github.heitorcordeiromaciel.server.TransferValidator
import io.netty.buffer.ByteBuf
import java.io.ByteArrayInputStream
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.network.handling.IPayloadContext

/** Client -> Server: Transfer Pokémon from Home to PC Contains serialized Pokémon data */
data class TransferToPCPacket(val pokemonData: ByteArray) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TransferToPCPacket
        return pokemonData.contentEquals(other.pokemonData)
    }

    override fun hashCode(): Int {
        return pokemonData.contentHashCode()
    }

    companion object {
        val TYPE: CustomPacketPayload.Type<TransferToPCPacket> =
                CustomPacketPayload.Type(
                        ResourceLocation.fromNamespaceAndPath(
                                "cobblehome_neoforge",
                                "transfer_to_pc"
                        )
                )

        val STREAM_CODEC: StreamCodec<ByteBuf, TransferToPCPacket> =
                StreamCodec.composite(
                        ByteBufCodecs.BYTE_ARRAY,
                        TransferToPCPacket::pokemonData,
                        ::TransferToPCPacket
                )

        fun handle(packet: TransferToPCPacket, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player()

                if (player is net.minecraft.server.level.ServerPlayer) {
                    try {
                        // Deserialize Pokémon
                        val inputStream = ByteArrayInputStream(packet.pokemonData)
                        val nbt = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap())
                        val registryAccess = player.server.registryAccess()

                        val pokemon = Pokemon.loadFromNBT(registryAccess, nbt)

                        // Validate and perform transfer
                        val result = TransferValidator.transferToPC(player, pokemon)

                        if (result.success) {
                            Cobblemon.LOGGER.info(
                                    "Player ${player.name.string} transferred Pokémon ${pokemon.uuid} to PC"
                            )
                        } else {
                            Cobblemon.LOGGER.warn(
                                    "Failed to transfer Pokémon ${pokemon.uuid} to PC: ${result.message}"
                            )
                        }

                        // Send result back to client
                        context.reply(TransferResultPacket(result.success, result.message, null))
                    } catch (e: Exception) {
                        Cobblemon.LOGGER.error("Error handling transfer to PC", e)
                        context.reply(
                                TransferResultPacket(false, "Internal error: ${e.message}", null)
                        )
                    }
                }
            }
        }
    }
}
