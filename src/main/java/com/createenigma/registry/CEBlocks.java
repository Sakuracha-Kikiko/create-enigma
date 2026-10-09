package com.createenigma.registry;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

import com.createenigma.CreateEnigma;
import com.createenigma.content.DisguisedMotorBlock;
import com.createenigma.content.EnigmaCoreBlock;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.Rarity;
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

    /**
     * 伪装创造马达 / Disguised Creative Motor
     *
     * <p>Registered to look and behave like Create's Creative Motor, because the whole point is that
     * it cannot be told apart. That means borrowing from Create wherever Create owns the appearance:
     * the blockstate points at Create's models, the renderer and Flywheel visual are Create's, and
     * the item rarity is mirrored too (EPIC, which is what gives the real one its purple name).
     *
     * <p><b>The one thing not copied is the stress capacity transform.</b> Create's
     * {@code CStress.setCapacity} throws for blocks that are not Create's own, so the capacity is
     * supplied by the block entity instead - see {@code DisguisedMotorBlockEntity}.
     */
    public static final BlockEntry<DisguisedMotorBlock> DISGUISED_MOTOR =
            CreateEnigma.registrate().block("disguised_motor", DisguisedMotorBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(p -> p.mapColor(MapColor.COLOR_PURPLE)
                            .forceSolidOn())
                    .transform(pickaxeOnly())
                    .onRegister(BlockStressValues.setGeneratorSpeed(256, true))
                    .item()
                    .properties(p -> p.rarity(Rarity.EPIC))
                    .transform(customItemModel())
                    .register();

    public static void register() {
        // Class loading performs the registration.
    }
}
