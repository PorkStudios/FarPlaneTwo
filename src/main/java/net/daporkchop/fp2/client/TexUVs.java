/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.client.gl.object.ShaderStorageBuffer;
import net.daporkchop.fp2.util.DirectBufferReuse;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.math.vector.i.Vec2i;
import net.daporkchop.lib.primitive.map.IntIntMap;
import net.daporkchop.lib.primitive.map.ObjIntMap;
import net.daporkchop.lib.primitive.map.open.IntIntOpenHashMap;
import net.daporkchop.lib.primitive.map.open.ObjIntOpenHashMap;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static net.daporkchop.fp2.client.ClientConstants.*;
import static net.daporkchop.fp2.util.compat.of.OFHelper.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * Global terrain info used by terrain rendering shaders.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class TexUVs {
    public static final ShaderStorageBuffer QUAD_LISTS = new ShaderStorageBuffer();
    public static final ShaderStorageBuffer QUAD_DATA = new ShaderStorageBuffer();

    private static final Map<IBlockState, StateFaceReplacer> STATE_TO_REPLACER = new IdentityHashMap<>();
    private static final StateFaceReplacer DEFAULT_REPLACER = (state, face) -> state;

    private static final Map<IBlockState, StateFaceQuadRenderer> STATE_TO_RENDERER = new IdentityHashMap<>();
    private static final StateFaceQuadRenderer DEFAULT_RENDERER = (state, face, model) -> {
        List<BakedQuad> quads = model.getQuads(state, face, 0L);
        if (quads != null && !quads.isEmpty()) {
            List<PackedBakedQuad> out = new ArrayList<>(quads.size());
            for (int i = 0, len = quads.size(); i < len; i++) {
                out.add(new PackedBakedQuad(quads.get(i)));
            }
            return out;
        }
        return null;
    };

    public static IntIntMap STATEID_TO_INDEXID;

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
            TextureAtlasSprite sprite = mc.getTextureMapBlocks().getAtlasSprite(spriteName);
            return Collections.singletonList(new PackedBakedQuad(sprite.getInterpolatedU(0.0d), sprite.getInterpolatedV(0.0d), sprite.getInterpolatedU(spriteFactor), sprite.getInterpolatedV(spriteFactor), 0.0f));
        };
        putRenderer(Blocks.WATER, waterRenderer);
        putRenderer(Blocks.FLOWING_WATER, waterRenderer);
        putRenderer(Blocks.LAVA, (state, face, model) ->
                Collections.singletonList(new PackedBakedQuad(mc.getTextureMapBlocks().getAtlasSprite("minecraft:blocks/lava_still"), -1)));
        putRenderer(Blocks.FLOWING_LAVA, (state, face, model) ->
                Collections.singletonList(new PackedBakedQuad(mc.getTextureMapBlocks().getAtlasSprite("minecraft:blocks/lava_flow"), -1)));

        //grass renders in two layers, which is somewhat expensive to simulate with shaders. we render the sides as dirt, you can't tell the difference
        // from far away anyway
        putRenderer(Blocks.GRASS, (state, face, model) -> {
            if (OF && PUnsafe.getInt(mc.gameSettings, OF_BETTERGRASS_OFFSET) != OF_OFF) {
                if (face != EnumFacing.DOWN) {
                    return DEFAULT_RENDERER.render(state, EnumFacing.UP, model); //use the top texture for the sides
                }
            }
            return DEFAULT_RENDERER.render(state, face, model);
        });
    }

    public static void reloadUVs() {
        ObjIntMap<List<PackedBakedQuad>> distinctQuadsToId = new ObjIntOpenHashMap<>();
        List<List<PackedBakedQuad>> distinctQuadsById = new ArrayList<>();

        ObjIntMap<int[]> distinctIndicesToId = new IntArrayEqualsMap();
        List<int[]> distinctIndicesById = new ArrayList<>();

        IntIntMap stateIdToIndexId = new IntIntOpenHashMap();

        List<PackedBakedQuad> missingTextureQuads = new ArrayList<>();
        missingTextureQuads.add(new PackedBakedQuad(mc.getTextureMapBlocks().getMissingSprite(), -1));

        BlockModelShapes shapes = mc.getBlockRendererDispatcher().getBlockModelShapes();
        for (Block block : Block.REGISTRY) {
            for (IBlockState state : block.getBlockState().getValidStates()) {
                int[] faceIds = new int[6];

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

                int id = distinctIndicesToId.get(faceIds);
                if (id < 0) { //allocate new ID
                    distinctIndicesToId.put(faceIds, id = distinctIndicesToId.size());
                    distinctIndicesById.add(faceIds);
                }

                stateIdToIndexId.put(Block.getStateId(state), id);
            }
        }

        STATEID_TO_INDEXID = stateIdToIndexId;

        @SuppressWarnings("deprecation")
        ByteBuf buffer = ByteBufAllocator.DEFAULT.directBuffer().order(ByteOrder.nativeOrder());
        try {
            List<Vec2i> quadIdToList = new ArrayList<>(distinctQuadsById.size());
            int quadIndices = 0;
            for (int i = 0, len = distinctQuadsById.size(); i < len; i++) {
                List<PackedBakedQuad> quads = distinctQuadsById.get(i);
                quadIdToList.add(new Vec2i(quadIndices, quadIndices += quads.size()));
                for (PackedBakedQuad quad : quads) {
                    buffer.writeFloat(quad.minU).writeFloat(quad.minV)
                            .writeFloat(quad.maxU).writeFloat(quad.maxV)
                            .writeFloat(quad.tintFactor);
                }
            }
            try (ShaderStorageBuffer ssbo = QUAD_DATA.bind()) { //upload data
                glBufferData(GL_SHADER_STORAGE_BUFFER, DirectBufferReuse.wrapByte(buffer.memoryAddress(), buffer.readableBytes()), GL_STATIC_DRAW);
            }

            buffer.clear();

            for (int[] faceIds : distinctIndicesById) {
                for (int i : faceIds) {
                    Vec2i list = quadIdToList.get(i);
                    buffer.writeInt(list.getX()).writeInt(list.getY());
                }
            }
            try (ShaderStorageBuffer ssbo = QUAD_LISTS.bind()) { //upload data
                glBufferData(GL_SHADER_STORAGE_BUFFER, DirectBufferReuse.wrapByte(buffer.memoryAddress(), buffer.readableBytes()), GL_STATIC_DRAW);
            }
        } finally {
            buffer.release();
        }
    }

    @FunctionalInterface
    public interface StateFaceReplacer {
        IBlockState replace(IBlockState state, EnumFacing face);
    }

    @FunctionalInterface
    public interface StateFaceQuadRenderer {
        List<PackedBakedQuad> render(IBlockState state, EnumFacing face, IBakedModel model);
    }

    @AllArgsConstructor
    @EqualsAndHashCode
    public class PackedBakedQuad {
        public final float minU;
        public final float minV;
        public final float maxU;
        public final float maxV;
        public final float tintFactor;

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
