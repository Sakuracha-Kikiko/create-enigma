package com.createenigma.client;

import com.createenigma.CreateEnigma;
import com.createenigma.client.ponder.EnigmaPonderPlugin;

import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only setup.
 *
 * <p>Ponder scenes are registered through a plugin added during client setup - the same place and
 * the same call Create uses for its own plugin. Scenes are client-side data: the index is built on
 * the client and there is nothing for the server to know about.
 */
@EventBusSubscriber(modid = CreateEnigma.MOD_ID, value = Dist.CLIENT)
public final class EnigmaClientSetup {

    private EnigmaClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        PonderIndex.addPlugin(new EnigmaPonderPlugin());
    }
}
