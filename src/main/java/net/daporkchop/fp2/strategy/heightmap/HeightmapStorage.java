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
import net.daporkchop.lib.common.function.io.IOBiFunction;
import net.daporkchop.lib.common.function.io.IOFunction;
import net.daporkchop.lib.common.function.io.IOPredicate;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.compression.zstd.Zstd;
import net.minecraft.world.WorldServer;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static net.daporkchop.fp2.strategy.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class HeightmapStorage implements IFarStorage<HeightmapPos> {
    protected final Map<HeightmapPos, Object> effectivelyAMutex = new ConcurrentHashMap<>(FP2Config.generationThreads << 8); //really big to avoid contention
    protected final Path storageRoot;

    protected final ThreadLocal<ByteBuf> loadCache = new ThreadLocal<>();

    public HeightmapStorage(@NonNull WorldServer world) {
        this.storageRoot = world.getChunkSaveLocation().toPath().resolve("fp2/" + RenderMode.HEIGHTMAP.name().toLowerCase());

        if (Files.isDirectory(this.storageRoot)) {
            try {
                long count = Files.list(this.storageRoot).parallel()
                        .filter(Files::isDirectory)
                        .flatMap((IOFunction<Path, Stream<Path>>) Files::list)
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".tmp"))
                        .filter((IOPredicate<Path>) Files::deleteIfExists)
                        .count();
                FP2.LOGGER.info("Deleted {} leftover temporary files.", count);
            } catch (IOException e) {
                FP2.LOGGER.error("Unable to delete temp files", e);
            }
        }
    }

    @Override
    public ByteBuf load(@NonNull HeightmapPos posIn) {
        if (FP2Config.debug.disableRead || FP2Config.debug.disablePersistence) {
            return null;
        }

        this.effectivelyAMutex.compute(posIn, (pos, o) -> {
            Path storagePath = this.storagePath(pos);

            if (Files.isRegularFile(storagePath)) {
                ByteBuf buf = null;
                try (FileChannel channel = FileChannel.open(storagePath, StandardOpenOption.READ)) {
                    int size = toInt(channel.size(), "file size");
                    buf = PooledByteBufAllocator.DEFAULT.ioBuffer(size, size);
                    buf.writeBytes(channel, size);

                    checkState(buf.readableBytes() == size, "only read %d/%d bytes!", buf.readableBytes(), size);
                    this.loadCache.set(buf.retain());
                } catch (IOException e) {
                    FP2.LOGGER.warn("I/O exception while loading tile at " + pos, e);
                } finally {
                    if (buf != null) {
                        buf.release();
                    }
                }
            }

            return null; //return null to release lock without actually modifying the map
        });

        ByteBuf data = this.loadCache.get();
        this.loadCache.set(null);
        return data;
    }

    @Override
    public void store(@NonNull HeightmapPos posIn, @NonNull ByteBuf data) {
        if (FP2Config.debug.disableWrite || FP2Config.debug.disablePersistence) {
            return;
        }

        this.effectivelyAMutex.compute(posIn, (IOBiFunction<HeightmapPos, Object, Object>) (pos, o) -> {
            Path storagePath = this.storagePath(pos);
            Path tmpPath = storagePath.resolveSibling(storagePath.getFileName() + ".tmp");

            Files.createDirectories(tmpPath.getParent());

            //write data to temporary file
            try (FileChannel channel = FileChannel.open(tmpPath, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
                data.readBytes(channel, data.readableBytes());
            }

            //replace real file with temporary one
            Files.move(tmpPath, storagePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            return null; //return null to release lock without actually modifying the map
        });
    }

    @Override
    public void close() throws IOException {
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
                    .writeInt(buf.readableBytes())
                    .writeInt(HEIGHTMAP_STORAGE_VERSION);
            checkState(DEFLATER_CACHE.get().compress(buf, packed));

            return packed;
        } finally {
            buf.release();
        }
    }

    public void packAndStore(@NonNull HeightmapPiece piece) {
        ByteBuf packed = this.pack(piece);
        try {
            this.store(piece, packed);
        } finally {
            packed.release();
        }
    }

    protected Path storagePath(@NonNull HeightmapPos pos) {
        return this.storageRoot.resolve(PStrings.fastFormat("%d/%d.%d.fp2", pos.level(), pos.x(), pos.z()));
    }
}
