package com.createenigma.content;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

/**
 * 熄灭蛋糕 / Extinguished Cake: used on a lit blaze burner, it puts the burner out.
 *
 * <p>A burner that is heating is in {@code KINDLED} or {@code SEETHING}; this turns one of those
 * into {@code SMOULDERING}, which is a burner whose blaze is present but idle. It refuses a
 * creative burner, because that one never burns fuel in the first place and putting it out would
 * only break whatever it is driving.
 *
 * <p>Changing the block state alone does not stick: {@code BlazeBurnerBlockEntity.tick} derives the
 * state from its fuel every tick ({@code if (activeFuel == FuelType.NORMAL) updateBlockState();}),
 * so a burner left with fuel would come straight back up. Both fuel fields have to go to zero as
 * well.
 *
 * <p>Those two fields are {@code protected} and there is no setter, so they are cleared the way the
 * game itself would clear them: the block entity's own NBT is saved, the two keys are zeroed, and it
 * is loaded back. {@code read} is the code that assigns them, so this cannot drift from it - and the
 * tag is the complete one, so the goggles and train hat a burner may be wearing survive.
 */
public class ExtinguishedCakeItem extends Item {

    /** The NBT keys {@code BlazeBurnerBlockEntity.read} fills from, in its own spelling. */
    private static final String FUEL_LEVEL = "fuelLevel";
    private static final String BURN_TIME_REMAINING = "burnTimeRemaining";

    private static final String TOOLTIP_STORY = "item.create_enigma.extinguished_cake.tooltip.story";
    private static final String TOOLTIP_HINT = "item.create_enigma.extinguished_cake.tooltip.hint";

    /**
     * Eaten, it restores 5 hunger and 12 saturation (nutrition 5, saturation modifier 1.2, and the
     * game doubles that: 5 * 1.2 * 2 = 12) and grants ten seconds of fire resistance.
     *
     * <p>For scale, a golden carrot is 6 and 14.4 - so this sits just under the best food in the
     * game, which is what a cake baked in a blaze burner had better be. Fire resistance is the
     * point of it: the cake had its fire put out, and eating it lends you the same.
     */
    public static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(5)
            .saturationModifier(1.2F)
            .effect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0), 1.0F)
            .build();

    public ExtinguishedCakeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof BlazeBurnerBlockEntity burner))
            return InteractionResult.PASS;
        if (burner.isCreative() || !burner.getHeatLevelFromBlock().isAtLeast(HeatLevel.KINDLED))
            return InteractionResult.PASS;

        if (!level.isClientSide) {
            extinguish(level, pos, burner);
            Player player = context.getPlayer();
            if (player == null || !player.getAbilities().instabuild)
                context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        if (!shiftDown())
            return;
        tooltip.add(Component.translatable(TOOLTIP_STORY).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(TOOLTIP_HINT).withStyle(ChatFormatting.GRAY));
    }

    /** Shift, asked of the client only: an item is never hovered on a dedicated server. */
    private static boolean shiftDown() {
        return FMLEnvironment.dist.isClient()
                && net.minecraft.client.gui.screens.Screen.hasShiftDown();
    }

    /**
     * Puts the given burner out and reports whether it did anything.
     *
     * <p>Separate from {@link #useOn} so a test can drive it without a player holding the cake.
     */
    public static boolean extinguish(Level level, BlockPos pos, BlazeBurnerBlockEntity burner) {
        if (burner.isCreative() || !burner.getHeatLevelFromBlock().isAtLeast(HeatLevel.KINDLED))
            return false;
        if (level.isClientSide)
            return false;

        CompoundTag data = burner.saveWithoutMetadata(level.registryAccess());
        data.putInt(FUEL_LEVEL, BlazeBurnerBlockEntity.FuelType.NONE.ordinal());
        data.putInt(BURN_TIME_REMAINING, 0);
        burner.loadWithComponents(data, level.registryAccess());

        // Fuel is gone, so this recomputes SMOULDERING from it; it also notifies the client.
        burner.updateBlockState();
        burner.setChanged();

        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.2F);
        if (level instanceof ServerLevel server) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;
            server.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 24, 0.3, 0.3, 0.3, 0.02);
        }
        return true;
    }

    /** The heat level a burner shows, for tests and for the message that refuses a creative one. */
    public static HeatLevel heatOf(BlazeBurnerBlockEntity burner) {
        return BlazeBurnerBlock.getHeatLevelOf(burner.getBlockState());
    }
}
