package com.createenigma.client;

import net.minecraft.client.gui.screens.Screen;

/**
 * Shared Shift state for this mod's item summaries.
 *
 * <p><b>There is deliberately no "hold Shift" hint here.</b> There used to be, rebuilt from Create's
 * own language keys, on the theory that matching Create's wording would match Create. It does not:
 * {@code TooltipHelper.holdShift} still exists but nothing calls it any more - the call in
 * {@code BlueprintScreen} is commented out - and Create's summaries live in
 * {@code ItemDescription.linesOnShift}, which means an unmodified Create item shows its name and
 * nothing else until Shift is held. The hint gave every item in this mod a line no Create item has.
 */
public final class SummaryTooltips {

    private SummaryTooltips() {}

    /**
     * Whether the player is asking for the longer version.
     *
     * <p>{@code Screen.hasShiftDown} rather than an input event: it is what Create's own summaries
     * use, and it stays correct while a screen is open, which a key-state flag maintained by events
     * would not.
     */
    public static boolean shiftHeld() {
        return Screen.hasShiftDown();
    }
}
