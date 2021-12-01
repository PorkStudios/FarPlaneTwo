/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.fp2.client;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.event.ReloadEvent;
import net.daporkchop.fp2.core.event.AbstractReloadEvent;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.attribute.Attribute;
import net.daporkchop.fp2.gl.attribute.uniform.UniformArrayBuffer;
import net.daporkchop.fp2.gl.attribute.uniform.UniformArrayFormat;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.primitive.map.ObjIntMap;
import net.daporkchop.lib.primitive.map.open.ObjIntOpenHashMap;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static net.daporkchop.fp2.compat.of.OFHelper.*;
import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * Global terrain info used by terrain rendering shaders.
 *
 * @author DaPorkchop_
 */
@Getter
public class TextureUVs extends AbstractReleasable {
    private static final Map<IBlockState, StateFaceReplacer> STATE_TO_REPLACER = new IdentityHashMap<>();
    private static final StateFaceReplacer DEFAULT_REPLACER = (state, face) -> state;

    private static final Map<IBlockState, StateFaceQuadRenderer> STATE_TO_RENDERER = new IdentityHashMap<>();
    private static final StateFaceQuadRenderer DEFAULT_RENDERER = (state, face, model) -> {
        List<BakedQuad> quads = model.getQuads(state, face, 0L);
        if (quads.isEmpty()) { //the model has no cullfaces for the given facing direction, try to find a matching non-cullface
            for (BakedQuad quad : model.getQuads(state, null, 0L)) {
                if (quad.getFace() == face) {
                    quads = Collections.singletonList(quad);
                    break;
                }
            }
        }

        //TODO: we could possibly do something using the code from FaceBakery#getFacingFromVertexData(int[]) to find the quad closest to this face, even if it's not
        // an exact match...

        if (!quads.isEmpty()) {
            List<PackedBakedQuad> out = new ArrayList<>(quads.size());
            for (int i = 0, len = quads.size(); i < len; i++) {
                out.add(new PackedBakedQuad(quads.get(i)));
            }
            return out;
        }
        return null;
    };

    public static Reference2IntMap<IBlockState> STATEID_TO_INDEXID;

    public static void putReplacer(@NonNull Block block, @NonNull StateFaceReplacer replacer) {
        for (IBlockState state : block.getBlockState().getValidStates()) {
            putReplacer(state, replacer);
        }
    }

    public static void putReplacer(@NonNull IBlockState state, @NonNull StateFaceReplacer replacer) {
        STATE_TO_REPLACER.put(state, replacer);
    }

    public static void putRenderer(@NonNull Block block, @NonNull StateFaceQuadRenderer renderer) {
        for (IBlockState state : block.getBlockState().getValidStates()) {
            putRenderer(state, renderer);
        }
    }

    public static void putRenderer(@NonNull IBlockState state, @NonNull StateFaceQuadRenderer renderer) {
        STATE_TO_RENDERER.put(state, renderer);
    }

    public static void initDefault() {
        //fluids use their own system for rendering
        StateFaceQuadRenderer waterRenderer = (state, face, model) -> {
            String spriteName = face.getHorizontalIndex() < 0 ? "minecraft:blocks/water_still" : "minecraft:blocks/water_flow";
            double spriteFactor = face.getHorizontalIndex() < 0 ? 16.0d : 8.0d;
            TextureAtlasSprite sprite = MC.getTextureMapBlocks().getAtlasSprite(spriteName);
            return Collections.singletonList(new PackedBakedQuad(sprite.getInterpolatedU(0.0d), sprite.getInterpolatedV(0.0d), sprite.getInterpolatedU(spriteFactor), sprite.getInterpolatedV(spriteFactor), 0.0f));
        };
        putRenderer(Blocks.WATER, waterRenderer);
        putRenderer(Blocks.FLOWING_WATER, waterRenderer);
        putRenderer(Blocks.LAVA, (state, face, model) ->
                Collections.singletonList(new PackedBakedQuad(MC.getTextureMapBlocks().getAtlasSprite("minecraft:blocks/lava_still"), -1)));
        putRenderer(Blocks.FLOWING_LAVA, (state, face, model) ->
                Collections.singletonList(new PackedBakedQuad(MC.getTextureMapBlocks().getAtlasSprite("minecraft:blocks/lava_flow"), -1)));

        //grass renders in two layers, which is somewhat expensive to simulate with shaders. we render the sides as dirt, you can't tell the difference
        // from far away anyway
        putRenderer(Blocks.GRASS, (state, face, model) -> {
            if (OF && PUnsafe.getInt(MC.gameSettings, OF_BETTERGRASS_OFFSET) != OF_OFF) {
                if (face != EnumFacing.DOWN) {
                    return DEFAULT_RENDERER.render(state, EnumFacing.UP, model); //use the top texture for the sides
                }
            }
            return DEFAULT_RENDERER.render(state, face, model);
        });

        //reload texture UVs for the first time
        reloadAll();
    }

