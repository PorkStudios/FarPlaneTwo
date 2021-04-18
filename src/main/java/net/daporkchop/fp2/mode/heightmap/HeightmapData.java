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

package net.daporkchop.fp2.mode.heightmap;

import net.minecraft.block.state.IBlockState;
import net.minecraft.world.biome.Biome;

import static net.daporkchop.fp2.mode.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * Represents a single data sample contained in a single layer heightmap tile.
 *
 * @author DaPorkchop_
 */
public class HeightmapData {
    public IBlockState state = STATE_AIR;
    public Biome biome;
    public int height_int; //the 32-bit integer part of the layer's height
    public int height_frac; //the 8-bit fractional part of the layer's height
    public int light;
    public int secondaryConnection = DEFAULT_LAYER; //the layer number that this sample should connect to if the neighboring sample on the same layer is absent

    /**
     * Resets this instance.
     *
     * @return this instance
     */
    public HeightmapData reset() {
        this.state = STATE_AIR;
        this.biome = null;
        this.height_int = 0;
        this.height_frac = 0;
        this.light = 0;
        this.secondaryConnection = DEFAULT_LAYER;
        return this;
    }
}
