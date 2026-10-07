package com.createenigma.network;

import com.createenigma.CreateEnigma;
import com.createenigma.content.EnigmaAdvancements;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Wires up this mod's packets.
 *
 * <p>Only one, and only ever client to server: the client reporting that it watched the Enigma
 * ponder. Everything the server needs to decide is derived from the payload type alone.
 */
@EventBusSubscriber(modid = CreateEnigma.MOD_ID)
public final class CENetwork {

    private CENetwork() {}

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(EnigmaPonderWatched.TYPE, EnigmaPonderWatched.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        // The client is trusted here on purpose: an advancement is cosmetic, and
                        // there is no server-side way to observe that a ponder was watched. If
                        // this ever gates something that matters, add a server-side check rather
                        // than believing this packet.
                        EnigmaAdvancements.grantEnigma(player);
                    }
                }));
    }
}
