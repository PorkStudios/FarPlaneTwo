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

package net.daporkchop.fp2.mode.common.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.Recycler;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.mode.RenderMode;
import net.daporkchop.fp2.mode.api.Compressed;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.api.piece.IFarPieceBuilder;
import net.daporkchop.fp2.mode.api.server.IFarStorage;
import net.daporkchop.fp2.util.IReusablePersistent;
import net.daporkchop.ldbjni.LevelDB;
import net.daporkchop.ldbjni.direct.DirectDB;
import net.daporkchop.lib.common.misc.file.PFiles;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.primitive.map.IntObjMap;
import net.daporkchop.lib.primitive.map.concurrent.IntObjConcurrentHashMap;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.world.WorldServer;

import java.io.File;
import java.io.IOException;
import java.util.function.IntFunction;

import static net.daporkchop.fp2.debug.FP2Debug.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class FarStorage<POS extends IFarPos, V extends IReusablePersistent> implements IFarStorage<POS, V> {
    protected final IntObjMap<DirectDB> dbs = new IntObjConcurrentHashMap<>();
    protected final File storageRoot;
    @Getter
    protected final RenderMode mode;

    protected final IntFunction<DirectDB> dbOpenFunction;

    public FarStorage(@NonNull WorldServer world, @NonNull RenderMode mode, @NonNull String type) {
        this.mode = mode;

        this.storageRoot = new File(world.getChunkSaveLocation(), "fp2/" + mode.name().toLowerCase() + '/' + type);
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
    public Compressed<POS, V> load(@NonNull POS pos) {
        if (FP2_DEBUG && (FP2Config.debug.disableRead || FP2Config.debug.disablePersistence)) {
            return null;
        }

        try {
            //read from db
            ByteBuf key = ByteBufAllocator.DEFAULT.buffer();
            ByteBuf packed;
            try {
                pos.writePosNoLevel(key);
                if ((packed = this.dbs.computeIfAbsent(pos.level(), this.dbOpenFunction).getZeroCopy(key)) == null) {
                    return null;
                }
            } finally {
                key.release();
            }

            //unpack
            boolean release = true;
            try {
                if (readVarInt(packed) == this.mode().storageVersion()) {
                    return new Compressed<>(pos, packed);
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
    public void store(@NonNull POS pos, @NonNull Compressed<POS, V> piece) {
        if (FP2_DEBUG && (FP2Config.debug.disableWrite || FP2Config.debug.disablePersistence)) {
            return;
        }

        ByteBuf packed = ByteBufAllocator.DEFAULT.buffer();
        try {
            //write piece
            writeVarInt(packed, this.mode().storageVersion()); //prefix with version
            piece.write(packed);

            //write to db
            ByteBuf key = ByteBufAllocator.DEFAULT.buffer();
            try {
                pos.writePosNoLevel(key);
                this.dbs.computeIfAbsent(pos.level(), this.dbOpenFunction).put(key, packed);
            } finally {
                key.release();
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