    public static void reloadAll() {
        new AbstractReloadEvent<TextureUVs>() {
            @Override
            protected void handleSuccess(int total) {
                fp2().chat().success("§a%d texture UV caches successfully reloaded.", total);
            }

            @Override
            protected void handleFailure(int failed, int total, @NonNull Throwable cause) {
                fp2().log().error("texture UV cache reload failed", cause);
                fp2().chat().error("§c%d/%d texture UV cache failed to reload (check log for info)", failed, total);
            }
        }.fire();
    }

    protected final GL gl;

    protected final UniformArrayFormat<QuadList> listsFormat;
    protected final UniformArrayBuffer<QuadList> listsBuffer;

    protected final UniformArrayFormat<PackedBakedQuad> quadsFormat;
    protected final UniformArrayBuffer<PackedBakedQuad> quadsBuffer;

    public TextureUVs(@NonNull GL gl) {
        this.gl = gl;

        this.listsFormat = gl.createUniformArrayFormat(QuadList.class).build();
        this.listsBuffer = this.listsFormat.createBuffer(BufferUsage.STATIC_DRAW);

        this.quadsFormat = gl.createUniformArrayFormat(PackedBakedQuad.class).build();
        this.quadsBuffer = this.quadsFormat.createBuffer(BufferUsage.STATIC_DRAW);

        this.reloadUVs();

        fp2().eventBus().registerWeak(this);
    }

    @Override
    protected void doRelease() {
        fp2().eventBus().unregister(this);

        this.quadsBuffer.close();
        this.listsBuffer.close();
    }

    @FEventHandler
    protected void onReload(@NonNull ReloadEvent<TextureUVs> event) {
        event.doReload(this::reloadUVs);
    }

