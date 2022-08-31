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
 */

package net.daporkchop.fp2.core.mode.api.tile;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.lib.common.pool.recycler.Recycler;
import net.daporkchop.fp2.core.util.serialization.variable.IVariableSizeRecyclingCodec;
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.reference.cache.Cached;
import net.daporkchop.lib.compression.zstd.Zstd;
import net.daporkchop.lib.compression.zstd.ZstdDeflater;
import net.daporkchop.lib.compression.zstd.ZstdInflater;

import java.io.IOException;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link ITileSnapshot} whose tile data is compressed when not in use.
 *
 * @author DaPorkchop_
 */
@Getter
public class CompressedTileSnapshot<POS extends IFarPos, T extends IFarTile> extends AbstractTileSnapshot<POS, T> {
    protected static final Cached<ZstdDeflater> ZSTD_DEF = Cached.threadLocal(() -> Zstd.PROVIDER.deflater(Zstd.PROVIDER.deflateOptions()), ReferenceStrength.WEAK);
    protected static final Cached<ZstdInflater> ZSTD_INF = Cached.threadLocal(() -> Zstd.PROVIDER.inflater(Zstd.PROVIDER.inflateOptions()), ReferenceStrength.WEAK);

    @NonNull
    protected final POS pos;
    protected final long timestamp;

    @Getter(AccessLevel.NONE)
    protected final byte[] data;

    public CompressedTileSnapshot(@NonNull TileSnapshot<POS, T> src) {
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
    protected void doRelease() {
        //no-op
    }

    @Override
    @SneakyThrows(IOException.class)
    public T loadTile(@NonNull Recycler<T> recycler, @NonNull IVariableSizeRecyclingCodec<T> codec) {
        this.ensureNotReleased();

        if (this.data != null) {
            //allocate buffers
            ByteBuf compressed = Unpooled.wrappedBuffer(this.data);
            ByteBuf uncompressed = ByteBufAllocator.DEFAULT.buffer(Zstd.PROVIDER.frameContentSize(compressed));
            try {
                //decompress data
                checkState(ZSTD_INF.get().decompress(compressed, uncompressed));

                //initialize tile from decompressed data
                T tile = recycler.allocate();
                try (DataIn in = DataIn.wrapView(uncompressed)) {
                    codec.load(tile, in);
                }
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
        this.ensureNotReleased();

        return this.data == null;
    }

    @Override
    public ITileSnapshot<POS, T> compressed() {
        this.ensureNotReleased();

        return this; //we're already compressed!
    }

    @Override
    public ITileSnapshot<POS, T> uncompressed() {
        this.ensureNotReleased();

        byte[] uncompressedData = null;
        if (this.data != null) {
            //allocate buffer
            uncompressedData = new byte[Zstd.PROVIDER.frameContentSize(Unpooled.wrappedBuffer(this.data))];

            //decompress data
            checkState(ZSTD_INF.get().decompress(Unpooled.wrappedBuffer(this.data), Unpooled.wrappedBuffer(uncompressedData).clear()));
        }

        return new TileSnapshot<>(this.pos, this.timestamp, uncompressedData);
    }

    @Override
    public DebugStats.TileSnapshot stats() {
        this.ensureNotReleased();

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
