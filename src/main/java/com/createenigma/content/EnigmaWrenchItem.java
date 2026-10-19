package com.createenigma.content;

import com.createenigma.structure.EnigmaStructure;
import com.createenigma.structure.EnigmaStructure.Generation;
import com.createenigma.structure.EnigmaValidator;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * 神秘扳手 / Enigma Wrench
 *
 * <p>Right-click a chest and it reads the machine being built around it. This is the mod's only
 * teacher: the ponder shows the finished machine, made of Creative Motors that cannot be obtained
 * in survival, so the wrench is where a player finds out what they are actually meant to do.
 *
 * <p><b>The chest still opens normally with anything else in hand.</b> No cancellation is needed
 * for that: a chest answers {@code useWithoutItem}, not {@code useItemOn}, so with a wrench held
 * the block declines the interaction and this runs. Holding any other item - or nothing - opens it
 * as usual.
 *
 * <p><b>Reading is server side.</b> The validation walks 82 block positions across up to four
 * orientations and may replace a block, so it happens once, on the server, and the answer is sent
 * to the player as a chat message. The client still consumes the click so the chest does not open.
 */
public class EnigmaWrenchItem extends Item {

    public EnigmaWrenchItem(Properties properties) {
        super(properties);
    }

    /**
     * Runs <b>before</b> the block gets its turn, which is the whole reason the chest stays shut.
     *
     * <p>{@code ServerPlayerGameMode.useItemOn} asks the item first
     * ({@code stack.onItemUseFirst}) and only falls through to {@code blockstate.useItemOn} if that
     * returns {@code PASS}. Overriding {@code useOn} instead - which runs <em>after</em> the block -
     * meant the chest opened first and this never had a chance to say anything.
     *
     * <p>Returning anything other than {@code PASS} consumes the click, so no cancellation,
     * event handler or block override is needed: a player holding the wrench simply cannot open a
     * chest, and a player holding anything else opens it as usual.
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (!level.getBlockState(pos).is(Blocks.CHEST)) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        EnigmaStructure structure = EnigmaStructure.get();
        if (structure == null) {
            return InteractionResult.PASS;
        }

        EnigmaValidator.Report report =
                EnigmaValidator.analyse(level, pos, structure, Generation.PRIMAL);

        if (report.isComplete()) {
            Container chest = offeringChest(level, pos);
            EnigmaOfferings.Verdict verdict =
                    chest == null ? EnigmaOfferings.Verdict.EMPTY : EnigmaOfferings.judge(chest);

            switch (verdict) {
                case ACCEPTABLE -> {
                    EnigmaOfferings.consume(chest);
                    promote(level, pos, player);
                }
                case EMPTY -> refuse(level, pos, player,
                        "create_enigma.wrench.offerings_empty", 1.4F);
                case IMPURE -> refuse(level, pos, player,
                        "create_enigma.wrench.offerings_impure", 0.6F);
            }
        } else {
            player.displayClientMessage(readout(report, player.isShiftKeyDown()), false);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5F, 1.4F);
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * The chest as one container, including both halves of a double chest.
     *
     * <p>{@code override = true} is not optional here. With {@code false} the combiner refuses when a
     * solid block sits above the chest - which {@code isChestBlockedAt} is meant to test - and the
     * machine is built around this chest, so there is very often something above it. The failure is
     * silent: the container comes back {@code null} and every offering would read as "empty" no
     * matter what the player put in.
     *
     * <p>Reaching the combined view also matters for a double chest. A single {@code ChestBlockEntity}
     * exposes only its own twenty-seven slots, so an offering in the far half would be invisible.
     */
    @Nullable
    private static Container offeringChest(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock chest)) {
            return null;
        }
        return ChestBlock.getContainer(chest, state, level, pos, true);
    }

    /** The machine is complete but the offering is not acceptable. Nothing changes in the world. */
    private static void refuse(Level level, BlockPos pos, Player player, String key, float pitch) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), false);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5F, pitch);
    }

    /**
     * What the wrench says.
     *
     * <p>Order matters here, and one case has to come first: "everything is present except the
     * motors" is also "more than sixty blocks", so the general tiers would swallow it. That case is
     * the whole point of the wrench - it is the moment a player has done everything they can and is
     * still stuck - so it is asked first.
     *
     * <p>Holding shift replaces the atmosphere with the exact position and the exact problem. The
     * vague tiers are the experience; this is the thing that stops someone being stuck at 79 of 82
     * with no way to find out which three.
     */
    private static Component readout(EnigmaValidator.Report report, boolean precise) {
        if (precise && report.firstGap() != null) {
            BlockPos gap = report.firstGap();
            return Component.translatable("create_enigma.enigma_core.unformed",
                            gap.getX(), gap.getY(), gap.getZ(), report.detail())
                    .withStyle(ChatFormatting.GRAY);
        }

        String key;
        if (report.onlyMotorsMissing()) {
            key = "create_enigma.wrench.not_picky";
        } else if (report.placed() >= 60) {
            key = "create_enigma.wrench.gathering";
        } else if (report.placed() >= 21) {
            key = "create_enigma.wrench.hungry";
        } else if (report.placed() >= 6) {
            key = "create_enigma.wrench.chest_question";
        } else {
            key = "create_enigma.wrench.chest";
        }

        return Component.translatable(key).withStyle(ChatFormatting.GRAY);
    }

    /**
     * Swaps the chest for the core, which is the moment the machine stops being a pile of blocks.
     *
     * <p>By the time this runs the chest holds nothing but Hands of God, and those have already been
     * taken, so the drop below usually moves nothing. It is kept because the contents are still the
     * player's: if some path ever reaches here with a chest that has other things in it, losing them
     * silently would be the kind of loss with no explanation and no undo.
     *
     * <p>Nothing else moves: the core takes the chest's position, which is also the anchor position,
     * so every offset and every later validation still refers to the same place.
     *
     * <p>What happens next is deliberate. The complete machine records all seven motors, and the
     * first generation has six - so the newly placed core immediately reports itself incomplete and
     * names the position of the motor the ponder never showed. The machine delivers the reveal
     * itself, in the same voice it has been using all along.
     */
    private static void promote(Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            Containers.dropContents(level, pos, chest);
        }

        level.setBlockAndUpdate(pos, EnigmaStructure.anchorState(Generation.COMPLETE));
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6F, 1.2F);
        player.displayClientMessage(
                Component.translatable("create_enigma.wrench.promoted").withStyle(ChatFormatting.GOLD),
                false);
    }
}
