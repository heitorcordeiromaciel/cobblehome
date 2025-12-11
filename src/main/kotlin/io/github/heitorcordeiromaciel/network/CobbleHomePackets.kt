package io.github.heitorcordeiromaciel.network

import io.github.heitorcordeiromaciel.network.packets.*
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.registration.PayloadRegistrar

/** Registers all CobbleHome network packets */
@Suppress("DEPRECATION")
@EventBusSubscriber(modid = "cobblehome_neoforge", bus = EventBusSubscriber.Bus.MOD)
object CobbleHomePackets {

        const val PROTOCOL_VERSION = "1"

        val CHANNEL = ResourceLocation.fromNamespaceAndPath("cobblehome_neoforge", "main")

        @SubscribeEvent
        fun onRegisterPayloadHandler(event: RegisterPayloadHandlersEvent) {
                val registrar = event.registrar(PROTOCOL_VERSION)

                // Client -> Server packets
                registerClientToServer(registrar)

                // Server -> Client packets
                registerServerToClient(registrar)
        }

        private fun registerClientToServer(registrar: PayloadRegistrar) {
                registrar.playToServer(
                        RequestPCDataPacket.TYPE,
                        RequestPCDataPacket.STREAM_CODEC,
                        RequestPCDataPacket::handle
                )

                registrar.playToServer(
                        TransferToHomePacket.TYPE,
                        TransferToHomePacket.STREAM_CODEC,
                        TransferToHomePacket::handle
                )

                registrar.playToServer(
                        TransferToPCPacket.TYPE,
                        TransferToPCPacket.STREAM_CODEC,
                        TransferToPCPacket::handle
                )
        }

        private fun registerServerToClient(registrar: PayloadRegistrar) {
                registrar.playToClient(
                        SendPCDataPacket.TYPE,
                        SendPCDataPacket.STREAM_CODEC,
                        SendPCDataPacket::handle
                )

                registrar.playToClient(
                        TransferResultPacket.TYPE,
                        TransferResultPacket.STREAM_CODEC,
                        TransferResultPacket::handle
                )
        }
}
