/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2026 DaPorkchop_
 * Portions Copyright (c) 2026 FarPlane contributors
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

package dev.farplane.engine.storage;

import dev.farplane.Farplane;
import dev.farplane.engine.Tile;
import dev.farplane.engine.TilePos;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static dev.farplane.engine.EngineConstants.*;

/**
 * File-based tile storage implementation.
 * <p>
 * Stores tiles as individual files in a directory hierarchy:
 * {root}/tiles/{level}/{x}/{y}/{z}.dat
 *
 * @author FarPlane contributors
 */
public class FileTileStorage implements TileStorage {
    private final Path rootDir;
    private final ExecutorService ioExecutor;

    public FileTileStorage(Path worldDir) {
        this.rootDir = worldDir.resolve("farplane").resolve("tiles");
        this.ioExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "FarPlane-IO");
            t.setDaemon(true);
            return t;
        });

        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            Farplane.LOGGER.error("Failed to create storage directory: {}", rootDir, e);
        }
    }

    private Path tilePath(TilePos pos) {
        return rootDir
                .resolve(String.valueOf(pos.level()))
                .resolve(String.valueOf(pos.x()))
                .resolve(String.valueOf(pos.y()))
                .resolve(pos.z() + ".dat");
    }

    @Override
    public CompletableFuture<Tile> load(TilePos pos) {
        return CompletableFuture.supplyAsync(() -> {
            Path path = tilePath(pos);
            if (!Files.exists(path)) {
                return null;
            }

            try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
                Tile tile = new Tile();
                readTile(in, tile);
                return tile;
            } catch (IOException e) {
                Farplane.LOGGER.warn("Failed to load tile at {}: {}", pos, e.getMessage());
                return null;
            }
        }, ioExecutor);
    }

    @Override
    public CompletableFuture<Void> save(TilePos pos, Tile tile) {
        return CompletableFuture.runAsync(() -> {
            Path path = tilePath(pos);
            try {
                Files.createDirectories(path.getParent());
                try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)))) {
                    writeTile(out, tile);
                }
            } catch (IOException e) {
                Farplane.LOGGER.warn("Failed to save tile at {}: {}", pos, e.getMessage());
            }
        }, ioExecutor);
    }

    @Override
    public CompletableFuture<Void> delete(TilePos pos) {
        return CompletableFuture.runAsync(() -> {
            Path path = tilePath(pos);
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                Farplane.LOGGER.warn("Failed to delete tile at {}: {}", pos, e.getMessage());
            }
        }, ioExecutor);
    }

    @Override
    public boolean exists(TilePos pos) {
        return Files.exists(tilePath(pos));
    }

    @Override
    public void flush() {
        // File-based storage is already flushed after each write
    }

    @Override
    public void close() {
        ioExecutor.shutdown();
    }

    // --- Tile serialization format ---
    // Version 1:
    //   int: count
    //   For each entry:
    //     short: cellIndex (x*256 + y*16 + z)
    //     int: x, y, z (position data)
    //     int: edges
    //     int: state0, state1, state2
    //     int: biomeAndLight

    private static final int FORMAT_VERSION = 1;

    private void writeTile(DataOutputStream out, Tile tile) throws IOException {
        out.writeInt(FORMAT_VERSION);
        out.writeInt(tile.count());

        // We need to iterate through all cells to find set ones
        // For v1, we use a simple approach: write all entries sequentially
        // The Tile class stores data in parallel arrays indexed by slot
        // We need to iterate by cell index to maintain spatial locality

        // For now, write a placeholder - the actual serialization needs access to internal arrays
        // This will be implemented when we add proper serialization support to Tile
        for (int i = 0; i < tile.count(); i++) {
            // Write slot index (we'll use sequential for now)
            out.writeInt(i);
        }
    }

    private void readTile(DataInputStream in, Tile tile) throws IOException {
        int version = in.readInt();
        if (version != FORMAT_VERSION) {
            throw new IOException("Unsupported tile format version: " + version);
        }

        int count = in.readInt();
        // For v1, we just mark that we read the count
        // Actual deserialization will be implemented with proper Tile serialization
        for (int i = 0; i < count; i++) {
            in.readInt(); // skip slot indices
        }
    }
}
