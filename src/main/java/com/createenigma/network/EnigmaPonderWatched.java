package com.createenigma.network;

import com.createenigma.CreateEnigma;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * "I finished watching a ponder scene."
 *
 * <p>Sent client to server, because Ponder is purely client-side: the server has no idea what scene
 * a player is looking at, and advancements are granted on the server.
 *
 * <h2>Why this now carries a scene id</h2>
 *
 * <p>It used to carry nothing, on the reasoning that a payload with no fields gives the server
 * nothing to have to validate. That held while there was exactly one fact to report. There are now
 * two - "watched some ponder" and "watched <em>that</em> ponder" - and they are the same message
 * with one extra bit of information, so the id travels rather than a second payload type existing
 * for the sake of a boolean.
 *
 * <p><b>The server still cannot check this.</b> Ponder has no server-side representation, so the
 * id is taken on trust: a modified client could claim any scene. That remains acceptable only
 * because the whole consequence is a cosmetic advancement. If this ever gates anything that
 * matters, the check has to move server-side rather than this packet growing more trusted.
 */
public record EnigmaPonderWatched(ResourceLocation sceneId) implements CustomPacketPayload {

    /** Create's scene about the machine - the one that also grants the Enigma itself. */
    public static final ResourceLocation ENIGMA_SCENE =
            ResourceLocation.fromNamespaceAndPath("create", "creative_motor_mojang");

    public static final CustomPacketPayload.Type<EnigmaPonderWatched> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "ponder_watched"));

    public static final StreamCodec<ByteBuf, EnigmaPonderWatched> CODEC =
            ResourceLocation.STREAM_CODEC.map(EnigmaPonderWatched::new, EnigmaPonderWatched::sceneId);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
