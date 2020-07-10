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
import net.daporkchop.fp2.client.render.object.ShaderStorageBuffer;
import net.daporkchop.fp2.util.Constants;
import net.minecraft.block.material.MapColor;
import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

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

    static {
        primary_init();
    }

    public static void primary_init() {
        try (ShaderStorageBuffer globalInfo = GLOBAL_INFO.bind()) {
            glBufferData(GL_SHADER_STORAGE_BUFFER, MAPCOLORS_OFFSET + MAPCOLORS_SIZE, GL_DYNAMIC_DRAW);

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
}
