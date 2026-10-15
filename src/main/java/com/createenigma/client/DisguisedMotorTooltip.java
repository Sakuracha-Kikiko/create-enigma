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
 * <p>Nothing is shown while Shift is up, which is what Create's own summaries do: the text lives in
 * {@code ItemDescription.linesOnShift}, so an unmodified Create item shows its name and nothing
 * else. An earlier version added a "Hold [Shift] for Summary" hint, copied from
 * {@code TooltipHelper.holdShift} - a method Create still ships but no longer calls. It gave every
 * one of these items a line that no Create item has, and on this one it also half-spoiled the
 * disguise by announcing that there was something to see.
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
        if (!SummaryTooltips.shiftHeld()) {
            return;
        }

        event.getToolTip().add(Component.translatable("create_enigma.disguised_motor.summary")
                .withStyle(ChatFormatting.GOLD));
    }
}
