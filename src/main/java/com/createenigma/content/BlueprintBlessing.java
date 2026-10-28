package com.createenigma.content;

import com.createenigma.CreateEnigma;
import com.createenigma.registry.CEItems;
import com.createenigma.structure.EnigmaSchematic;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 神秘扳手把空白蓝图写成神秘蓝图 - the wrench writes a page of the machine's blueprint.
 *
 * <h2>Two places it can be done, and why the depot is the one that matters</h2>
 *
 * <p>The ritual is on a depot: a blank blueprint lying out in the open, a wrench held over it, and
 * the sheet is written in place. That reads, which is the point - the machine is taught by reading,
 * so its blueprint should be something the wrench <em>writes</em> rather than something a player
 * assembles from a recipe grid.
 *
 * <p>The same click also works on a blank blueprint dropped on the ground. It is kept because
 * nothing in vanilla right-clicks a dropped item - {@code ItemEntity} overrides neither
 * {@code interact} nor {@code interactAt} - so the slot is free, but aiming at a small entity on the
 * floor is fiddly, and a depot is what the player will reach for.
 *
 * <h2>How the click is taken</h2>
 *
 * <p>Both paths listen to NeoForge's interaction events, which fire before the block or entity is
 * asked, and both cancel with {@code SUCCESS} on both sides so the client does not run its own
 * attempt. Only the server writes the page.
 */
@EventBusSubscriber(modid = CreateEnigma.MOD_ID)
public final class BlueprintBlessing {

    private BlueprintBlessing() {}

    /**
     * The ritual: wrench on a depot holding a blank blueprint.
     *
     * <p>Right-click-block rather than the depot's own interaction, for the same reason the core is
     * handled by the wrench: the event fires before the block is asked, so the click cannot be
     * swallowed by the depot trying to put the wrench down on itself.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof DepotBlockEntity depot)) {
            return;
        }
        if (!event.getItemStack().is(CEItems.ENIGMA_WRENCH.get())) {
            return;
        }
        if (!depot.getHeldItem().is(AllItems.EMPTY_SCHEMATIC.get())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide()) {
            return;
        }
        bless(depot);
    }

    /**
     * Writes the whole sheet lying on the depot - the sheet is on display, so the pile on it is the
     * pile that gets written, count and all.
     *
     * <p>{@code notifyUpdate} is what puts the page in front of the player: the held item goes out
     * in every block entity packet, so one update is enough and the depot's own renderer picks it up.
     *
     * @return whether anything was written
     */
    public static boolean bless(DepotBlockEntity depot) {
        ItemStack blanks = depot.getHeldItem();
        if (!blanks.is(AllItems.EMPTY_SCHEMATIC.get())) {
            return false;
        }

        ItemStack written = EnigmaSchematic.blueprintStack();
        written.setCount(blanks.getCount());
        depot.setHeldItem(written);
        depot.notifyUpdate();

        depot.getLevel().playSound(null, depot.getBlockPos(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

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