    protected void reloadUVs() {
        if (MC.getTextureMapBlocks() == null) { //texture map hasn't been initialized yet, meaning the game is still starting
            return;
        }

        ObjIntMap<List<PackedBakedQuad>> distinctQuadsToId = new ObjIntOpenHashMap<>();
        List<List<PackedBakedQuad>> distinctQuadsById = new ArrayList<>();

        ObjIntMap<int[]> distinctIndicesToId = new IntArrayEqualsMap();
        List<int[]> distinctIndicesById = new ArrayList<>();

        Reference2IntMap<IBlockState> stateIdToIndexId = new Reference2IntOpenHashMap<>();

        List<PackedBakedQuad> missingTextureQuads = new ArrayList<>();
        missingTextureQuads.add(new PackedBakedQuad(MC.getTextureMapBlocks().getMissingSprite(), -1));

        List<IBlockState> erroredStates = new ArrayList<>();

        BlockModelShapes shapes = MC.getBlockRendererDispatcher().getBlockModelShapes();
        for (Block block : Block.REGISTRY) {
            for (IBlockState state : block.getBlockState().getValidStates()) {
                int[] faceIds = new int[6];

                try {
                    StateFaceReplacer replacer = STATE_TO_REPLACER.getOrDefault(state, DEFAULT_REPLACER);
                    StateFaceQuadRenderer renderer = STATE_TO_RENDERER.getOrDefault(state, DEFAULT_RENDERER);
                    for (EnumFacing face : EnumFacing.VALUES) {
                        IBlockState replacedState = replacer.replace(state, face);
                        IBakedModel model = shapes.getModelForState(replacedState);
                        List<PackedBakedQuad> quads = PorkUtil.fallbackIfNull(renderer.render(replacedState, face, model), missingTextureQuads);

                        int id = distinctQuadsToId.getOrDefault(quads, -1);
                        if (id < 0) { //allocate new ID
                            distinctQuadsToId.put(quads, id = distinctQuadsToId.size());
                            distinctQuadsById.add(quads);
                        }

                        faceIds[face.getIndex()] = id;
                    }
                } catch (Exception e) { //we couldn't process the state (likely a buggy mod block), so let's just ignore it for now
                    FP2_LOG.error("exception while generating texture UVs for " + state, e);
                    erroredStates.add(state);
                    continue;
                }

                int id = distinctIndicesToId.get(faceIds);
                if (id < 0) { //allocate new ID
                    distinctIndicesToId.put(faceIds, id = distinctIndicesToId.size());
                    distinctIndicesById.add(faceIds);
                }

                stateIdToIndexId.put(state, id);
            }
        }

        if (!erroredStates.isEmpty()) { //some block states failed!
            FP2_LOG.error("failed to generate texture UVs for {} block states, they will be replaced with missing textures:", erroredStates.size());
            erroredStates.forEach(FP2_LOG::error);

            int id = stateIdToIndexId.getInt(Blocks.AIR.getDefaultState());
            erroredStates.forEach(state -> stateIdToIndexId.put(state, id));
        }

        STATEID_TO_INDEXID = stateIdToIndexId;

        QuadList[] quadIdToList = new QuadList[distinctQuadsById.size()];
        List<PackedBakedQuad> quadsOut = new ArrayList<>(distinctQuadsById.size());
        for (int i = 0, len = distinctQuadsById.size(); i < len; i++) {
            List<PackedBakedQuad> quads = distinctQuadsById.get(i);
            quadIdToList[i] = new QuadList(quadsOut.size(), quadsOut.size() + quads.size());
            quadsOut.addAll(quads);
        }
        this.quadsBuffer.set(quadsOut.toArray(new PackedBakedQuad[0]));

        List<QuadList> listsOut = new ArrayList<>(quadIdToList.length);
        for (int[] faceIds : distinctIndicesById) {
            for (int i : faceIds) {
                listsOut.add(quadIdToList[i]);
            }
        }
        this.listsBuffer.set(listsOut.toArray(new QuadList[0]));
    }

    /**
     * @author DaPorkchop_
     */
    @FunctionalInterface
    public interface StateFaceReplacer {
        IBlockState replace(IBlockState state, EnumFacing face);
    }

    /**
     * @author DaPorkchop_
     */
    @FunctionalInterface
    public interface StateFaceQuadRenderer {
        List<PackedBakedQuad> render(IBlockState state, EnumFacing face, IBakedModel model);
    }

    /**
     * @author DaPorkchop_
     */
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class QuadList {
        @Attribute(vectorAxes = { "First", "Last" }, convert = Attribute.Conversion.TO_UNSIGNED)
        public final int ua_texQuadListFirst;
        public final int ua_texQuadListLast;
    }

    /**
     * @author DaPorkchop_
     */
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class PackedBakedQuad {
        @Attribute(vectorAxes = { "S", "T", "P", "Q" })
        public final float ua_texQuadCoordS;
        public final float ua_texQuadCoordT;
        public final float ua_texQuadCoordP;
        public final float ua_texQuadCoordQ;

        @Attribute
        public final float ua_texQuadTint;

        public PackedBakedQuad(TextureAtlasSprite sprite, float tintFactor) {
            this(sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV(), tintFactor);
        }

        public PackedBakedQuad(TextureAtlasSprite sprite, int tintIndex) {
            this(sprite, tintIndex == -1 ? 1.0f : 0.0f);
        }

        public PackedBakedQuad(BakedQuad quad) {
            this(quad.getSprite(), quad.getTintIndex());
        }
    }

    /**
     * @author DaPorkchop_
     */
    private static class IntArrayEqualsMap extends ObjIntOpenHashMap<int[]> {
        @Override
        protected int hash0(int[] key) {
            return Arrays.hashCode(key);
        }

        @Override
        protected boolean equals0(int[] k1, int[] k2) {
            return Arrays.equals(k1, k2);
        }
    }
}
