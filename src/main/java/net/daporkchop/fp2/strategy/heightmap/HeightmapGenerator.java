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

package net.daporkchop.fp2.strategy.heightmap;

import lombok.NonNull;
import net.daporkchop.fp2.util.threading.CachedBlockAccess;
import net.minecraft.world.WorldServer;

/**
 * Extracts height and color information from a world for use by the heightmap rendering strategy.
 * <p>
 * Once initialized, instances of this class are expected to be safely usable by multiple concurrent threads.
 *
 * @author DaPorkchop_
 */
public interface HeightmapGenerator {
    /**
     * Initializes this instance.
     * <p>
     * An instance is only initialized once. No other methods will be called on this instance until initialization is complete.
     *
     * @param world the world that the heightmap will be generated for
     */
    void init(@NonNull WorldServer world);

    /**
     * Generates a rough estimate of the terrain in the given chunk.
     * <p>
     * This allows implementations to make an optimized, generator-specific estimation of what the terrain would look like once generated, without requiring
     * the actual generation of said chunks.
     * <p>
     * A {@link CachedBlockAccess} parameter is provided only for implementations do not implement this method, and instead choose to have it serve as a
     * proxy to {@link #generateExact(CachedBlockAccess, HeightmapChunk)}.
     * <p>
     * Note that this should only generate a square with side length {@link HeightmapConstants#HEIGHT_VOXELS}, centered on chunk coordinates 0,0. Any outer
     * edges will be automatically filled in with data from the generated area, and later with data from neighboring chunks once it becomes available.
     *
     * @param world the {@link CachedBlockAccess} providing access to block/height data in the world
     * @param chunk the chunk to generate
     */
    void generateRough(@NonNull CachedBlockAccess world, @NonNull HeightmapChunk chunk);

    /**
     * Generates the terrain for the given chunk based on the block data.
     * <p>
     * Note that this should only generate a square with side length {@link HeightmapConstants#HEIGHT_VOXELS}, centered on chunk coordinates 0,0. Any outer
     * edges will be automatically filled in with data from the generated area, and later with data from neighboring chunks once it becomes available.
     *
     * @param world the {@link CachedBlockAccess} providing access to block/height data in the world
     * @param chunk the chunk to generate
     */
    void generateExact(@NonNull CachedBlockAccess world, @NonNull HeightmapChunk chunk);
}
