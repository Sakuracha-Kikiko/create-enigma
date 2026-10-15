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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Rewrites the tooltip of this mod's blueprint.
 *
 * <p>Create's own tooltip prints the schematic's file name - {@code enigma/first_generation.nbt},
 * in gold, as the second line. That is honest and useless: it is an implementation detail of how a
 * blueprint finds its structure, and a player reading it learns only that something called a .nbt
 * exists somewhere.
 *
 * <p>So the line is removed and replaced. Nothing is hidden that a player needs: the file name is
 * not the item's identity, it is where the item's data happens to live.
 *
 * <p>The name comes from the {@code minecraft:item_name} component set on the stack rather than
 * from here, so it reads correctly everywhere - an item frame, a chat message, a hopper - and not
 * only in the inventory tooltip.
 */
@EventBusSubscriber(modid = CreateEnigma.MOD_ID, value = Dist.CLIENT)
public final class EnigmaBlueprintTooltip {

    private EnigmaBlueprintTooltip() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(AllItems.SCHEMATIC.get())) {
            return;
        }
        if (!EnigmaSchematic.RELATIVE_PATH.equals(stack.get(AllDataComponents.SCHEMATIC_FILE))) {
            return;
        }

        List<Component> tooltip = event.getToolTip();

        // Create adds this one itself, and there is no flag to turn it off - so it is removed by
        // matching what it contains rather than by position, which would break the moment Create
        // adds another line above it.
        tooltip.removeIf(line -> line.getString().contains(EnigmaSchematic.RELATIVE_PATH));

        tooltip.add(Component.translatable("create_enigma.blueprint.summary")
                .withStyle(ChatFormatting.GRAY));

        if (SummaryTooltips.shiftHeld()) {
            tooltip.add(Component.translatable("create_enigma.blueprint.detail")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(SummaryTooltips.holdShift());
        }
    }
}
