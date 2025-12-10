package io.github.heitorcordeiromaciel

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent

@Mod("cobblehome_neoforge")
class Cobblehome {

    init {
        NeoForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun onCommandRegistration(event: RegisterCommandsEvent) {
        event.dispatcher.register(
                Commands.literal("test").executes { context ->
                    val speciesId = ResourceLocation.tryParse("cobblemon:bidoof")

                    if (speciesId != null) {
                        val species = PokemonSpecies.getByIdentifier(speciesId)

                        context.source.sendSystemMessage(
                                Component.literal("Got species: ")
                                        .withStyle(Style.EMPTY.withColor(0x03e3fc))
                                        .append(species?.translatedName)
                        )
                    }

                    0
                }
        )
    }
}
