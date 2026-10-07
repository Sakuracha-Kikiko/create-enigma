package com.createenigma.network;

import com.createenigma.CreateEnigma;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * "I finished watching Mojang's Enigma."
 *
 * <p>Sent client to server, because Ponder is purely client-side: the server has no idea what scene
 * a player is looking at, and advancements are granted on the server.
 *
 * <p><b>Deliberately carries no fields.</b> The message <em>is</em> the payload type, so there is
 * no client-supplied data for the server to have to validate - a client can claim this one fact
 * and nothing else. If more scenes ever need reporting, add a field then and validate it; do not
 * start by sending a scene id the server blindly trusts.
 */
public record EnigmaPonderWatched() implements CustomPacketPayload {

    /** The one scene this payload is about. */
    public static final ResourceLocation ENIGMA_SCENE =
            ResourceLocation.fromNamespaceAndPath("create", "creative_motor_mojang");

    public static final EnigmaPonderWatched INSTANCE = new EnigmaPonderWatched();

    public static final CustomPacketPayload.Type<EnigmaPonderWatched> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "ponder_watched"));

    /** Nothing to write: the type carries the whole message. */
    public static final StreamCodec<RegistryFriendlyByteBuf, EnigmaPonderWatched> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
