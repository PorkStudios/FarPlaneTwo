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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import lombok.NonNull;
import net.daporkchop.fp2.FP2;
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.strategy.RenderMode;
import net.daporkchop.fp2.strategy.common.IFarStorage;
import net.daporkchop.ldbjni.LevelDB;
import net.daporkchop.ldbjni.direct.DirectDB;
import net.daporkchop.lib.common.function.io.IOBiFunction;
import net.daporkchop.lib.common.function.io.IOFunction;
import net.daporkchop.lib.common.function.io.IOPredicate;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.compression.zstd.Zstd;
import net.daporkchop.lib.primitive.map.IntObjMap;
import net.daporkchop.lib.primitive.map.concurrent.IntObjConcurrentHashMap;
import net.minecraft.world.WorldServer;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import static net.daporkchop.fp2.strategy.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class HeightmapStorage implements IFarStorage<HeightmapPos> {
    protected final IntObjMap<DirectDB> dbs = new IntObjConcurrentHashMap<>();
    protected final File storageRoot;

    protected final IntFunction<DirectDB> dbOpenFunction;

    public HeightmapStorage(@NonNull WorldServer world) {
        this.storageRoot = new File(world.getChunkSaveLocation(), "fp2/" + RenderMode.HEIGHTMAP.name().toLowerCase());

        checkState(LevelDB.PROVIDER.isNative());

        this.dbOpenFunction = i -> {
            try {
                return LevelDB.PROVIDER.open(new File(this.storageRoot, String.valueOf(i)), FP2Config.storage.leveldbOptions);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    public ByteBuf load(@NonNull HeightmapPos posIn) {
        if (FP2Config.debug.disableRead || FP2Config.debug.disablePersistence) {
            return null;
        }

        ByteBuf buf = PooledByteBufAllocator.DEFAULT.ioBuffer();
        try {
            buf.writeInt(posIn.x).writeInt(posIn.z);
            return this.dbs.computeIfAbsent(posIn.level, this.dbOpenFunction).get(buf);
        } finally {
            buf.release();
        }
    }

    @Override
    public void store(@NonNull HeightmapPos posIn, @NonNull ByteBuf data) {
        if (FP2Config.debug.disableWrite || FP2Config.debug.disablePersistence) {
            return;
        }

        ByteBuf buf = PooledByteBufAllocator.DEFAULT.ioBuffer();
        try {
            buf.writeInt(posIn.x).writeInt(posIn.z);
            this.dbs.computeIfAbsent(posIn.level, this.dbOpenFunction).put(buf, data);
        } finally {
            buf.release();
        }
    }

    @Override
    public void close() throws IOException {
        for (DirectDB db : this.dbs.values())   {
            db.close();
        }
    }

    public HeightmapPiece unpack(@NonNull ByteBuf packed, boolean release) {
        try {
            if (packed.readableBytes() >= 8 && packed.readInt() == HEIGHTMAP_STORAGE_VERSION) {
                int uncompressedSize = packed.readInt();
                ByteBuf buf = PooledByteBufAllocator.DEFAULT.ioBuffer(uncompressedSize, uncompressedSize);
                try {
                    checkState(INFLATER_CACHE.get().decompress(packed, buf));
                    if (release) {
                        packed.release();
                        release = false;
                    }
                    return new HeightmapPiece(buf);
                } finally {
                    buf.release();
                }
            }
            return null;
        } finally {
            if (release) {
                packed.release();
            }
        }
    }

    public HeightmapPiece loadAndUnpack(@NonNull HeightmapPos pos) {
        ByteBuf packed = this.load(pos);
        return packed != null ? this.unpack(packed, true) : null;
    }

    public ByteBuf pack(@NonNull HeightmapPiece piece) {
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.ioBuffer();
        try {
            piece.writePiece(buf);

            ByteBuf packed = PooledByteBufAllocator.DEFAULT.ioBuffer(8 + Zstd.PROVIDER.compressBound(buf.readableBytes()))
                    .writeInt(HEIGHTMAP_STORAGE_VERSION)
                    .writeInt(buf.readableBytes());
            checkState(DEFLATER_CACHE.get().compress(buf, packed));

            return packed;
        } finally {
            buf.release();
        }
    }
}
