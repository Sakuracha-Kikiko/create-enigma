package com.createenigma;

import com.createenigma.registry.CEBlockEntities;
import com.createenigma.registry.CEBlocks;
import com.createenigma.registry.CECreativeTabs;
import com.simibubi.create.foundation.data.CreateRegistrate;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Create: Enigma
 *
 * <p>Registers the machine from Mojang's 2022 Java Edition trailer as a real multiblock structure.
 * The layout is the one Create recreated in its Creative Motor ponder scene as "Mojang's Enigma"
 * (see {@code assets/create/ponder/creative_motor_mojang.nbt}); the ponder scene itself is Create's
 * asset, the block layout is not, and this mod only ships its own cleaned-up structure template.
 */
@Mod(CreateEnigma.MOD_ID)
public class CreateEnigma {

    public static final String MOD_ID = "create_enigma";
    public static final String NAME = "Create: Enigma";

    /**
     * Own Registrate instance. Create explicitly refuses to let other mods register through its own
     * instance ({@code Create.registrate()} throws), so an addon builds its own - the same way
     * Create: Connected does.
     */
    private static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID)
            .defaultCreativeTab((ResourceKey<CreativeModeTab>) null);

    public static CreateRegistrate registrate() {
        return REGISTRATE;
    }

    public CreateEnigma(IEventBus modBus) {
        REGISTRATE.registerEventListeners(modBus);

        CEBlocks.register();
        CEBlockEntities.register();
        CECreativeTabs.register(modBus);
    }
}
