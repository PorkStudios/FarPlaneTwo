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

package net.daporkchop.fp2.mode.api.tile;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.debug.util.DebugStats;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.fp2.util.annotation.DebugOnly;
import net.daporkchop.lib.compression.zstd.Zstd;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link ITileSnapshot} whose tile data is compressed when not in use.
 *
 * @author DaPorkchop_
 */
@Getter
public class CompressedTileSnapshot<POS extends IFarPos, T extends IFarTile> implements ITileSnapshot<POS, T> {
    @NonNull
    protected final POS pos;
    protected final long timestamp;

    @Getter(AccessLevel.NONE)
    protected final byte[] data;

    protected CompressedTileSnapshot(@NonNull TileSnapshot<POS, T> src) {
        this.pos = src.pos();
        this.timestamp = src.timestamp();

        if (src.data == null) { //no data
            this.data = null;
        } else { //source snapshot has some data, let's compress it
            ByteBuf compressed = ByteBufAllocator.DEFAULT.buffer(Zstd.PROVIDER.compressBound(src.data.length));
            try {
                //compress data
                checkState(ZSTD_DEF.get().compress(Unpooled.wrappedBuffer(src.data), compressed));

                //copy compressed data into a byte array
                this.data = new byte[compressed.readableBytes()];
                compressed.readBytes(this.data);
            } finally {
                compressed.release();
            }
        }
    }

    @Override
    public T loadTile(@NonNull SimpleRecycler<T> recycler) {
        if (this.data != null) {
            //allocate buffers
            ByteBuf compressed = Unpooled.wrappedBuffer(this.data);
            ByteBuf uncompressed = ByteBufAllocator.DEFAULT.buffer(Zstd.PROVIDER.frameContentSize(compressed));
            try {
                //decompress data
                checkState(ZSTD_INF.get().decompress(compressed, uncompressed));

                //initialize tile from decompressed data
                T tile = recycler.allocate();
                tile.read(uncompressed);
                return tile;
            } finally {
                uncompressed.release();
                //no need to release compressed buffer, since it's just wrapping a byte[]
            }
        } else {
            return null;
        }
    }

    @Override
    public boolean isEmpty() {
        return this.data == null;
    }

    @Override
    public ITileSnapshot<POS, T> compressed() {
        return this; //we're already compressed!
    }

    @Override
    public ITileSnapshot<POS, T> uncompressed() {
        byte[] uncompressedData = null;
        if (this.data != null) {
            //allocate buffer
            uncompressedData = new byte[Zstd.PROVIDER.frameContentSize(Unpooled.wrappedBuffer(this.data))];

            //decompress data
            checkState(ZSTD_INF.get().decompress(Unpooled.wrappedBuffer(this.data), Unpooled.wrappedBuffer(uncompressedData).clear()));
        }

        return new TileSnapshot<>(this.pos, this.timestamp, uncompressedData);
    }

    @DebugOnly
    @Override
    public DebugStats.TileSnapshot stats() {
        if (this.data == null) { //this tile is empty!
            return DebugStats.TileSnapshot.ZERO;
        } else {
            return DebugStats.TileSnapshot.builder()
                    .allocatedSpace(this.data.length)
                    .totalSpace(this.data.length)
                    .uncompressedSize(Zstd.PROVIDER.frameContentSize(Unpooled.wrappedBuffer(this.data)))
                    .build();
        }
    }
}
