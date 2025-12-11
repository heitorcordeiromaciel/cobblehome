package io.github.heitorcordeiromaciel.ui

import java.util.function.Supplier
import net.minecraft.core.registries.Registries
import net.minecraft.world.inventory.MenuType
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

/** Registers menu types and screen factories for CobbleHome UI */
object MenuRegistry {

    val MENU_TYPES: DeferredRegister<MenuType<*>> =
            DeferredRegister.create(Registries.MENU, "cobblehome_neoforge")

    val COBBLEHOME_MENU_TYPE: DeferredHolder<MenuType<*>, MenuType<CobbleHomeMenu>> =
            MENU_TYPES.register(
                    "cobblehome",
                    Supplier {
                        IMenuTypeExtension.create { containerId, playerInventory, _ ->
                            CobbleHomeMenu(
                                    containerId,
                                    playerInventory,
                                    playerInventory.player as
                                            net.minecraft.server.level.ServerPlayer
                            )
                        }
                    }
            )

    @EventBusSubscriber(
            modid = "cobblehome_neoforge",
            bus = EventBusSubscriber.Bus.MOD,
            value = [Dist.CLIENT]
    )
    object ClientRegistration {
        @SubscribeEvent
        fun onRegisterMenuScreens(event: RegisterMenuScreensEvent) {
            event.register(COBBLEHOME_MENU_TYPE.get()) { menu, inventory, title ->
                CobbleHomeScreen(menu, inventory, title)
            }
        }
    }
}
