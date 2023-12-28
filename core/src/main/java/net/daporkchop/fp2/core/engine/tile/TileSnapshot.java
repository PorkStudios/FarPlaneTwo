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

package net.daporkchop.fp2.core.engine.tile;

import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.engine.Tile;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.util.serialization.variable.IVariableSizeRecyclingCodec;
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.binary.stream.DataOut;
import net.daporkchop.lib.common.annotation.BorrowOwnership;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.pool.recycler.Recycler;
import net.daporkchop.lib.unsafe.PCleaner;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.io.IOException;

/**
 * @author DaPorkchop_
 */
public class TileSnapshot extends AbstractTileSnapshot {
    public static TileSnapshot readFromNetwork(@NonNull TilePos pos, @NonNull DataIn in) throws IOException {
        return new TileSnapshot(pos, in);
    }

    public static TileSnapshot of(@NonNull TilePos pos, long timestamp) {
        return new TileSnapshot(pos, timestamp);
    }

    public static TileSnapshot of(@NonNull TilePos pos, long timestamp, @BorrowOwnership long dataAddr, int dataLength) {
        return dataLength < 0
                ? new TileSnapshot(pos, timestamp) //negative length means there's no data
                : new TileSnapshot(pos, timestamp, dataAddr, dataLength);
    }

    protected TileSnapshot(@NonNull TilePos pos, @NonNull DataIn in) throws IOException {
        super(pos, in.readVarLongZigZag());

        int length = in.readVarIntZigZag();
        if (length < 0) { //no data!
            this.data = 0L;
        } else if (length == 0) { //special case for zero-length buffer which requires zero allocations
            this.data = ZERO_LENGTH_DATA;
        } else {
            //allocate buffer space
            this.data = PUnsafe.allocateMemory(DATA_SIZE(length));
            this.cleaner = PCleaner.cleaner(this, this.data);

            //read data
            _data_length(this.data, length);
            in.readFully(DirectBufferHackery.wrapByte(_data_payload(this.data), length));
        }
    }

    protected TileSnapshot(@NonNull TilePos pos, long timestamp) {
        super(pos, timestamp);

        this.data = 0L; //there is no data
    }

    protected TileSnapshot(@NonNull TilePos pos, long timestamp, @BorrowOwnership long dataAddr, @NotNegative int dataLength) {
        super(pos, timestamp);

        if (dataLength == 0) { //special case for zero-length buffer which requires zero allocations
            this.data = ZERO_LENGTH_DATA;
        } else {
            //allocate buffer space
            this.data = PUnsafe.allocateMemory(DATA_SIZE(dataLength));
            this.cleaner = PCleaner.cleaner(this, this.data);

            //populate data buffer
            _data_length(this.data, dataLength);
            PUnsafe.copyMemory(dataAddr, _data_payload(this.data), dataLength);
        }
    }

    public void writeForNetwork(@NonNull DataOut out) throws IOException {
        this.ensureNotReleased();

        out.writeVarLongZigZag(this.timestamp);

        if (this.data == 0L) { //no data!
            out.writeVarIntZigZag(-1);
        } else { //tile data is present, write it
            int length = _data_length(this.data);

            out.writeVarIntZigZag(length);
            if (length != 0) {
                out.write(DirectBufferHackery.wrapByte(_data_payload(this.data), length));
            }
        }
    }

    @Override
    @SneakyThrows(IOException.class)
    public Tile loadTile(@NonNull Recycler<Tile> recycler, @NonNull IVariableSizeRecyclingCodec<Tile> codec) {
        this.ensureNotReleased();

        if (this.data != 0L) {
            Tile tile = recycler.allocate();

            int length = _data_length(this.data);
            try (DataIn in = DataIn.wrap(DirectBufferHackery.wrapByte(_data_payload(this.data), length))) {
                codec.load(tile, in);
            }
            return tile;
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

        return new CompressedTileSnapshot(this);
    }

    @Override
    public ITileSnapshot uncompressed() {
        this.ensureNotReleased();

        return this; //we're already uncompressed!
    }

    @Override
    public DebugStats.TileSnapshot stats() {
        this.ensureNotReleased();

        if (this.data == 0L) { //this tile is empty!
            return DebugStats.TileSnapshot.ZERO;
        } else {
            int length = _data_length(this.data);

            return DebugStats.TileSnapshot.builder()
                    .allocatedSpace(length)
                    .totalSpace(length)
                    .uncompressedSize(length)
                    .build();
        }
    }
}
