package com.createenigma.client;

import com.createenigma.CreateEnigma;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import net.minecraft.resources.ResourceLocation;

/**
 * This mod's Flywheel partial models, and the one rule that makes them work.
 *
 * <h2>A partial model must exist before the model bake</h2>
 *
 * <p>{@code PartialModel} is not a lazy handle. Its constructor is the only thing that ever fills in
 * the baked model:
 *
 * <pre>
 *   private PartialModel(ResourceLocation id) {
 *       if (populateOnInit) { this.bakedModel = FlwLibXplat.getBakedModel(modelManager, id); }
 *   }
 *   public BakedModel get() { return bakedModel; }
 * </pre>
 *
 * <p>{@code populateOnInit} is only true while the model bake is running, and {@code of(id)} returns
 * whatever already sits in Flywheel's {@code ALL} map. So a partial model first asked for <b>after</b>
 * the bake has {@code bakedModel == null} for the rest of the session, and
 * {@code Models.partial(null)} draws the missing model - a full cube of missing texture, with nothing
 * in the log to say so.
 *
 * <p>That is exactly what happened here: the reference lived in the visual's static initializer, which
 * does not run until a crank is first drawn, which is long after the bake. The block showed as a
 * missing-texture cube while its item - which uses an ordinary model - looked correct.
 *
 * <p>So every partial model belongs in this class, and {@link #init()} is called from client setup,
 * before any bake. Initialising this class is what puts the entries into Flywheel's map early enough
 * for them to be filled.
 */
public final class EnigmaPartialModels {

    /**
     * The creative hand crank's arm: Create's geometry by name, this mod's textures.
     *
     * <p>Three-line model json, no geometry copied - see
     * {@code assets/create_enigma/models/block/creative_hand_crank/handle.json}.
     */
    public static final PartialModel CRANK_HANDLE = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "block/creative_hand_crank/handle"));

    private EnigmaPartialModels() {}

    /** Loads this class, which is what registers the partial models. Never skip it. */
    public static void init() {
        // Class loading performs the registration.
    }
}
