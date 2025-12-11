package io.github.heitorcordeiromaciel.network.packets

import com.cobblemon.mod.common.Cobblemon
import io.github.heitorcordeiromaciel.server.TransferValidator
import io.netty.buffer.ByteBuf
import java.util.UUID
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.network.handling.IPayloadContext

/**
 * Client -> Server: Transfer Pokémon from PC to Home Server validates, removes from PC, and sends
 * Pokémon data to client
 */
data class TransferToHomePacket(val pokemonUUID: String) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE: CustomPacketPayload.Type<TransferToHomePacket> =
                CustomPacketPayload.Type(
                        ResourceLocation.fromNamespaceAndPath(
                                "cobblehome_neoforge",
                                "transfer_to_home"
                        )
                )

        val STREAM_CODEC: StreamCodec<ByteBuf, TransferToHomePacket> =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8,
                        TransferToHomePacket::pokemonUUID,
                        ::TransferToHomePacket
                )

        fun handle(packet: TransferToHomePacket, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player()

                if (player is net.minecraft.server.level.ServerPlayer) {
                    try {
                        val uuid = UUID.fromString(packet.pokemonUUID)

                        // Validate and perform transfer
                        val result = TransferValidator.transferToHome(player, uuid)

                        if (result.success) {
                            Cobblemon.LOGGER.info(
                                    "Player ${player.name.string} transferred Pokémon $uuid to Home"
                            )
                        } else {
                            Cobblemon.LOGGER.warn(
                                    "Failed to transfer Pokémon $uuid to Home: ${result.message}"
                            )
                        }

                        // Send result back to client
                        context.reply(
                                TransferResultPacket(
                                        result.success,
                                        result.message,
                                        result.pokemonData
                                )
                        )
                    } catch (e: Exception) {
                        Cobblemon.LOGGER.error("Error handling transfer to home", e)
                        context.reply(
                                TransferResultPacket(false, "Internal error: ${e.message}", null)
                        )
                    }
                }
            }
        }
    }
}
