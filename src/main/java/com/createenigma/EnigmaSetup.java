package com.createenigma;

import com.createenigma.structure.EnigmaSchematic;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Startup work that is not registration.
 *
 * <p>Only one job so far: writing this mod's blueprint out to the schematics folder, which is the
 * only way to hand Create's own schematic system a structure - a blueprint item carries a file name,
 * not the structure itself.
 *
 * <p>Common setup rather than client setup, because both sides read the file from different places
 * and both need their copy. In single player that is the same machine; on a server the two paths
 * exist separately and each side writes its own.
 */
@EventBusSubscriber(modid = CreateEnigma.MOD_ID)
public final class EnigmaSetup {

    private EnigmaSetup() {}

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // Deferred: common setup runs in parallel across mods, and this touches the file system and
        // reads block states out of the registry.
        event.enqueueWork(EnigmaSchematic::extract);
    }
}
