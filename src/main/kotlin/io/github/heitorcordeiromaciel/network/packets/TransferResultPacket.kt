package io.github.heitorcordeiromaciel.network.packets

import io.github.heitorcordeiromaciel.storage.HomeStorageManager
import io.netty.buffer.ByteBuf
import java.io.ByteArrayInputStream
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.network.handling.IPayloadContext

/**
 * Server -> Client: Transfer result Indicates success/failure and optionally contains Pokémon data
 * for Home storage
 */
data class TransferResultPacket(
        val success: Boolean,
        val message: String,
        val pokemonData: ByteArray?
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TransferResultPacket
        if (success != other.success) return false
        if (message != other.message) return false
        if (pokemonData != null) {
            if (other.pokemonData == null) return false
            if (!pokemonData.contentEquals(other.pokemonData)) return false
        } else if (other.pokemonData != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = success.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + (pokemonData?.contentHashCode() ?: 0)
        return result
    }

    companion object {
        val TYPE: CustomPacketPayload.Type<TransferResultPacket> =
                CustomPacketPayload.Type(
                        ResourceLocation.fromNamespaceAndPath(
                                "cobblehome_neoforge",
                                "transfer_result"
                        )
                )

        val STREAM_CODEC: StreamCodec<ByteBuf, TransferResultPacket> =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL,
                        TransferResultPacket::success,
                        ByteBufCodecs.STRING_UTF8,
                        TransferResultPacket::message,
                        ByteBufCodecs.optional(ByteBufCodecs.BYTE_ARRAY),
                        {
                            it.pokemonData?.let { data -> java.util.Optional.of(data) }
                                    ?: java.util.Optional.empty()
                        },
                        { success, message, pokemonData ->
                            TransferResultPacket(success, message, pokemonData.orElse(null))
                        }
                )

        fun handle(packet: TransferResultPacket, context: IPayloadContext) {
            context.enqueueWork {
                if (packet.success) {
                    // If transferring to Home, add Pokémon to local storage
                    if (packet.pokemonData != null) {
                        try {
                            val inputStream = ByteArrayInputStream(packet.pokemonData)
                            val nbt =
                                    NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap())
                            val registryAccess =
                                    net.minecraft.client.Minecraft.getInstance()
                                            .connection
                                            ?.registryAccess()
                                            ?: return@enqueueWork

                            val pokemon =
                                    com.cobblemon.mod.common.pokemon.Pokemon.loadFromNBT(
                                            registryAccess,
                                            nbt
                                    )

                            // Add to home storage
                            HomeStorageManager.addPokemon(pokemon)

                            // Show success message
                            val player = net.minecraft.client.Minecraft.getInstance().player
                            player?.displayClientMessage(
                                    net.minecraft.network.chat.Component.literal(
                                            "Transferred ${pokemon.species.name} to Home!"
                                    ),
                                    false
                            )
                        } catch (e: Exception) {
                            val player = net.minecraft.client.Minecraft.getInstance().player
                            player?.displayClientMessage(
                                    net.minecraft.network.chat.Component.literal(
                                            "Error: Failed to add Pokémon to Home"
                                    ),
                                    false
                            )
                        }
                    } else {
                        // Transferring to PC succeeded
                        val player = net.minecraft.client.Minecraft.getInstance().player
                        player?.displayClientMessage(
                                net.minecraft.network.chat.Component.literal(
                                        "Transferred Pokémon to PC!"
                                ),
                                false
                        )
                    }
                } else {
                    // Show error message
                    val player = net.minecraft.client.Minecraft.getInstance().player
                    player?.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "Transfer failed: ${packet.message}"
                            ),
                            false
                    )
                }
            }
        }
    }
}
