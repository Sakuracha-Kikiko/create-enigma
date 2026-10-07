package com.createenigma.registry;

import com.createenigma.CreateEnigma;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Own creative mode tab.
 *
 * <p>A plain tab rather than Create's nested tab system: that layout is not part of Create's public
 * API, and this mod has a single item to show.
 */
public class CECreativeTabs {

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateEnigma.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("base",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.create_enigma.base"))
                    .icon(() -> CEBlocks.ENIGMA_CORE.asStack())
                    .displayItems((parameters, output) -> {
                        output.accept(CEBlocks.ENIGMA_CORE.asItem());
                        output.accept(CEItems.ENIGMA_WRENCH.asItem());
                    })
                    .build());

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
