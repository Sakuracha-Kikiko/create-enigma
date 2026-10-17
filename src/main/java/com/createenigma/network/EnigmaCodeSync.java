package com.createenigma.network;

import com.createenigma.CreateEnigma;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * This world's three assembly numbers, sent from the server to the client.
 *
 * <h2>Why three numbers and not the seed</h2>
 *
 * <p>The seed never reaches a client - vanilla sends it to nobody, and {@code /seed} works only
 * because the server answers a request. Something has to cross the gap, and the choice is what.
 *
 * <p>Sending the seed would let the client work out anything else the server ever derives from it,
 * now and later. Sending the three numbers gives away exactly the three digits the scene is meant to
 * reveal, and nothing else. If a future puzzle wants a different secret from the same seed, it needs
 * its own field here, which is the point: what is sent stays small and deliberate.
 *
 * <h2>Why it must be sent at all</h2>
 *
 * <p>Ponder scenes are built on the client, and the scene has to print the digits. The client cannot
 * derive them, so it is told. {@code EnigmaCodeSyncClient} stores them and rebuilds the ponder index,
 * because the first build happens before any server connection exists and would otherwise bake in
 * the wrong numbers for the rest of the session.
 */
public record EnigmaCodeSync(int first, int second, int third) implements CustomPacketPayload {

    public static final Type<EnigmaCodeSync> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "code_sync"));

    public static final StreamCodec<ByteBuf, EnigmaCodeSync> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EnigmaCodeSync::first,
            ByteBufCodecs.VAR_INT, EnigmaCodeSync::second,
            ByteBufCodecs.VAR_INT, EnigmaCodeSync::third,
            EnigmaCodeSync::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** "3, 2, 5." - the line the scene prints, in one place so both ends read the same. */
    public String describe() {
        return first + ", " + second + ", " + third + ".";
    }
}
