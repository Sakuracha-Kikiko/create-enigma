package com.createenigma.client;

import com.createenigma.CreateEnigma;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueHandler;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the Enigma Wrench: the body, then a gear that turns as the player scrolls.
 *
 * <h2>Why a copy of {@code WrenchItemRenderer} instead of using it</h2>
 *
 * <p>The gear is a <b>second</b> baked model, not part of the wrench's own geometry, and Create's
 * renderer holds it in a {@code protected static final} field pointing at
 * {@code create:item/wrench/gear}. That model names Create's own texture, so reusing the renderer
 * would draw our body in violet and leave the gear gold - the one part of the item that moves would
 * be the part that did not match.
 *
 * <p>So the gear is re-pointed at {@code create_enigma:item/wrench_gear}, which is Create's gear
 * geometry with our texture named instead of theirs. That model json is three lines and copies no
 * geometry: {@code "parent": "create:item/wrench/gear"} inherits every element, and only the
 * texture binding changes.
 *
 * <p>The rest is Create's own arithmetic, kept identical on purpose - and the signs are worth a
 * warning. The pivot is the gear's <b>own axis</b>, and it is reached by
 * {@code translate(+0.0625)} then {@code translate(-0.0625)} around the rotation. Swapping them, or
 * writing the pair symmetrically the other way round, moves the pivot 0.125 off the gear's centre and
 * the gear <b>orbits</b> instead of turning: it still moves, so the mistake looks like a rendering
 * quirk rather than a wrong number. In Create's source it reads as one signed local negated at the
 * first call site ({@code fneg} in {@code WrenchItemRenderer}), which is easy to miss when copying
 * and easy to "tidy up" wrongly. The angle comes from {@code ScrollValueHandler.getScroll}, which is
 * why the gear turns when the player scrolls while holding the wrench instead of spinning on its own.
 */
public class EnigmaWrenchRenderer extends CustomRenderedItemModelRenderer {

    /** Create's gear geometry, bound to this mod's texture. */
    private static final PartialModel GEAR = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "item/wrench_gear"));

    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer partial,
            ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        partial.render(model.getOriginalModel(), light);

        pose.translate(0.0625, 0, 0);
        pose.mulPose(Axis.YP.rotationDegrees(
                ScrollValueHandler.getScroll(AnimationTickHolder.getPartialTicks())));
        pose.translate(-0.0625, 0, 0);

        partial.render(GEAR.get(), light);
    }
}
