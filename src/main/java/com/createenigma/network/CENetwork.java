package com.createenigma.network;

import com.createenigma.CreateEnigma;
import com.createenigma.client.ponder.EnigmaCodeSyncClient;
import com.createenigma.content.EnigmaAdvancements;
import com.createenigma.content.EnigmaCode;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Wires up this mod's packets.
 *
 * <p>Two, in opposite directions:
 *
 * <ul>
 *   <li><b>Client to server:</b> a finished ponder scene. Which advancements that earns is decided
 *       here, from the scene id in the payload.</li>
 *   <li><b>Server to client:</b> this world's three assembly numbers, sent once per login. Ponder
 *       scenes are built on the client and have to print them; the client cannot derive them.</li>
 * </ul>
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
                        // The client is trusted here on purpose: advancements are cosmetic, and
                        // there is no server-side way to observe that a ponder was watched. If
                        // this ever gates something that matters, add a server-side check rather
                        // than believing this packet more.
                        EnigmaAdvancements.grantPonderWatched(player);

                        if (EnigmaPonderWatched.ENIGMA_SCENE.equals(payload.sceneId())) {
                            EnigmaAdvancements.grantEnigma(player);
                        }
                    }
                }));

        // The client class is only touched when this runs, and it only runs on a client. That is
        // the standard shape for a clientbound handler in a class that also loads on a server.
        registrar.playToClient(EnigmaCodeSync.TYPE, EnigmaCodeSync.CODEC,
                (payload, context) -> context.enqueueWork(() -> EnigmaCodeSyncClient.accept(payload)));
    }

    /**
     * Tells each player what their world's numbers are, on the way in.
     *
     * <p>On login rather than on server start: a dedicated server may have nobody connected when it
     * starts, and single player logs in once per world - which is also the moment a client switching
     * saves needs to be told again, because it keeps its previous session's numbers otherwise.
     */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        int[] progresses = EnigmaCode.wrenchProgresses(level.getServer().overworld().getSeed());
        PacketDistributor.sendToPlayer(player,
                new EnigmaCodeSync(progresses[0], progresses[1], progresses[2]));
    }
}
