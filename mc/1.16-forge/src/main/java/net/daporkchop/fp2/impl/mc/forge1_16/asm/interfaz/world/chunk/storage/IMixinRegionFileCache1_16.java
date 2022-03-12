/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.world.chunk.storage;

import net.daporkchop.lib.math.vector.Vec2i;
import net.minecraft.util.math.ChunkPos;

import java.io.IOException;
import java.util.List;

/**
 * @author DaPorkchop_
 */
public interface IMixinRegionFileCache1_16 {
    /**
     * Checks whether the chunk at the given position exists.
     *
     * @param pos the chunk position
     * @return whether the chunk at the given position exists
     */
    boolean fp2_RegionFileCache_hasChunk(ChunkPos pos) throws IOException;

    /**
     * Gets a {@link List} containing the positions of all regions that exist.
     *
     * @return a {@link List} containing the positions of all regions that exist
     */
    List<Vec2i> fp2_RegionFileCache_listRegions() throws IOException;

    /**
     * Gets a {@link List} containing the positions of all chunks that exist in the given region.
     *
     * @param regionPos the region's position
     * @return a {@link List} containing the positions of all chunks that exist in the given region
     */
    List<ChunkPos> fp2_RegionFileCache_listChunksInRegion(Vec2i regionPos) throws IOException;
}
