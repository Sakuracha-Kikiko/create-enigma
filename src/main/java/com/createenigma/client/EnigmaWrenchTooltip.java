package com.createenigma.client;

import java.util.List;

import com.createenigma.CreateEnigma;
import com.createenigma.registry.CEItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * The Enigma Wrench's summary, shown while Shift is held.
 *
 * <p><b>Why this is not Create's {@code ItemDescription}.</b> Create's summary system reads its
 * text from language keys at registration time and hands back a fixed set of components, so the
 * wording is settled before the game even reaches a world. This tooltip needs to be built at the
 * moment it is drawn - first so the obfuscated run can be styled exactly, and more importantly so
 * that it can later depend on something the player has or has not done. Starting from Create's
 * system would have to be undone to get there.
 *
 * <p><b>The hint is always shown; the summary only while Shift is held.</b> That is Create's
 * layout - {@code ItemDescription.Builder.build} puts the hint into all three of its lists and the
 * summary only into {@code linesOnShift} - and it is the difference between an item that explains
 * itself and one that says nothing until the player guesses a key.
 *
 * <p>Client only: tooltips are drawn on the client, and the Shift check is a client input state.
 */
@EventBusSubscriber(modid = CreateEnigma.MOD_ID, value = Dist.CLIENT)
public final class EnigmaWrenchTooltip {

    private EnigmaWrenchTooltip() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(CEItems.ENIGMA_WRENCH.get())) {
            return;
        }
        // This event also fires with a null player while the game builds its tooltip search
        // trees, so the guard is real, not defensive noise.
        if (event.getEntity() == null) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        tooltip.add(SummaryTooltips.holdShift());
        if (!SummaryTooltips.shiftHeld()) {
            return;
        }

        tooltip.add(summary());
        tooltip.add(aside());
    }

    /** "用于调试[乱码]的多功能工具…？" - the subject is the only part that is obscured. */
    private static Component summary() {
        return Component.translatable("create_enigma.enigma_wrench.summary",
                        Component.translatable("create_enigma.enigma_wrench.subject")
                                .withStyle(ChatFormatting.OBFUSCATED))
                .withStyle(ChatFormatting.GOLD);
    }

    /** The muttered second line: darker than the first, and struck through. */
    private static Component aside() {
        return Component.translatable("create_enigma.enigma_wrench.aside")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH);
    }
}
