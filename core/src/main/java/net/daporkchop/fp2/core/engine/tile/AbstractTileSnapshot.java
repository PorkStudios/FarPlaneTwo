/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.common.system.PlatformInfo;
import net.daporkchop.lib.common.util.exception.AlreadyReleasedException;
import net.daporkchop.lib.unsafe.PCleaner;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.ByteBuffer;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Base implementation of {@link ITileSnapshot}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public abstract class AbstractTileSnapshot extends AbstractRefCounted implements ITileSnapshot {
    /*
     * struct data {
     *     int length;
     *     byte payload[...];
     * };
     */

    private static final long DATA_LENGTH_OFFSET = 0L;
    private static final long DATA_LENGTH_SIZE = INT_SIZE;

    private static final long DATA_PAYLOAD_OFFSET = DATA_LENGTH_OFFSET + DATA_LENGTH_SIZE;

    protected static long DATA_SIZE(@NotNegative int length) {
        return DATA_PAYLOAD_OFFSET + notNegative(length, "length");
    }

    protected static int _data_length(long data) {
        return PUnsafe.getInt(data + DATA_LENGTH_OFFSET); //assume this field is aligned
    }

    protected static void _data_length(long data, @NotNegative int length) {
        PUnsafe.putInt(data + DATA_LENGTH_OFFSET, length); //assume this field is aligned
    }

    protected static long _data_payload(long data) {
        return data + DATA_PAYLOAD_OFFSET;
    }

    //dummy buffer to allow the memory to be released automatically if the class is unloaded
    private static final ByteBuffer ZERO_LENGTH_DATA_BUFFER = ByteBuffer.allocateDirect(toInt(DATA_SIZE(0))).order(PlatformInfo.BYTE_ORDER);
    protected static final long ZERO_LENGTH_DATA = PUnsafe.pork_directBufferAddress(ZERO_LENGTH_DATA_BUFFER);

    static {
        //initialize ZERO_LENGTH_DATA
        _data_length(ZERO_LENGTH_DATA, 0);
    }

    @NonNull
    protected final TilePos pos;
    protected final long timestamp;

    @Getter(AccessLevel.NONE)
    protected long data;
    @Getter(AccessLevel.NONE)
    protected PCleaner cleaner;

    @Override
    public ITileSnapshot retain() throws AlreadyReleasedException {
        super.retain();
        return this;
    }

    @Override
    protected void doRelease() {
        if (this.cleaner != null) {
            this.cleaner.clean();
            this.cleaner = null; //help gc
        }
    }

    @Override
    public long dataSize() {
        this.ensureNotReleased();

        if (this.data == 0L) {
            return 0L;
        } else {
            return _data_length(this.data);
        }
    }
}
