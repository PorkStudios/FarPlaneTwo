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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.region;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.core.util.FastByteArrayOutputStream;
import net.daporkchop.lib.common.function.io.IOBiFunction;
import net.daporkchop.lib.common.function.io.IOFunction;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.world.chunk.storage.RegionFileCache;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.DeflaterOutputStream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Re-implementation of {@link RegionFileCache}, but with actual thread-safety and stuff, since the vanilla code has a crazy number of race conditions.
 *
 * @author DaPorkchop_
 */
public class ThreadSafeRegionFileCache {
    public static final ThreadSafeRegionFileCache INSTANCE = new ThreadSafeRegionFileCache(256);

    protected static Path region(@NonNull Path regionDir, int chunkX, int chunkZ) {
        return regionDir.resolve(PStrings.fastFormat("r.%d.%d.mca", chunkX >> 5, chunkZ >> 5));
    }

    private final int maxSize;
    private final Map<Path, RegionFile> openFiles = Collections.synchronizedMap(new LinkedHashMap<Path, RegionFile>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Path, RegionFile> eldest) {
            if (this.size() >= ThreadSafeRegionFileCache.this.maxSize) {
                try {
                    synchronized (eldest.getValue()) {
                        eldest.getValue().close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            } else {
                return false;
            }
        }
    });

    public ThreadSafeRegionFileCache(int maxSize) {
        this.maxSize = positive(maxSize, "maxSize") - 1;
    }

    /**
     * Closes all currently open regions.
     */
    public void clear() throws IOException {
        //close and uncache all regions
        this.openFiles.values().removeIf(region -> {
            try {
                synchronized (region) {
                    region.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        });
    }

    protected RegionFile getRegion(@NonNull Path _path, boolean create) throws IOException {
        return this.openFiles.compute(_path, (IOBiFunction<Path, RegionFile, RegionFile>) (path, region) -> {
            if (region == null) { //region isn't cached, we need to open a new one
                if (create || Files.exists(path)) {
                    Files.createDirectories(path.getParent());
                    region = new RegionFile(path.toFile());
                }
            }
            if (region != null) { //begin synchronize on region
                PUnsafe.monitorEnter(region);
            }
            return region;
        });
    }

    /**
     * Writes the given chunk data to the chunk at the given position.
     *
     * @param regionDir the path to the region directory
     * @param chunkX    the X coordinate of the chunk
     * @param chunkZ    the Z coordinate of the chunk
     * @param data      the uncompressed data to write
     */
    public void write(@NonNull Path regionDir, int chunkX, int chunkZ, @NonNull ByteBuf data) throws IOException {
        //compress data
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
        try (OutputStream out = new DeflaterOutputStream(baos)) {
            data.readBytes(out, data.readableBytes());
        }

        //attempt to open region
        RegionFile region = this.getRegion(region(regionDir, chunkX, chunkZ), true);
        checkState(region != null, "region was null?!?");

        try {
            //write compressed data to region
            region.write(chunkX & 0x1F, chunkZ & 0x1F, baos.buf(), baos.size());
        } finally {
            PUnsafe.monitorExit(region);
        }
    }

    /**
     * Reads the data for the given chunk at the given position.
     *
     * @param regionDir the path to the region directory
     * @param chunkX    the X coordinate of the chunk
     * @param chunkZ    the Z coordinate of the chunk
     * @return the data stored for the given chunk, or {@code null} if the chunk doesn't exist
     */
    public DataInputStream read(@NonNull Path regionDir, int chunkX, int chunkZ) throws IOException {
        //attempt to open region
        RegionFile region = this.getRegion(region(regionDir, chunkX, chunkZ), false);

        //region doesn't exist, so we know that the chunk doesn't exist either
        if (region == null) {
            return null;
        }

        try {
            //read chunk from region
            return region.getChunkDataInputStream(chunkX & 0x1F, chunkZ & 0x1F);
        } finally {
            PUnsafe.monitorExit(region);
        }
    }

    /**
     * Checks whether or not the chunk at the given position exists.
     *
     * @param regionDir the path to the region directory
     * @param chunkX    the X coordinate of the chunk
     * @param chunkZ    the Z coordinate of the chunk
     * @return whether or not the chunk exists
     */
    public boolean exists(@NonNull Path regionDir, int chunkX, int chunkZ) throws IOException {
        //attempt to open region
        RegionFile region = this.getRegion(region(regionDir, chunkX, chunkZ), false);

        //region doesn't exist, so we know that the chunk doesn't exist either
        if (region == null) {
            return false;
        }

        try {
            //check if the chunk is stored in the region
            return region.isChunkSaved(chunkX & 0x1F, chunkZ & 0x1F);
        } finally {
            PUnsafe.monitorExit(region);
        }
    }

    /**
     * Gets a {@link Stream} over the position of every chunk that exists in the world.
     * <p>
     * Note that the returned {@link Stream} must be closed manually (using {@link Stream#close()}).
     *
     * @param regionDir the path to the region directory
     * @return a {@link Stream} over the position of every chunk that exists in the world
     */
    public Stream<ChunkPos> allChunks(@NonNull Path regionDir) throws IOException {
        if (Files.notExists(regionDir)) { //the region directory might not exist yet in a new world
            return Stream.empty();
        }

        return Files.list(regionDir).filter(Files::isRegularFile)
                .map(Path::getFileName).map(Path::toString)
                .map(Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\.mca$")::matcher)
                .filter(Matcher::matches)
                .flatMap((IOFunction<Matcher, Stream<ChunkPos>>) matcher -> {
                    RegionFile region = this.getRegion(regionDir.resolve(matcher.group()), true);
                    try {
                        int baseX = Integer.parseInt(matcher.group(1)) << 4;
                        int baseZ = Integer.parseInt(matcher.group(2)) << 4;

                        List<ChunkPos> positions = new ArrayList<>();
                        for (int dx = 0; dx < 32; dx++) {
                            for (int dz = 0; dz < 32; dz++) {
                                if (region.isChunkSaved(dx, dz)) {
                                    positions.add(new ChunkPos(baseX + dx, baseZ + dz));
                                }
                            }
                        }
                        return positions.stream();
                    } finally {
                        PUnsafe.monitorExit(region);
                    }
                });
    }
}
