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

import dev.farplane.engine.Tile;
import dev.farplane.engine.TilePos;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for tile storage backends.
 * <p>
 * Implementations handle persistence of tile data to disk.
 *
 * @author FarPlane contributors
 */
public interface TileStorage extends AutoCloseable {

    /**
     * Loads a tile from storage.
     *
     * @param pos the tile position
     * @return a future that completes with the tile, or null if not found
     */
    CompletableFuture<Tile> load(TilePos pos);

    /**
     * Saves a tile to storage.
     *
     * @param pos  the tile position
     * @param tile the tile data to save
     * @return a future that completes when the tile is saved
     */
    CompletableFuture<Void> save(TilePos pos, Tile tile);

    /**
     * Deletes a tile from storage.
     *
     * @param pos the tile position
     * @return a future that completes when the tile is deleted
     */
    CompletableFuture<Void> delete(TilePos pos);

    /**
     * Checks if a tile exists in storage.
     *
     * @param pos the tile position
     * @return true if the tile exists
     */
    boolean exists(TilePos pos);

    /**
     * Flushes any pending writes to disk.
     */
    void flush() throws IOException;

    @Override
    void close() throws IOException;
}
