package com.createenigma.client;

import net.createmod.catnip.lang.FontHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

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
        // Guarded so the class can be referenced from a dedicated server: Screen is a client class,
        // and only the branch that runs on a client ever resolves it.
        return net.neoforged.fml.loading.FMLEnvironment.dist.isClient() && Screen.hasShiftDown();
    }

    /**
     * One line of summary body, in Create's own colours: words wrapped in underscores are picked out
     * in the palette's highlight colour, everything else in its primary colour.
     *
     * <p>{@code TooltipHelper.cutTextComponent} applies exactly this rule, after wrapping the text to
     * a fixed pixel width. The wrapping is left to the game here, which already wraps tooltips at the
     * window edge, so only the colouring is reproduced.
     *
     * <p>The palette is not a matter of taste: Create's own tooltips all come from one
     * {@code ItemDescription.Modifier} registered with {@code Palette.STANDARD_CREATE}, so using
     * anything else here - gray, white, gold - makes our lines disagree with the mod they sit next
     * to. It is passed in rather than hardcoded so a deliberate exception stays possible.
     */
    public static Component summary(Component text) {
        return summary(text, FontHelper.Palette.STANDARD_CREATE);
    }

    public static Component summary(Component text, FontHelper.Palette palette) {
        MutableComponent line = Component.empty();
        String[] parts = text.getString().split("_", -1);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty())
                continue;
            line.append(Component.literal(parts[i])
                    .setStyle(i % 2 == 0 ? palette.primary() : palette.highlight()));
        }
        return line;
    }
}
