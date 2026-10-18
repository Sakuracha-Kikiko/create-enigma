package com.createenigma.client;

import com.createenigma.CreateEnigma;
import com.createenigma.registry.CEItems;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;

import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Gives the Enigma Wrench the spinning gear that Create's own wrench has.
 *
 * <h2>Why our wrench had no gear</h2>
 *
 * <p>Create's wrench is not a plain model. {@code WrenchItemRenderer} draws its base model and then
 * draws a <b>second</b> model - a Flywheel {@code PartialModel} baked from
 * {@code create:item/wrench/gear} - rotated by
 * {@code ScrollValueHandler.getScroll(partialTicks)}, which is why the gear turns when the player
 * scrolls while holding the wrench. Our item inherited the wrench's <i>model json</i> and nothing
 * else, so no renderer existed to draw the gear and it was simply absent.
 *
 * <h2>Why this is not {@code EnigmaWrenchItem.initializeClient}</h2>
 *
 * <p>That is how Create does it for its own wrench, and it is one line - but it does not survive a
 * dedicated server here. Overriding {@code initializeClient} makes the item class's bytecode mention
 * {@code WrenchItemRenderer}, whose superclass {@code BlockEntityWithoutLevelRenderer} is
 * client-only; verifying the item class while the mod is being constructed on a server therefore
 * fails with <em>Attempted to load class net/minecraft/client/renderer/
 * BlockEntityWithoutLevelRenderer for invalid dist DEDICATED_SERVER</em>, and the mod does not load
 * at all. It was caught by the gametest server, which is exactly the dedicated-server path.
 *
 * <p>{@code RegisterClientExtensionsEvent} is the same extension point without that coupling: it is
 * a client-only mod-bus event, so nothing in the common item class ever names a client type.
 * {@code SimpleCustomRenderer.create} also registers the item in Create's {@code CustomRenderedItems},
 * which is what makes the item's baked model a {@code CustomRenderedItemModel} - the custom renderer
 * entry point hard-casts to that type.
 *
 * <p>The renderer is this mod's own ({@link EnigmaWrenchRenderer}) rather than Create's, because
 * only a renderer written here can point the gear at this mod's texture.
 */
@EventBusSubscriber(modid = CreateEnigma.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class EnigmaClientExtensions {

    private EnigmaClientExtensions() {}

    @SubscribeEvent
    public static void registerItemRenderers(RegisterClientExtensionsEvent event) {
        Item wrench = CEItems.ENIGMA_WRENCH.get();
        event.registerItem(SimpleCustomRenderer.create(wrench, new EnigmaWrenchRenderer()), wrench);
    }
}
