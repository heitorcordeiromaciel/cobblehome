package io.github.heitorcordeiromaciel.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import io.github.heitorcordeiromaciel.network.packets.OpenUIPacket
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.PacketDistributor

/** Registers and handles the /cobblehome command */
object CobbleHomeCommand {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(Commands.literal("vault").executes { context -> execute(context) })
    }

    private fun execute(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source

        // Ensure this is a player
        val player = source.player
        if (player !is ServerPlayer) {
            source.sendFailure(Component.literal("This command can only be used by players"))
            return 0
        }

        // Send packet to client to open UI
        PacketDistributor.sendToPlayer(player, OpenUIPacket())

        return 1
    }
}
