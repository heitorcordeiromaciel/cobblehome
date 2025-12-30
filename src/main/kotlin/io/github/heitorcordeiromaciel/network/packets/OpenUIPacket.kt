package io.github.heitorcordeiromaciel.network.packets

import io.github.heitorcordeiromaciel.ui.CobbleHomeMenu
import io.github.heitorcordeiromaciel.ui.CobbleHomeScreen
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.network.handling.IPayloadContext

/** Server -> Client: Opens the CobbleHome UI on the client */
data class OpenUIPacket(val dummy: Boolean = true) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE: CustomPacketPayload.Type<OpenUIPacket> =
                CustomPacketPayload.Type(
                        ResourceLocation.fromNamespaceAndPath(
                                "cobblehome_neoforge",
                                "open_ui"
                        )
                )

        val STREAM_CODEC: StreamCodec<ByteBuf, OpenUIPacket> =
                StreamCodec.of(
                        { _, _ -> }, // Write - nothing to write
                        { OpenUIPacket() } // Read - create empty packet
                )

        @OnlyIn(Dist.CLIENT)
        fun handle(packet: OpenUIPacket, context: IPayloadContext) {
            context.enqueueWork {
                val minecraft = Minecraft.getInstance()
                val player = minecraft.player ?: return@enqueueWork

                // Load storage from disk to ensure we have latest data
                io.github.heitorcordeiromaciel.storage.HomeStorageManager.load()

                // Create and open the menu client-side
                val menu = CobbleHomeMenu(
                        0, // containerId - will be assigned by Minecraft
                        player.inventory
                )

                // Open the screen
                minecraft.setScreen(CobbleHomeScreen(menu, player.inventory, Component.literal("CobbleHome")))
            }
        }
    }
}
