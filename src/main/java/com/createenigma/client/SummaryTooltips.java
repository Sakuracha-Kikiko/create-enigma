package com.createenigma.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Shared Shift state and key hint for this mod's item summaries.
 *
 * <h2>The hint is not optional</h2>
 *
 * <p>It was removed once, on the reading that Create shows nothing until Shift is held. That reading
 * came from {@code ItemDescription.linesOnShift} holding the summary - true, and half the picture.
 * {@code getCurrentLines} picks between three lists, and {@code Builder.build} adds the hint to
 * <b>all three</b>:
 *
 * <pre>
 * lines        - the hint, Shift key in gray       (Shift not held)
 * linesOnShift - the hint, Shift key in white, then the summary and behaviours
 * linesOnCtrl  - the hint, Ctrl key in white, then the actions
 * </pre>
 *
 * <p>So the hint is on screen in every state, and what changes is which key is lit. Removing it left
 * the items saying nothing at all until the player guessed a key they had no way to know about.
 *
 * <h2>What actually differs from Create</h2>
 *
 * <p>Only where the summary goes: here it appears when Shift is held and is absent otherwise, which
 * is what {@code lines} and {@code linesOnShift} being separate lists means. Showing it in both
 * states - which an earlier version did - put a line under the hint that Create never has there.
 */
public final class SummaryTooltips {

    private SummaryTooltips() {}

    /**
     * The "Hold [Shift] for Summary" line, matching Create's own output.
     *
     * <p>The key is gray while Shift is up and white while it is held - that colour change is the
     * only feedback Create gives that the key registered, and it is the reason this is built per
     * call rather than cached.
     */
    public static Component holdShift() {
        return Component.translatable("create.tooltip.holdForDescription",
                        Component.translatable("create.tooltip.keyShift")
                                .withStyle(shiftHeld() ? ChatFormatting.WHITE : ChatFormatting.GRAY))
                .withStyle(ChatFormatting.DARK_GRAY);
    }

    /**
     * Whether the player is asking for the longer version.
     *
     * <p>{@code Screen.hasShiftDown} rather than an input event: it is what Create's own
     * {@code getCurrentLines} uses, and it stays correct while a screen is open, which a key-state
     * flag maintained by events would not.
     */
    public static boolean shiftHeld() {
        return Screen.hasShiftDown();
    }
}
