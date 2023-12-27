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

import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.FLevelServer;
import net.daporkchop.fp2.core.mode.api.ctx.IFarLevel;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.server.world.ExactFBlockLevelHolder;
import net.daporkchop.fp2.core.server.world.TerrainGeneratorInfo;

import java.nio.file.Path;

/**
 * @author DaPorkchop_
 */
public interface IFarLevelServer extends IFarLevel, FLevelServer {
    /**
     * Gets the {@link IFarTileProvider} used in this level.
     *
     * @return the {@link IFarTileProvider} used in this level
     */
    IFarTileProvider tileProvider();

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
