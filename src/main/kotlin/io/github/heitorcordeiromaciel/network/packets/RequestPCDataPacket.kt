package io.github.heitorcordeiromaciel.network.packets

import com.cobblemon.mod.common.Cobblemon
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.network.handling.IPayloadContext

/** Client -> Server: Request PC data Sent when player opens CobbleHome UI */
data class RequestPCDataPacket(val dummy: Boolean = true) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE: CustomPacketPayload.Type<RequestPCDataPacket> =
                CustomPacketPayload.Type(
                        ResourceLocation.fromNamespaceAndPath(
                                "cobblehome_neoforge",
                                "request_pc_data"
                        )
                )

        val STREAM_CODEC: StreamCodec<ByteBuf, RequestPCDataPacket> =
                StreamCodec.of(
                        { _, _ -> }, // Write - nothing to write
                        { RequestPCDataPacket() } // Read - create empty packet
                )

        fun handle(packet: RequestPCDataPacket, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player()

                if (player is net.minecraft.server.level.ServerPlayer) {
                    try {
                        // Get player's PC
                        val pc = Cobblemon.storage.getPC(player)
                        val pcPokemonCount = pc.toList().size
                        
                        Cobblemon.LOGGER.info("RequestPCDataPacket: Player ${player.name.string} has $pcPokemonCount Pokémon in PC")

                        // Send PC data back to client
                        val response = SendPCDataPacket.fromPCStore(pc, player.server)
                        context.reply(response)

                        Cobblemon.LOGGER.info("Sent PC data to ${player.name.string}")
                    } catch (e: Exception) {
                        Cobblemon.LOGGER.error("Failed to send PC data to player", e)
                        // Send empty response
                        context.reply(SendPCDataPacket(emptyList()))
                    }
                }
            }
        }
    }
}
