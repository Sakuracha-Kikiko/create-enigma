package com.createenigma.client;

import com.createenigma.CreateEnigma;
import com.createenigma.client.ponder.EnigmaPonderPlugin;
import com.mojang.logging.LogUtils;

import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import org.slf4j.Logger;

/**
 * Client-only setup.
 *
 * <p>Ponder scenes are registered through a plugin added during client setup - the same place and
 * the same call Create uses for its own plugin. Scenes are client-side data: the index is built on
 * the client and there is nothing for the server to know about.
 *
 * <p>The log line exists because plugin registration is the first of three steps that can each
 * fail without any visible symptom - add the plugin, register the scene, then have the scene's
 * item survive the index filters. If the plugin never gets added, nothing downstream happens and
 * the only evidence is its absence.
 */
@EventBusSubscriber(modid = CreateEnigma.MOD_ID, value = Dist.CLIENT)
public final class EnigmaClientSetup {

    private static final Logger LOGGER = LogUtils.getLogger();

    private EnigmaClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("Enigma ponder: adding plugin");
        PonderIndex.addPlugin(new EnigmaPonderPlugin());
    }
}
