package com.createenigma.client;

import com.createenigma.CreateEnigma;
import com.createenigma.registry.CEBlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * The Disguised Motor's summary.
 *
 * <p>It is behind Shift for the same reason the block exists: the disguise is supposed to hold until
 * someone looks closely. The item is indistinguishable from a real Creative Motor - same model, same
 * texture, same rarity colour - so the only thing that ever gives it away is a player who holds
 * Shift over it.
 *
 * <p>The hint is always there and the summary only appears once Shift is held, which is Create's
 * layout: the hint lives in all three of {@code ItemDescription}'s lists, the summary only in
 * {@code linesOnShift}. Nothing is shown before Shift but the hint - announcing more would
 * half-spoil the disguise this block exists to keep.
 */
@EventBusSubscriber(modid = CreateEnigma.MOD_ID, value = Dist.CLIENT)
public final class DisguisedMotorTooltip {

    private DisguisedMotorTooltip() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(CEBlocks.DISGUISED_MOTOR.get().asItem())) {
            return;
        }
        // Fired with a null player while the game builds its tooltip search trees.
        if (event.getEntity() == null) {
            return;
        }

        event.getToolTip().add(SummaryTooltips.holdShift());
        if (!SummaryTooltips.shiftHeld()) {
            return;
        }

        // One blank line directly under the hint, as on the other two items. Nothing is shown before
        // Shift but the hint itself - announcing more would half-spoil the disguise this block
        // exists to keep.
        event.getToolTip().add(Component.empty());
        event.getToolTip().add(Component.translatable("create_enigma.disguised_motor.summary")
                .withStyle(ChatFormatting.GOLD));
    }
}
