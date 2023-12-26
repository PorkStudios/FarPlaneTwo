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

package net.daporkchop.fp2.core.mode.api.tile;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.engine.Tile;
import net.daporkchop.fp2.core.util.serialization.variable.IVariableSizeRecyclingCodec;
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.common.annotation.BorrowOwnership;
import net.daporkchop.lib.common.pool.recycler.Recycler;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.reference.cache.Cached;
import net.daporkchop.lib.compression.zstd.Zstd;
import net.daporkchop.lib.compression.zstd.ZstdDeflater;
import net.daporkchop.lib.compression.zstd.ZstdInflater;
import net.daporkchop.lib.unsafe.PCleaner;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.io.IOException;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link ITileSnapshot} whose tile data is compressed when not in use.
 *
 * @author DaPorkchop_
 */
public class CompressedTileSnapshot extends AbstractTileSnapshot {
    protected static final Cached<ZstdDeflater> ZSTD_DEF = Cached.threadLocal(() -> Zstd.PROVIDER.deflater(Zstd.PROVIDER.deflateOptions()), ReferenceStrength.WEAK);
    protected static final Cached<ZstdInflater> ZSTD_INF = Cached.threadLocal(() -> Zstd.PROVIDER.inflater(Zstd.PROVIDER.inflateOptions()), ReferenceStrength.WEAK);

    protected CompressedTileSnapshot(@BorrowOwnership @NonNull TileSnapshot src) {
        super(src.pos(), src.timestamp());

        if (src.data == 0L) { //no data
            this.data = 0L;
        } else { //source snapshot has some data, let's compress it
            int length = _data_length(src.data); //assume this field is aligned

            ByteBuf compressed = ByteBufAllocator.DEFAULT.buffer(Zstd.PROVIDER.compressBound(length));
            try {
                //compress data
                checkState(ZSTD_DEF.get().compress(Unpooled.wrappedBuffer(_data_payload(src.data), length, false), compressed));
                int compressedLength = compressed.readableBytes();

                //allocate buffer space
                this.data = PUnsafe.allocateMemory(DATA_SIZE(compressedLength));
                this.cleaner = PCleaner.cleaner(this, this.data);

                //populate data buffer
                _data_length(this.data, compressedLength);
                compressed.readBytes(DirectBufferHackery.wrapByte(_data_payload(this.data), compressedLength));
            } finally {
                compressed.release();
            }
        }
    }

    @Override
    @SneakyThrows(IOException.class)
    public Tile loadTile(@NonNull Recycler<Tile> recycler, @NonNull IVariableSizeRecyclingCodec<Tile> codec) {
        this.ensureNotReleased();

        if (this.data != 0L) {
            //allocate buffers
            ByteBuf compressed = Unpooled.wrappedBuffer(_data_payload(this.data), _data_length(this.data), false);
            ByteBuf uncompressed = ByteBufAllocator.DEFAULT.buffer(Zstd.PROVIDER.frameContentSize(compressed));
            try {
                //decompress data
                checkState(ZSTD_INF.get().decompress(compressed, uncompressed));

                //initialize tile from decompressed data
                Tile tile = recycler.allocate();
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

        return this.data == 0L;
    }

    @Override
    public ITileSnapshot compressed() {
        this.ensureNotReleased();

        return this; //we're already compressed!
    }

    @Override
    public ITileSnapshot uncompressed() {
        this.ensureNotReleased();

        if (this.data == 0L) { //this tile is empty!
            return TileSnapshot.of(this.pos(), this.timestamp());
        } else {
            //allocate buffers
            ByteBuf compressed = Unpooled.wrappedBuffer(_data_payload(this.data), _data_length(this.data), false);
            ByteBuf uncompressed = ByteBufAllocator.DEFAULT.directBuffer(Zstd.PROVIDER.frameContentSize(compressed));
            try {
                //decompress data
                checkState(ZSTD_INF.get().decompress(compressed, uncompressed));

                //construct an ordinary snapshot from the uncompressed data
                return TileSnapshot.of(this.pos(), this.timestamp(), uncompressed.memoryAddress(), uncompressed.readableBytes());
            } finally {
                uncompressed.release();
                //no need to release compressed buffer, since it's just wrapping a memory address
            }
        }
    }

    @Override
    public DebugStats.TileSnapshot stats() {
        this.ensureNotReleased();

        if (this.data == 0L) { //this tile is empty!
            return DebugStats.TileSnapshot.ZERO;
        } else {
            int compressedLength = _data_length(this.data);
            int uncompressedLength = Zstd.PROVIDER.frameContentSize(Unpooled.wrappedBuffer(_data_payload(this.data), compressedLength, false));

            return DebugStats.TileSnapshot.builder()
                    .allocatedSpace(compressedLength)
                    .totalSpace(compressedLength)
                    .uncompressedSize(uncompressedLength)
                    .build();
        }
    }
}
