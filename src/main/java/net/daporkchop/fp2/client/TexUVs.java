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
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.client.gl.object.ShaderStorageBuffer;
import net.daporkchop.fp2.util.DirectBufferReuse;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.primitive.map.ObjIntMap;
import net.daporkchop.lib.primitive.map.open.ObjIntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static net.daporkchop.fp2.client.ClientConstants.*;
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

    private static final Map<IBlockState, StateFaceQuadRenderer> STATE_TO_RENDERER = new IdentityHashMap<>();
    private static final StateFaceQuadRenderer DEFAULT_RENDERER = (state, model, face) -> {
        List<BakedQuad> quads = model.getQuads(state, face, 0L);
        if (quads == null || quads.isEmpty()) {
            return null;
        }

        List<PackedBakedQuad> packedQuads = new ArrayList<>(quads.size());
        for (int i = 0, len = quads.size(); i < len; i++) {
            packedQuads.add(new PackedBakedQuad(quads.get(i)));
        }
        return packedQuads;
    };

    public static Map<IBlockState, int[]> STATES_TO_IDS;

    public static void reloadUVs() {
        //note to self: tint index -1 = biome color is ignored, everything else = biome color is used

        ObjIntMap<List<PackedBakedQuad>> distinctQuadsToId = new ObjIntOpenHashMap<>();
        List<List<PackedBakedQuad>> distinctQuadsById = new ArrayList<>();
        Map<IBlockState, int[]> statesToIds = new IdentityHashMap<>();

        List<PackedBakedQuad> missingTextureQuads = new ArrayList<>(1);
        missingTextureQuads.add(new PackedBakedQuad(mc.getTextureMapBlocks().getMissingSprite(), -1));

        BlockModelShapes shapes = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes();
        for (Block block : Block.REGISTRY) {
            /*TextureAtlasSprite t = null;
            if (block == Blocks.WATER) {
                t = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/water_still");
            }*/
            for (IBlockState state : block.getBlockState().getValidStates()) {
                int[] faceIds = new int[EnumFacing.VALUES.length];

                IBakedModel model = shapes.getModelForState(state);
                StateFaceQuadRenderer renderer = STATE_TO_RENDERER.getOrDefault(state, DEFAULT_RENDERER);
                for (EnumFacing face : EnumFacing.VALUES) {
                    List<PackedBakedQuad> quads = PorkUtil.fallbackIfNull(renderer.render(state, model, face), missingTextureQuads);

                    int id = distinctQuadsToId.getOrDefault(quads, -1);
                    if (id < 0) { //allocate new ID
                        distinctQuadsToId.put(quads, id = distinctQuadsToId.size());
                        distinctQuadsById.add(quads);
                    }

                    faceIds[face.getIndex()] = id;
                }

                statesToIds.put(state, faceIds);
            }
        }

        STATES_TO_IDS = statesToIds;

        @SuppressWarnings("deprecation")
        ByteBuf listsBuffer = ByteBufAllocator.DEFAULT.directBuffer().order(ByteOrder.nativeOrder());
        @SuppressWarnings("deprecation")
        ByteBuf quadsBuffer = ByteBufAllocator.DEFAULT.directBuffer().order(ByteOrder.nativeOrder());
        try {
            int quadIndices = 0;
            for (List<PackedBakedQuad> list : distinctQuadsById) {
                listsBuffer.writeInt(quadIndices); //start
                for (PackedBakedQuad quad : list) {
                    quadsBuffer.writeFloat(quad.minU).writeFloat(quad.minV)
                            .writeFloat(quad.maxU).writeFloat(quad.maxV)
                            .writeFloat(quad.tintFactor)
                    .writeFloat(0.0f);
                    quadIndices++;
                }
                listsBuffer.writeInt(quadIndices); //end
            }

            //upload data
            try (ShaderStorageBuffer ssbo = QUAD_LISTS.bind()) {
                glBufferData(GL_SHADER_STORAGE_BUFFER, DirectBufferReuse.wrapByte(listsBuffer.memoryAddress(), listsBuffer.readableBytes()), GL_STATIC_DRAW);
            }
            try (ShaderStorageBuffer ssbo = QUAD_DATA.bind()) {
                glBufferData(GL_SHADER_STORAGE_BUFFER, DirectBufferReuse.wrapByte(quadsBuffer.memoryAddress(), quadsBuffer.readableBytes()), GL_STATIC_DRAW);
            }
        } finally {
            quadsBuffer.release();
            listsBuffer.release();
        }
    }

    @FunctionalInterface
    public interface StateFaceQuadRenderer {
        List<PackedBakedQuad> render(IBlockState state, IBakedModel model, EnumFacing face);
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
}
