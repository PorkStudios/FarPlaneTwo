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

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.client.gl.object.ShaderStorageBuffer;
import net.daporkchop.fp2.util.Constants;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.biome.Biome;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * Global terrain info used by terrain rendering shaders.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class GlobalInfo {
    public static final ShaderStorageBuffer GLOBAL_INFO = new ShaderStorageBuffer();

    public static final long BIOME_OFFSET = 0L;
    public static final int BIOME_SIZE = 256 * 2 * 4;

    public static final long WATERCOLOR_OFFSET = BIOME_OFFSET + BIOME_SIZE;
    public static final int WATERCOLOR_SIZE = 256 * 4;

    public static final long COLORMAP_GRASS_OFFSET = WATERCOLOR_OFFSET + WATERCOLOR_SIZE;
    public static final int COLORMAP_GRASS_SIZE = 65536 * 4;

    public static final long COLORMAP_FOLIAGE_OFFSET = COLORMAP_GRASS_OFFSET + COLORMAP_GRASS_SIZE;
    public static final int COLORMAP_FOLIAGE_SIZE = 65536 * 4;

    public static final long MAPCOLORS_OFFSET = COLORMAP_FOLIAGE_OFFSET + COLORMAP_FOLIAGE_SIZE;
    public static final int MAPCOLORS_SIZE = 64 * 4;

    public static final long UVS_OFFSET = MAPCOLORS_OFFSET + MAPCOLORS_SIZE;
    public static final int UVS_SIZE = 4096 * 16 * 6 * 2 * VEC2_SIZE; //4096 ids, 16 meta values, 6 faces, 2 vec2s

    public static final long TOTAL_SIZE = UVS_OFFSET + UVS_SIZE;

    static {
        init();
    }

    public static void init() {
        try (ShaderStorageBuffer globalInfo = GLOBAL_INFO.bind()) {
            glBufferData(GL_SHADER_STORAGE_BUFFER, TOTAL_SIZE, GL_STATIC_DRAW);

            {
                FloatBuffer buffer = Constants.createFloatBuffer(BIOME_SIZE >> 2);
                for (int i = 0; i < 256; i++) {
                    Biome biome = Biome.getBiome(i, Biomes.PLAINS);
                    buffer.put(biome.getDefaultTemperature()).put(biome.getRainfall());
                }
                buffer.clear();
                glBufferSubData(GL_SHADER_STORAGE_BUFFER, BIOME_OFFSET, buffer);
            }
            {
                IntBuffer buffer = Constants.createIntBuffer(WATERCOLOR_SIZE >> 2);
                for (int i = 0; i < 256; i++) {
                    Biome biome = Biome.getBiome(i, Biomes.PLAINS);
                    buffer.put(0xFF000000 | biome.getWaterColor());
                }
                buffer.clear();
                glBufferSubData(GL_SHADER_STORAGE_BUFFER, WATERCOLOR_OFFSET, buffer);
            }
            {
                IntBuffer buffer = Constants.createIntBuffer(MAPCOLORS_SIZE >> 2);
                for (int i = 0; i < 64; i++) {
                    int color = MapColor.COLORS[i] != null ? MapColor.COLORS[i].colorValue : 0xFF00FF;
                    buffer.put(0xFF000000 | color);
                }
                buffer.clear();
                glBufferSubData(GL_SHADER_STORAGE_BUFFER, MAPCOLORS_OFFSET, buffer);
            }
        }
    }

    public static void reloadUVs() {
        FloatBuffer buffer = Constants.createFloatBuffer(UVS_SIZE >> 2);
        BlockModelShapes shapes = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes();
        for (Block block : Block.REGISTRY) {
            TextureAtlasSprite t = null;
            if (block == Blocks.WATER) {
                t = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/water_still");
            }
            for (IBlockState state : block.getBlockState().getValidStates()) {
                int id = Block.getStateId(state);
                //TextureAtlasSprite texture = shapes.getTexture(state);
                IBakedModel model = shapes.getModelForState(state);
                for (EnumFacing face : EnumFacing.VALUES) {
                    List<BakedQuad> quads = model.getQuads(state, face, 0L);
                    if (quads != null && !quads.isEmpty()) {
                        TextureAtlasSprite texture = t != null ? t : quads.get(0).getSprite();
                        buffer.position((id * 6 + face.getIndex()) * 2 * VEC2_ELEMENTS);
                        buffer.put(texture.getMinU()).put(texture.getMinV())
                                .put(texture.getMaxU()).put(texture.getMaxV());
                    }
                }
            }
        }
        buffer.clear();
        try (ShaderStorageBuffer globalInfo = GLOBAL_INFO.bind()) {
            glBufferSubData(GL_SHADER_STORAGE_BUFFER, UVS_OFFSET, buffer);
        }
    }
}
