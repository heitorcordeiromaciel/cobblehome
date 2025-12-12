package io.github.heitorcordeiromaciel.network.packets

import com.cobblemon.mod.common.api.storage.pc.PCStore
import com.cobblemon.mod.common.pokemon.Pokemon
import io.github.heitorcordeiromaciel.storage.PCAccessor
import io.netty.buffer.ByteBuf
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.network.handling.IPayloadContext

/** Server -> Client: Send PC data Contains serialized Pokémon from player's PC */
data class SendPCDataPacket(val pokemonData: List<SerializedPokemon>) : CustomPacketPayload {

        override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

        data class SerializedPokemon(val uuid: String, val nbtData: ByteArray) {
                override fun equals(other: Any?): Boolean {
                        if (this === other) return true
                        if (javaClass != other?.javaClass) return false
                        other as SerializedPokemon
                        if (uuid != other.uuid) return false
                        if (!nbtData.contentEquals(other.nbtData)) return false
                        return true
                }

                override fun hashCode(): Int {
                        var result = uuid.hashCode()
                        result = 31 * result + nbtData.contentHashCode()
                        return result
                }
                
                companion object {
                        val STREAM_CODEC: StreamCodec<ByteBuf, SerializedPokemon> =
                                StreamCodec.composite(
                                        ByteBufCodecs.STRING_UTF8,
                                        SerializedPokemon::uuid,
                                        ByteBufCodecs.BYTE_ARRAY,
                                        SerializedPokemon::nbtData,
                                        ::SerializedPokemon
                                )
                }
        }

        companion object {
                val TYPE: CustomPacketPayload.Type<SendPCDataPacket> =
                        CustomPacketPayload.Type(
                                ResourceLocation.fromNamespaceAndPath(
                                        "cobblehome_neoforge",
                                        "send_pc_data"
                                )
                        )

                val STREAM_CODEC: StreamCodec<ByteBuf, SendPCDataPacket> =
                        StreamCodec.composite(
                                ByteBufCodecs.collection(
                                        { ArrayList() },
                                        SerializedPokemon.STREAM_CODEC
                                ),
                                SendPCDataPacket::pokemonData,
                                ::SendPCDataPacket
                        )

                fun fromPCStore(
                        pc: PCStore,
                        server: net.minecraft.server.MinecraftServer
                ): SendPCDataPacket {
                        val pokemonList = pc.toList()
                        com.cobblemon.mod.common.Cobblemon.LOGGER.info(
                                "SendPCDataPacket.fromPCStore: Converting ${pokemonList.size} Pokémon to packet"
                        )
                        
                        val serialized =
                                pokemonList.map { pokemon ->
                                        val registryAccess = server.registryAccess()

                                        // Create NBT and save the full Pokemon data
                                        val nbt = pokemon.saveToNBT(registryAccess)

                                        val outputStream = ByteArrayOutputStream()
                                        NbtIo.writeCompressed(nbt, outputStream)

                                        SerializedPokemon(
                                                uuid = pokemon.uuid.toString(),
                                                nbtData = outputStream.toByteArray()
                                        )
                                }
                        
                        com.cobblemon.mod.common.Cobblemon.LOGGER.info(
                                "SendPCDataPacket.fromPCStore: Serialized ${serialized.size} Pokémon"
                        )

                        return SendPCDataPacket(serialized)
                }

                fun handle(packet: SendPCDataPacket, context: IPayloadContext) {
                        context.enqueueWork {
                                com.cobblemon.mod.common.Cobblemon.LOGGER.info(
                                        "SendPCDataPacket.handle: Received packet with ${packet.pokemonData.size} serialized Pokémon"
                                )
                                
                                // Deserialize Pokémon and cache on client
                                val pokemonList =
                                        packet.pokemonData.mapNotNull { serialized ->
                                                try {
                                                        val inputStream =
                                                                ByteArrayInputStream(
                                                                        serialized.nbtData
                                                                )
                                                        val nbt =
                                                                NbtIo.readCompressed(
                                                                        inputStream,
                                                                        NbtAccounter.unlimitedHeap()
                                                                )
                                                        val registryAccess =
                                                                net.minecraft.client.Minecraft
                                                                        .getInstance()
                                                                        .connection
                                                                        ?.registryAccess()
                                                                        ?: return@mapNotNull null

                                                         Pokemon.loadFromNBT(registryAccess, nbt)
                                                } catch (e: Exception) {
                                                        com.cobblemon.mod.common.Cobblemon.LOGGER.error(
                                                                "Failed to deserialize Pokémon", e
                                                        )
                                                        null
                                                }
                                        }

                                // Update PC cache
                                PCAccessor.updatePCCache(pokemonList)
                                com.cobblemon.mod.common.Cobblemon.LOGGER.info(
                                        "SendPCDataPacket: Cached ${pokemonList.size} Pokémon from PC"
                                )
                        }
                }
        }
}
