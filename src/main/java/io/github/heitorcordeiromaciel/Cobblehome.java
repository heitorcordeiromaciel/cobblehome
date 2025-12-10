package io.github.heitorcordeiromaciel;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod("cobblehome_neoforge")
public final class Cobblehome {

    public Cobblehome() {
        NeoForge.EVENT_BUS.register(Cobblehome.class);
    }

    @SubscribeEvent
    public static void onCommandRegistration(final RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("test")
            .executes(context -> {
                Species species = PokemonSpecies.INSTANCE.getByIdentifier(ResourceLocation.tryParse("cobblemon:bidoof"));
                context.getSource().sendSystemMessage(
                    Component.literal("Got species: ")
                        .withStyle(Style.EMPTY.withColor(0x03e3fc))
                        .append(species.getTranslatedName())
                );
                return 0;
            })
        );
    }

}
