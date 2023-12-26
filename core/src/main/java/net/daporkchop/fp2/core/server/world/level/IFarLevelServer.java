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

package net.daporkchop.fp2.core.server.world.level;

import lombok.NonNull;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.FLevelServer;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.ctx.IFarLevel;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.server.world.ExactFBlockLevelHolder;
import net.daporkchop.fp2.core.server.world.TerrainGeneratorInfo;

import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;

/**
 * @author DaPorkchop_
 */
public interface IFarLevelServer extends IFarLevel, FLevelServer {
    /**
     * Gets the {@link IFarTileProvider} used by the given {@link IFarRenderMode} in this level.
     *
     * @param mode the {@link IFarRenderMode}
     * @return the {@link IFarTileProvider} used by the given {@link IFarRenderMode} in this level
     * @throws NoSuchElementException if no {@link IFarTileProvider} is registered for the given render mode (i.e. the given render mode is invalid)
     */
    <POS extends IFarPos, T extends IFarTile> IFarTileProvider tileProviderFor(@NonNull IFarRenderMode mode) throws NoSuchElementException;

    /**
     * Runs the given action on every {@link IFarTileProvider}.
     *
     * @param action the action
     */
    void forEachTileProvider(@NonNull BiConsumer<? super IFarRenderMode, ? super IFarTileProvider> action);

    /**
     * @return the level's root directory
     */
    @Deprecated
    Path levelDirectory();

    /**
     * @return a {@link TerrainGeneratorInfo} for this level
     */
    TerrainGeneratorInfo terrainGeneratorInfo();

    /**
     * @return an {@link ExactFBlockLevelHolder} for accessing this level's exact {@link FBlockLevel}
     */
    ExactFBlockLevelHolder exactBlockLevelHolder();

    /**
     * @return this level's sea level
     */
    int seaLevel();
}
