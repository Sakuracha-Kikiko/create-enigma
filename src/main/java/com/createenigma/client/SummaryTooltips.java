package com.createenigma.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The "hold Shift for Summary" line, shared by everything in this mod that has a summary.
 *
 * <p>Byte for byte what Create's {@code TooltipHelper.holdShift} produces, rebuilt from Create's two
 * language keys so this mod does not need Create's palette enum to say four words. Extracted the
 * moment there was a second caller - two copies of a line whose entire job is to look exactly like
 * Create's would drift apart silently.
 */
public final class SummaryTooltips {

    private SummaryTooltips() {}

    public static Component holdShift() {
        return Component.translatable("create.tooltip.holdForDescription",
                        Component.translatable("create.tooltip.keyShift").withStyle(ChatFormatting.GRAY))
                .withStyle(ChatFormatting.DARK_GRAY);
    }

    /**
     * Whether the player is asking for the longer version.
     *
     * <p>{@code Screen.hasShiftDown} rather than an input event: it is what Create's own tooltips
     * use, and it stays correct while a screen is open, which a key-state flag maintained by events
     * would not.
     */
    public static boolean shiftHeld() {
        return Screen.hasShiftDown();
    }
}
