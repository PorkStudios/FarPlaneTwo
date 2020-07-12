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

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import net.daporkchop.fp2.strategy.common.IFarChunkPos;
import net.daporkchop.fp2.strategy.common.IFarWorld;
import net.daporkchop.fp2.strategy.heightmap.vanilla.VanillaHeightmapGenerator;
import net.daporkchop.fp2.util.threading.CachedBlockAccess;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.primitive.map.LongObjMap;
import net.daporkchop.lib.primitive.map.concurrent.LongObjConcurrentHashMap;
import net.minecraft.world.WorldServer;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * Representation of a world used by the heightmap rendering strategy.
 *
 * @author DaPorkchop_
 */
@Accessors(fluent = true)
public class HeightmapWorld implements IFarWorld {
    @Getter
    protected final WorldServer world;
    @Getter
    protected final HeightmapGenerator generator;

    protected final Path storageRoot;

    protected final LongObjMap<CompletableFuture<HeightmapChunk>> cache = new LongObjConcurrentHashMap<>();

    public HeightmapWorld(@NonNull WorldServer world) {
        this.world = world;
        this.generator = new VanillaHeightmapGenerator();
        this.generator.init(world);

        this.storageRoot = world.getChunkSaveLocation().toPath().resolveSibling("fp2/heightmap");
    }

    protected CompletableFuture<HeightmapChunk> loadChunk(long key) {
        return this.cache.computeIfAbsent(key, l -> CompletableFuture.supplyAsync(() -> {
            int x = BinMath.unpackX(l);
            int z = BinMath.unpackY(l);
            CachedBlockAccess world = ((CachedBlockAccess.Holder) this.world).fp2_cachedBlockAccess();
            HeightmapChunk chunk = new HeightmapChunk(x, z);
            chunk.writeLock().lock();
            try {
                this.generator.generateRough(world, chunk);
            } finally {
                chunk.writeLock().unlock();
            }
            return chunk;
        }, THREAD_POOL));
    }

    @Override
    public HeightmapChunk chunk(@NonNull IFarChunkPos posIn) {
        HeightmapChunkPos pos = (HeightmapChunkPos) posIn;
        return this.loadChunk(BinMath.packXY(pos.x(), pos.z())).join();
    }

    @Override
    public HeightmapChunk getChunkNowOrLoadAsync(@NonNull IFarChunkPos posIn) {
        HeightmapChunkPos pos = (HeightmapChunkPos) posIn;
        return this.loadChunk(BinMath.packXY(pos.x(), pos.z())).getNow(null);
    }

    @Override
    public void blockChanged(int x, int y, int z) {
    }
}
