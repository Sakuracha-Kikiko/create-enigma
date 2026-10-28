package com.createenigma.content;

import com.createenigma.CreateEnigma;
import com.createenigma.registry.CEItems;
import com.createenigma.structure.EnigmaSchematic;
import com.simibubi.create.AllItems;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 神秘扳手把空白蓝图写成神秘蓝图 - the wrench, right-clicked on a dropped empty blueprint.
 *
 * <h2>Why the interaction lives on an item entity</h2>
 *
 * <p>Nothing in vanilla right-clicks a dropped item: {@code ItemEntity} overrides neither
 * {@code interact} nor {@code interactAt}, so the click falls through to nothing. That makes this
 * slot free, and it is a better home for the gesture than a crafting recipe: the machine is taught
 * by reading, so the blueprint is something the wrench <em>writes</em> rather than something a
 * player assembles.
 *
 * <p>NeoForge fires {@code PlayerInteractEvent.EntityInteract} before the entity's own
 * {@code interact} runs, on both sides. It is cancelled with {@code SUCCESS} so the client does not
 * also try to walk up and collect the item, and the conversion itself happens only on the server.
 *
 * <h2>One blueprint per click</h2>
 *
 * <p>A stack of them is a stack of blank sheets, not a stack of blueprints: each click writes one
 * page. Anything left over stays on the ground as its own item entity, which is why {@link #bless}
 * has to be given a level - it may need to put something back.
 */
@EventBusSubscriber(modid = CreateEnigma.MOD_ID)
public final class BlueprintBlessing {

    private BlueprintBlessing() {}

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof ItemEntity dropped)) {
            return;
        }
        if (!event.getItemStack().is(CEItems.ENIGMA_WRENCH.get())) {
            return;
        }
        if (!dropped.getItem().is(AllItems.EMPTY_SCHEMATIC.get())) {
            return;
        }

        // Consume the click on both sides, so the client does not run its own attempt at the item.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide()) {
            return;
        }
        bless(dropped);
    }

    /**
     * Turns one empty blueprint in the stack into this mod's blueprint.
     *
     * <p>Public rather than private so a gametest can drive it without synthesising an interaction
     * event, which would test the event bus rather than the rule.
     *
     * @return whether anything was written
     */
    public static boolean bless(ItemEntity dropped) {
        ItemStack blanks = dropped.getItem();
        if (!blanks.is(AllItems.EMPTY_SCHEMATIC.get())) {
            return false;
        }

        ItemStack written = EnigmaSchematic.blueprintStack();
        if (blanks.getCount() > 1) {
            // Keep the rest on the ground: shrink the pile, then hand the page its own entity so it
            // keeps the pile's momentum instead of appearing at the same spot with zero motion.
            blanks.shrink(1);
            ItemEntity page = new ItemEntity(dropped.level(), dropped.getX(), dropped.getY(),
                    dropped.getZ(), written);
            page.setDeltaMovement(dropped.getDeltaMovement());
            // No pick-up delay copied: the field is private and there is no getter, and a page that
            // can be collected at once is the behaviour a player expects from a click.
            dropped.level().addFreshEntity(page);
        } else {
            dropped.setItem(written);
        }

        dropped.level().playSound(null, dropped.blockPosition(),
                SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }
}
