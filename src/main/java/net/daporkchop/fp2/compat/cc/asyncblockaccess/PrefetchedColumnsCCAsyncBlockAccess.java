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

package net.daporkchop.fp2.compat.cc.asyncblockaccess;

import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import lombok.NonNull;
import net.daporkchop.fp2.util.threading.asyncblockaccess.AbstractPrefetchedAsyncBlockAccess;
import net.daporkchop.fp2.util.threading.asyncblockaccess.IAsyncBlockAccess;
import net.daporkchop.lib.primitive.map.LongObjMap;
import net.daporkchop.lib.primitive.map.open.LongObjOpenHashMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;

import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link IAsyncBlockAccess} returned by {@link IAsyncBlockAccess#prefetch(Stream)}.
 *
 * @author DaPorkchop_
 */
public class PrefetchedColumnsCCAsyncBlockAccess extends AbstractPrefetchedAsyncBlockAccess<CCAsyncBlockAccessImpl> {
    protected final LongObjMap<IColumn> columns = new LongObjOpenHashMap<>();

    public PrefetchedColumnsCCAsyncBlockAccess(CCAsyncBlockAccessImpl parent, WorldServer world, boolean allowGeneration, @NonNull Stream<IColumn> columns) {
        super(parent, world, allowGeneration);

        columns.forEach(column -> {
            long key = ChunkPos.asLong(column.getX(), column.getZ());
            checkArg(this.columns.putIfAbsent(key, column) == null, "duplicate column at (%d, %d)", column.getX(), column.getZ());
        });
    }

    @Override
    public int getTopBlockY(int blockX, int blockZ) {
        IColumn column = this.columns.get(ChunkPos.asLong(blockX >> 4, blockZ >> 4));
        if (column != null) {
            return column.getOpacityIndex().getTopBlockY(blockX & 0xF, blockZ & 0xF);
        }
        return super.getTopBlockY(blockX, blockZ);
    }

    @Override
    public int getTopBlockYBelow(int blockX, int blockY, int blockZ) {
        IColumn chunk = this.columns.get(ChunkPos.asLong(blockX >> 4, blockZ >> 4));
        if (chunk != null) {
            return chunk.getOpacityIndex().getTopBlockYBelow(blockX & 0xF, blockZ & 0xF, blockY);
        }
        return super.getTopBlockYBelow(blockX, blockY, blockZ);
    }

    //leave all other implementations blank because IColumn can't access them directly
}
