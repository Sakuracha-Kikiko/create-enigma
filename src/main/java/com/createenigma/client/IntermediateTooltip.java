package com.createenigma.client;

import com.createenigma.CreateEnigma;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Says which step an incomplete precision mechanism is at.
 *
 * <h2>Why this has to exist</h2>
 *
 * <p>The wrench asks for three mechanisms at three exact steps. Without this line a player has no
 * way to learn a mechanism's step at all: Create draws a durability-style bar whose width is
 * {@code round(progress * 13)} pixels of thirteen, which cannot be read to the pixel and carries no
 * number. The task "bring three mechanisms at these steps" is then not a puzzle but a guess, and a
 * guess that lands one step off fails exactly like a guess that lands twelve off.
 *
 * <p>So the number here is not a convenience. It is the feedback loop the recipe is built on, and
 * its formula is the recipe's formula to the character - see {@code EnigmaWrenchRecipe.progressAt}.
 * If these two ever disagree, every correct attempt fails and nothing in the game says why, which is
 * the failure this class exists to end.
 */
@EventBusSubscriber(modid = CreateEnigma.MOD_ID, value = Dist.CLIENT)
public final class IntermediateTooltip {

    private IntermediateTooltip() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(AllItems.INCOMPLETE_PRECISION_MECHANISM.get())) {
            return;
        }
        // Not defensive noise: the bar's value lives on the SequencedAssemblyItem subclass, and
        // item registration is the only thing that guarantees this stack is one.
        if (!(stack.getItem() instanceof SequencedAssemblyItem item)) {
            return;
        }

        // Create stores the progress as a float and draws round(progress * 13), which runs 0 to 13
        // because the first assembly step produces the 1/15 intermediate and the fifteenth resolves
        // it into a finished mechanism. The number a player counts in steps is one higher.
        int step = Math.round(item.getProgress(stack) * 13) + 1;
        event.getToolTip().add(Component.translatable("create_enigma.mechanism.step", step)
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
