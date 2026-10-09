package com.createenigma.client;

import java.util.List;

import com.createenigma.CreateEnigma;
import com.createenigma.registry.CEBlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * The Disguised Motor's summary.
 *
 * <p>It has to be behind Shift for the same reason the block exists: the disguise is supposed to
 * hold until someone looks closely. The item is indistinguishable from a real Creative Motor -
 * same model, same texture, same rarity colour - so the only thing that ever gives it away is a
 * player who holds Shift over it.
 *
 * <p>Layout follows Create's summaries: the hint is always the last line, and holding Shift only
 * appends below it, so nothing reflows.
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

        List<Component> tooltip = event.getToolTip();
        tooltip.add(SummaryTooltips.holdShift());
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("create_enigma.disguised_motor.summary")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
