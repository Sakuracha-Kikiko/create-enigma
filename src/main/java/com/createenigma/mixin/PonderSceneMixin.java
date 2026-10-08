package com.createenigma.mixin;

import com.createenigma.network.EnigmaPonderWatched;
import com.mojang.logging.LogUtils;

import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reports every ponder scene the player watches to the end, and which scene it was.
 *
 * <p><b>Client only</b> - it is in the {@code client} list of the mixin config. Ponder plays
 * entirely on the client, and {@link PacketDistributor#sendToServer} only exists there.
 *
 * <p><b>Why progress and not {@code setFinished}.</b> {@code PonderScene.setFinished(boolean)} looks
 * like the obvious hook, and it is the wrong one: it is only ever called by
 * {@code MarkAsFinishedInstruction}, and {@code creativeMotorMojang} is the one scene in
 * {@code KineticsScenes} that never calls {@code markAsFinished()}. Its {@code isFinished()} stays
 * false forever, so an injection there would compile, apply, and silently never fire - and it would
 * have looked correct for every <em>other</em> scene, which is worse. Completion is observed
 * through {@code getSceneProgress() == 1.0} instead, which is right for all of them.
 *
 * <p>{@code PonderUI} also has a progress bar and a public {@code seekToTime}, so a player can drag
 * to the end rather than watching. That is accepted: the alternative is tracking playback in a way
 * the UI does not expose, for advancements that are cosmetic. Noted so nobody later mistakes this
 * for proof that a scene was actually watched.
 */
@Mixin(PonderScene.class)
public abstract class PonderSceneMixin {

    @Unique
    private static final Logger CREATE_ENIGMA$LOGGER = LogUtils.getLogger();

    @Shadow
    public abstract ResourceLocation getId();

    @Shadow
    public abstract float getSceneProgress();

    @Unique
    private boolean create_enigma$reported;

    @Inject(method = "tick", at = @At("TAIL"))
    private void create_enigma$reportWatchedScene(CallbackInfo ci) {
        if (create_enigma$reported) {
            return;
        }
        if (this.getSceneProgress() < 1.0F) {
            return;
        }

        // Once per scene instance. A scene can be replayed (which resets progress), but a replay
        // has nothing new to report, and the awards themselves are idempotent server-side.
        create_enigma$reported = true;

        ResourceLocation id = this.getId();
        if (id == null) {
            return;
        }

        // Logged because this is the one step no automated test can reach: it only runs on a
        // client, and a mixin that failed to apply would otherwise be indistinguishable from a
        // player who simply never watched anything.
        CREATE_ENIGMA$LOGGER.info("Watched ponder scene {}; reporting it", id);

        PacketDistributor.sendToServer(new EnigmaPonderWatched(id));
    }
}
