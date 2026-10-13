package com.createenigma.registry;

import com.createenigma.CreateEnigma;
import com.createenigma.structure.EnigmaSchematic;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
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
                        output.accept(CEBlocks.DISGUISED_MOTOR.asItem());
                        output.accept(CEItems.ENIGMA_WRENCH.asItem());
                        output.accept(blueprint());
                    })
                    .build());

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }

    /**
     * Create's own blueprint item, pointed at this mod's schematic.
     *
     * <p>Nothing new is registered: a blueprint is a name plus an owner, and both are data
     * components on the item Create already ships. Everything downstream - the cannon, the table,
     * rotation, mirroring, the ghost preview - works unchanged.
     *
     * <p>TODO: this is the testing route. The intended way to get one is to find it, which is a
     * loot table entry rather than a creative tab entry. Kept for now because a blueprint that can
     * only be found is a blueprint that cannot be tested.
     */
    private static ItemStack blueprint() {
        ItemStack stack = new ItemStack(AllItems.SCHEMATIC.get());
        stack.set(AllDataComponents.SCHEMATIC_FILE, EnigmaSchematic.RELATIVE_PATH);
        stack.set(AllDataComponents.SCHEMATIC_OWNER, EnigmaSchematic.OWNER);
        return stack;
    }
}
