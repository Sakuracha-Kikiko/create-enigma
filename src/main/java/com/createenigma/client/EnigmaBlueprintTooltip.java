package com.createenigma.client;

import java.util.List;

import com.createenigma.CreateEnigma;
import com.createenigma.structure.EnigmaSchematic;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Replaces the tooltip of this mod's blueprint with its own.
 *
 * <h2>What is being replaced</h2>
 *
 * <p>Create gives a blueprint two lines that describe a blueprint in general, and this is not one:
 * the schematic's file name ({@code enigma/first_generation.nbt}, in gold) from
 * {@code SchematicItem.appendHoverText}, and a description built from the
 * {@code item.create.schematic.tooltip.*} language keys by {@code ClientEvents.addToItemTooltip}.
 * The first is an implementation detail of how a blueprint finds its structure; the second tells the
 * player how to position a hologram, which they will work out the moment they hold it.
 *
 * <h2>Why the list is rebuilt rather than filtered</h2>
 *
 * <p>Both of Create's lines arrive through {@code ItemTooltipEvent} as well, so which handler runs
 * first is decided by registration order - and that follows mod loading order, which is not
 * something to depend on. {@link EventPriority#LOWEST} settles it: this runs after Create's, so
 * whatever is in the list by then is everything the item would otherwise have shown.
 *
 * <p>That also makes the removal language-independent. Filtering by matching the file name would
 * miss the description, and matching the description's text would work in English and quietly stop
 * working in Chinese - a failure nobody would notice until a player reported it in a language the
 * author does not read.
 *
 * <p>So the name stays and everything appended after it goes. If a future Create adds a line here
 * worth keeping, it will be lost, and that is a trade made knowingly: a wrong description is worse
 * than a missing one.
 */
@EventBusSubscriber(modid = CreateEnigma.MOD_ID, value = Dist.CLIENT)
public final class EnigmaBlueprintTooltip {

    private EnigmaBlueprintTooltip() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(AllItems.SCHEMATIC.get())) {
            return;
        }
        if (!EnigmaSchematic.RELATIVE_PATH.equals(stack.get(AllDataComponents.SCHEMATIC_FILE))) {
            return;
        }

        List<Component> tooltip = event.getToolTip();

        // Line 0 is the item's name, which is already ours - set as a component on the stack, so it
        // is also right in an item frame and in chat. Everything after it belongs to Create.
        if (tooltip.size() > 1) {
            tooltip.subList(1, tooltip.size()).clear();
        }

        // Nothing but the hint while Shift is up, then the summary. That is Create's own split:
        // ItemDescription.Builder.build adds the hint to all three of its lists and the summary only
        // to linesOnShift.
        tooltip.add(SummaryTooltips.holdShift());
        if (!SummaryTooltips.shiftHeld()) {
            return;
        }

        tooltip.add(Component.translatable("create_enigma.blueprint.summary")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("create_enigma.blueprint.detail")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
