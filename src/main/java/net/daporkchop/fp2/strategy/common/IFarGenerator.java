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

package net.daporkchop.fp2.strategy.common;

import lombok.NonNull;
import net.daporkchop.fp2.util.threading.CachedBlockAccess;
import net.minecraft.world.WorldServer;

/**
 * Extracts height and color information from a world for use by a rendering mode.
 * <p>
 * Once initialized, instances of this class are expected to be safely usable by multiple concurrent threads.
 *
 * @author DaPorkchop_
 */
public interface IFarGenerator<POS extends IFarPos, P extends IFarPiece<POS>> {
    /**
     * Initializes this instance.
     * <p>
     * An instance is only initialized once. No other methods will be called on this instance until initialization is complete.
     *
     * @param world the world that the heightmap will be generated for
     */
    void init(@NonNull WorldServer world);

    /**
     * Generates a rough estimate of the terrain in the given piece.
     *
     * @param world the {@link CachedBlockAccess} providing access to block/height data in the world
     * @param piece the piece to generate
     */
    void generate(@NonNull CachedBlockAccess world, @NonNull P piece);
}
