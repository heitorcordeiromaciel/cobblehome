package io.github.heitorcordeiromaciel.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import io.github.heitorcordeiromaciel.ui.CobbleHomeMenu
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.SimpleMenuProvider

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

        // Open the CobbleHome UI
        // The client will send RequestPCDataPacket when the UI opens
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
