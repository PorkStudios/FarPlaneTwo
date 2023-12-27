/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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
 */

package net.daporkchop.fp2.core.engine.server.gen;

import lombok.NonNull;
import net.daporkchop.fp2.core.engine.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.common.server.gen.AbstractFarGenerator;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.reference.cache.Cached;

import static net.daporkchop.fp2.core.engine.EngineConstants.*;
import static net.daporkchop.fp2.core.util.math.MathUtil.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractVoxelGenerator extends AbstractFarGenerator {
    public static final int CACHE_MIN = -1;
    public static final int CACHE_MAX = T_VOXELS + 1;
    public static final int CACHE_SIZE = CACHE_MAX - CACHE_MIN;

    protected static final int CACHE_INDEX_ADD_000 = cacheIndex(CACHE_MIN + 0, CACHE_MIN + 0, CACHE_MIN + 0);
    protected static final int CACHE_INDEX_ADD_001 = cacheIndex(CACHE_MIN + 0, CACHE_MIN + 0, CACHE_MIN + 1);
    protected static final int CACHE_INDEX_ADD_010 = cacheIndex(CACHE_MIN + 0, CACHE_MIN + 1, CACHE_MIN + 0);
    protected static final int CACHE_INDEX_ADD_011 = cacheIndex(CACHE_MIN + 0, CACHE_MIN + 1, CACHE_MIN + 1);
    protected static final int CACHE_INDEX_ADD_100 = cacheIndex(CACHE_MIN + 1, CACHE_MIN + 0, CACHE_MIN + 0);
    protected static final int CACHE_INDEX_ADD_101 = cacheIndex(CACHE_MIN + 1, CACHE_MIN + 0, CACHE_MIN + 1);
    protected static final int CACHE_INDEX_ADD_110 = cacheIndex(CACHE_MIN + 1, CACHE_MIN + 1, CACHE_MIN + 0);
    protected static final int CACHE_INDEX_ADD_111 = cacheIndex(CACHE_MIN + 1, CACHE_MIN + 1, CACHE_MIN + 1);

    protected static final int[] CACHE_INDEX_ADD = {
            CACHE_INDEX_ADD_000, CACHE_INDEX_ADD_001,
            CACHE_INDEX_ADD_010, CACHE_INDEX_ADD_011,
            CACHE_INDEX_ADD_100, CACHE_INDEX_ADD_101,
            CACHE_INDEX_ADD_110, CACHE_INDEX_ADD_111
    };

    protected static int cacheIndex(int x, int y, int z) {
        assert x >= CACHE_MIN && x < CACHE_MAX : "x=" + x;
        assert y >= CACHE_MIN && y < CACHE_MAX : "y=" + y;
        assert z >= CACHE_MIN && z < CACHE_MAX : "z=" + z;

        return ((x - CACHE_MIN) * CACHE_SIZE + y - CACHE_MIN) * CACHE_SIZE + z - CACHE_MIN;
    }

    protected final Cached<byte[]> typeMapCache = Cached.threadLocal(() -> new byte[cb(CACHE_SIZE)], ReferenceStrength.WEAK);

    public AbstractVoxelGenerator(@NonNull IFarLevelServer world, @NonNull IFarTileProvider provider) {
        super(world, provider);
    }
}
