package com.createenigma.client.ponder;

import com.createenigma.CreateEnigma;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * Adds this mod's own ponder scene to Create's Creative Motor.
 *
 * <p><b>Why a new scene instead of fixing Create's.</b> Ponder has no way to replace or remove a
 * registered scene: {@code PonderSceneRegistry.addStoryBoard} puts into a {@code LinkedHashMultimap}
 * (append, never overwrite) and the registry exposes no remove method at all. The only lever is
 * {@code IndexExclusionHelper}, and that excludes a whole <em>item</em> - using it here would take
 * the Creative Motor's normal scenes down with it. So the original scene stays exactly as Create
 * wrote it, and this one is added after it.
 *
 * <p><b>Order comes from registration, not from an explicit rule.</b> The registry uses a
 * {@code LinkedHashMultimap}, so an item's scenes iterate in the order they were registered. This
 * mod depends on Create, so Create's plugin is added first and its two Creative Motor scenes land
 * first; ours is third. Create itself relies on the same behaviour - it never calls
 * {@code orderBefore}/{@code orderAfter} anywhere.
 *
 * <p><b>The schematic is Create's, referenced rather than copied.</b> The storyboard form that
 * takes a scene id string resolves that string to a schematic under this mod's own namespace,
 * which would mean shipping a second copy of the machine. Passing a {@link ResourceLocation}
 * instead points straight at Create's existing {@code create:ponder/creative_motor_mojang}, whose
 * contents are what we want to show anyway - the file holds the complete machine, including the
 * motor that Create's scene never reveals.
 */
public class EnigmaPonderPlugin implements PonderPlugin {

    private static final ResourceLocation CREATIVE_MOTOR =
            ResourceLocation.fromNamespaceAndPath("create", "creative_motor");

    private static final ResourceLocation MOJANG_SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath("create", "ponder/creative_motor_mojang");

    @Override
    public String getModId() {
        return CreateEnigma.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.addStoryBoard(CREATIVE_MOTOR, MOJANG_SCHEMATIC, EnigmaScenes::firstOfAllMachines);
    }
}
