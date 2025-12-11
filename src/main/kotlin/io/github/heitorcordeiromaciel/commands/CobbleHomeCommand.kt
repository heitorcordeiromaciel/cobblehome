package io.github.heitorcordeiromaciel.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import io.github.heitorcordeiromaciel.network.packets.RequestPCDataPacket
import io.github.heitorcordeiromaciel.ui.CobbleHomeMenu
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.SimpleMenuProvider
import net.neoforged.neoforge.network.PacketDistributor

/** Registers and handles the /cobblehome command */
object CobbleHomeCommand {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(Commands.literal("cobblehome").executes { context -> execute(context) })
    }

    private fun execute(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source

        // Ensure this is a player
        val player = source.player
        if (player !is ServerPlayer) {
            source.sendFailure(Component.literal("This command can only be used by players"))
            return 0
        }

        // Send request for PC data to client
        // The client will receive this and cache the PC data
        PacketDistributor.sendToPlayer(player, RequestPCDataPacket())

        // Open the CobbleHome UI
        // Note: In a full implementation, we'd wait for the client to confirm
        // it has the data before opening, but for simplicity we open immediately
        openCobbleHomeUI(player)

        return 1
    }

    private fun openCobbleHomeUI(player: ServerPlayer) {
        player.openMenu(
                SimpleMenuProvider(
                        { containerId, playerInventory, _ ->
                            return@SimpleMenuProvider CobbleHomeMenu(
                                    containerId,
                                    playerInventory,
                                    player
                            )
                        },
                        Component.literal("CobbleHome")
                )
        )
    }
}
