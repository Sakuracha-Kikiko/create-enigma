package com.createenigma.registry;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;

import com.createenigma.CreateEnigma;
import com.createenigma.content.EnigmaCoreBlock;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.material.MapColor;

/**
 * The mod's blocks. There is exactly one.
 *
 * <p>The rest of the machine is built from vanilla and Create blocks on purpose: the whole point is
 * that this mod <em>recognises</em> an existing arrangement rather than adding 80 new blocks and
 * asking the player to use those instead.
 *
 * <p><b>No datagen is run.</b> The blockstate, the two block models and the item model are all
 * hand-written under {@code src/main/resources}, so {@code build/generated/resources} stays empty
 * and there is no way for a generated file to collide with a hand-written one. The
 * {@code customItemModel()} transform is still applied because it is what hands the item builder
 * back to the block builder; its data provider simply never executes.
 */
public class CEBlocks {

    // addLayer is deprecated in this Create build but is still the supported way to set a render
    // layer, and Create itself uses it the same way (see BuilderTransformers#encasedBase).

    /**
     * 谜之核心 / Enigma Core
     *
     * <p>Replaces the chest of the original machine. {@code assembled} drives both the model and
     * the light level, so the core visibly changes when the machine is complete.
     */
    @SuppressWarnings("removal")
    public static final BlockEntry<EnigmaCoreBlock> ENIGMA_CORE =
            CreateEnigma.registrate().block("enigma_core", EnigmaCoreBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(p -> p.noOcclusion()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .lightLevel(state -> state.getValue(EnigmaCoreBlock.ASSEMBLED) ? 10 : 0))
                    .addLayer(() -> RenderType::cutoutMipped)
                    .transform(axeOrPickaxe())
                    .item()
                    .transform(customItemModel())
                    .register();

    public static void register() {
        // Class loading performs the registration.
    }
}
