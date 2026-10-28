package com.createenigma.registry;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

import com.createenigma.CreateEnigma;
import com.createenigma.content.CreativeHandCrankBlock;
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
     *
     * <p><b>It is not a survival item.</b> The core cannot be obtained as itself: broken, it drops
     * a chest, and the wrench dismantles it back into a chest. The block exists in the registry
     * because blueprints and this mod's own anchor state need it, so it is a creative-only block in
     * effect - which is also why it needs no tool tier of its own to be mined.
     *
     * <p>Its numbers are the owner's, not borrowed from a stone: hardness 3 and blast resistance 9
     * put it roughly at a chest's toughness, and {@code requiresCorrectToolForDrops} is left off on
     * purpose so that any hand or tool takes it. A block that only ever gives a chest back has no
     * reason to make the player carry a pickaxe.
     */
    @SuppressWarnings("removal")
    public static final BlockEntry<EnigmaCoreBlock> ENIGMA_CORE =
            CreateEnigma.registrate().block("enigma_core", EnigmaCoreBlock::new)
                    .properties(p -> p.mapColor(MapColor.COLOR_PURPLE)
                            .strength(3.0F, 9.0F)
                            .noOcclusion()
                            .lightLevel(state -> state.getValue(EnigmaCoreBlock.ASSEMBLED) ? 10 : 0))
                    .addLayer(() -> RenderType::cutoutMipped)
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

    /**
     * 创造手摇曲柄 / Creative Hand Crank
     *
     * <p>Create's hand crank, with the stress value changed from 8 to 16384 per RPM.
     *
     * <p><b>The whole difference is one registration.</b> {@code BlockStressValues.CAPACITIES} is a
     * public registry keyed by block, so a different block is a different key and needs no subclass
     * to override anything. {@link BlockStressValues#getCapacity} then answers 16384 for this block
     * where Create's config answers 8 for theirs, and the crank's generated capacity follows
     * {@code speed * value} exactly as Create's does - 16384 per RPM at 32 RPM.
     *
     * <p>The alternative, overriding {@code calculateAddedStressCapacity} on the block entity, is
     * what {@code DisguisedMotorBlockEntity} does - but only because that block needed a <em>fixed</em>
     * capacity rather than a per-RPM one. Here the per-RPM scaling is the behaviour being kept, so the
     * registry is the right lever.
     *
     * <p>The appearance is Create's on purpose: the blockstate and models point at Create's crank
     * models, and the arm is drawn by Create's own visual. Recolouring it would mean copying that
     * visual just to change which model it names - a separate decision, not a palette swap.
     */
    public static final BlockEntry<CreativeHandCrankBlock> CREATIVE_HAND_CRANK =
            CreateEnigma.registrate().block("creative_hand_crank", CreativeHandCrankBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(p -> p.mapColor(MapColor.COLOR_PURPLE))
                    .transform(pickaxeOnly())
                    .onRegister(block -> BlockStressValues.CAPACITIES.register(block, () -> 16384.0))
                    .item()
                    .properties(p -> p.rarity(Rarity.EPIC))
                    .transform(customItemModel())
                    .register();

    public static void register() {
        // Class loading performs the registration.
    }
}
