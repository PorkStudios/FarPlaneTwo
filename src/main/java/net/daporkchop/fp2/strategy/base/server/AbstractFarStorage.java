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

package net.daporkchop.fp2.strategy.base.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import lombok.NonNull;
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.strategy.RenderMode;
import net.daporkchop.fp2.strategy.common.IFarPiece;
import net.daporkchop.fp2.strategy.common.IFarPos;
import net.daporkchop.fp2.strategy.common.server.IFarStorage;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiece;
import net.daporkchop.ldbjni.LevelDB;
import net.daporkchop.ldbjni.direct.DirectDB;
import net.daporkchop.lib.common.misc.file.PFiles;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.compression.zstd.Zstd;
import net.daporkchop.lib.primitive.map.IntObjMap;
import net.daporkchop.lib.primitive.map.concurrent.IntObjConcurrentHashMap;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.world.WorldServer;

import java.io.File;
import java.io.IOException;
import java.util.function.IntFunction;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractFarStorage<POS extends IFarPos, P extends IFarPiece<POS>> implements IFarStorage<POS, P> {
    protected final IntObjMap<DirectDB> dbs = new IntObjConcurrentHashMap<>();
    protected final File storageRoot;

    protected final IntFunction<DirectDB> dbOpenFunction;

    public AbstractFarStorage(@NonNull WorldServer world) {
        this.storageRoot = new File(world.getChunkSaveLocation(), "fp2/" + this.mode().name().toLowerCase());
        PFiles.ensureDirectoryExists(this.storageRoot);

        this.dbOpenFunction = i -> {
            try {
                return LevelDB.PROVIDER.open(new File(this.storageRoot, String.valueOf(i)), FP2Config.storage.leveldb.use());
            } catch (IOException e) {
                LOGGER.error(PStrings.fastFormat("Unable to open DB in %s at level %d", this.storageRoot, i), e);
                PUnsafe.throwException(e);
                throw new RuntimeException(e); //unreachable
            }
        };
    }

    @Override
    public P load(@NonNull POS pos) {
        if (FP2Config.debug.disableRead || FP2Config.debug.disablePersistence) {
            return null;
        }

        try {
            //read from db
            ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer();
            ByteBuf packed;
            try {
                pos.writePosNoLevel(buf);
                if ((packed = this.dbs.computeIfAbsent(pos.level(), this.dbOpenFunction).getZeroCopy(buf)) == null) {
                    return null;
                }
            } finally {
                buf.release();
            }

            //unpack
            boolean release = true;
            try {
                if (packed.readInt() == this.mode().storageVersion) {
                    int uncompressedSize = packed.readInt();
                    buf = PooledByteBufAllocator.DEFAULT.buffer(uncompressedSize, uncompressedSize);
                    try {
                        checkState(ZSTD_INF.get().decompress(packed, buf));
                        packed.release();
                        release = false;
                        return uncheckedCast(this.mode().readPiece(buf));
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
        } catch (Exception e) {
            LOGGER.error(PStrings.fastFormat("Unable to load tile %s from DB at %s", pos, this.storageRoot), e);
            return null;
        }
    }

    @Override
    public void store(@NonNull POS pos, @NonNull P piece) {
        if (FP2Config.debug.disableWrite || FP2Config.debug.disablePersistence) {
            return;
        }

        ByteBuf packed = null;
        try {
            //pack piece
            ByteBuf buf = PooledByteBufAllocator.DEFAULT.ioBuffer();
            try {
                piece.readLock().lock();
                try {
                    piece.writePiece(buf);
                } finally {
                    piece.readLock().unlock();
                }

                packed = PooledByteBufAllocator.DEFAULT.ioBuffer(8 + Zstd.PROVIDER.compressBound(buf.readableBytes()))
                        .writeInt(RenderMode.HEIGHTMAP.storageVersion)
                        .writeInt(buf.readableBytes());
                checkState(ZSTD_DEF.get().compress(buf, packed));
            } finally {
                buf.release();
            }

            //write to db
            buf = PooledByteBufAllocator.DEFAULT.buffer();
            try {
                pos.writePosNoLevel(buf);
                this.dbs.computeIfAbsent(pos.level(), this.dbOpenFunction).put(buf, packed);
            } finally {
                buf.release();
            }
        } catch (Exception e) {
            LOGGER.error(PStrings.fastFormat("Unable to save tile %s to DB at %s", pos, this.storageRoot), e);
        } finally {
            if (packed != null) {
                packed.release();
            }
        }
    }

    @Override
    public void close() throws IOException {
        IOException root = null;
        for (DirectDB db : this.dbs.values()) {
            try {
                db.close();
            } catch (IOException e) {
                if (root == null) {
                    root = new IOException("Unable to close all DBs in " + this.storageRoot);
                }
                root.addSuppressed(e);
            }
        }
        if (root != null) {
            throw root;
        }
    }
}
