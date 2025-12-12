package io.github.heitorcordeiromaciel.network.packets

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.pokemon.Pokemon
import io.github.heitorcordeiromaciel.storage.HomeStorageManager
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.io.ByteArrayInputStream
import java.util.UUID

/**
 * Response packet from server to client after a Pokemon transfer.
 * Contains the Pokemon data to be added to Home storage.
 */
data class TransferPokemonResponsePacket(
    val pokemonUUID: String,
    val toHome: Boolean,
    val pokemonData: SendPCDataPacket.SerializedPokemon
) : CustomPacketPayload {

    companion object {
        val TYPE: CustomPacketPayload.Type<TransferPokemonResponsePacket> =
            CustomPacketPayload.Type(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    "cobblehome_neoforge",
                    "transfer_pokemon_response"
                )
            )

        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, TransferPokemonResponsePacket> =
            StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                TransferPokemonResponsePacket::pokemonUUID,
                ByteBufCodecs.BOOL,
                TransferPokemonResponsePacket::toHome,
                SendPCDataPacket.SerializedPokemon.STREAM_CODEC,
                TransferPokemonResponsePacket::pokemonData,
                ::TransferPokemonResponsePacket
            )

        @OnlyIn(Dist.CLIENT)
        fun handle(packet: TransferPokemonResponsePacket, context: IPayloadContext) {
            context.enqueueWork {
                try {
                    val uuid = UUID.fromString(packet.pokemonUUID)
                    
                    if (packet.toHome) {
                        // PC to Home transfer
                        // Deserialize Pokemon
                        val inputStream = ByteArrayInputStream(packet.pokemonData.nbtData)
                        val nbt = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap())
                        val registryAccess = net.minecraft.client.Minecraft.getInstance()
                            .connection?.registryAccess() ?: return@enqueueWork
                        
                        val pokemon = Pokemon.loadFromNBT(registryAccess, nbt)
                        
                        // Add to Home storage using the proper method that triggers save
                        HomeStorageManager.addPokemon(pokemon)
                        
                        // Remove from PC cache
                        val currentCache = io.github.heitorcordeiromaciel.storage.PCAccessor.getAllPCPokemon()
                        val updatedCache = currentCache.filter { it.uuid != uuid }
                        io.github.heitorcordeiromaciel.storage.PCAccessor.updatePCCache(updatedCache)
                        
                        Cobblemon.LOGGER.info("Added ${pokemon.species.name} to Home storage and removed from PC cache")
                    } else {
                        // Home to PC transfer
                        // Find Pokemon in Home storage
                        val homeStore = HomeStorageManager.getHomeStore()
                        val pokemon = homeStore.getAllSlots().find { it?.uuid == uuid }
                        
                        if (pokemon != null) {
                            // Remove from Home using the proper method that triggers save
                            HomeStorageManager.removePokemon(pokemon)
                            
                            // Serialize Pokemon and send to server
                            val registryAccess = net.minecraft.client.Minecraft.getInstance()
                                .connection?.registryAccess() ?: return@enqueueWork
                            
                            val nbt = pokemon.saveToNBT(registryAccess)
                            val outputStream = java.io.ByteArrayOutputStream()
                            net.minecraft.nbt.NbtIo.writeCompressed(nbt, outputStream)
                            
                            val transferPacket = io.github.heitorcordeiromaciel.network.packets.TransferToPCPacket(
                                pokemonData = SendPCDataPacket.SerializedPokemon(
                                    uuid = pokemon.uuid.toString(),
                                    nbtData = outputStream.toByteArray()
                                )
                            )
                            net.neoforged.neoforge.network.PacketDistributor.sendToServer(transferPacket)
                            
                            Cobblemon.LOGGER.info("Removed ${pokemon.species.name} from Home storage, sending to PC")
                        }
                    }
                } catch (e: Exception) {
                    Cobblemon.LOGGER.error("Failed to handle Pokemon transfer response", e)
                }
            }
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}
