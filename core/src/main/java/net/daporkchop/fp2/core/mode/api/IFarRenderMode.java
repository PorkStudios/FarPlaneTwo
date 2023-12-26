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

package net.daporkchop.fp2.core.mode.api;

import lombok.NonNull;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.core.client.world.level.IFarLevelClient;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.engine.Tile;
import net.daporkchop.fp2.core.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.core.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.core.server.player.IFarPlayerServer;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.lib.common.pool.recycler.Recycler;

/**
 * @author DaPorkchop_
 */
public interface IFarRenderMode {
    /**
     * @return this storage version's name
     */
    String name();

    /**
     * The maximum number of detail levels which may be used by this render mode.
     */
    int maxLevels();

    /**
     * @return {@code log2()} of the side length of a tile, in blocks
     */
    int tileShift();

    /**
     * @return the storage format version number
     */
    int storageVersion();

    /**
     * Creates a new {@link IFarTileProvider} for the given vanilla world.
     *
     * @param world the vanilla world
     * @return the new {@link IFarTileProvider}
     */
    IFarTileProvider tileProvider(@NonNull IFarLevelServer world);

    /**
     * Creates a new {@link IFarGeneratorExact exact generator} for the given {@link IFarLevelServer world} and {@link IFarTileProvider tile provider}.
     *
     * @param world    the {@link IFarLevelServer world}
     * @param provider the {@link IFarTileProvider tile provider}
     * @return the new {@link IFarGeneratorExact exact generator}
     */
    IFarGeneratorExact exactGenerator(@NonNull IFarLevelServer world, @NonNull IFarTileProvider provider);

    /**
     * Creates a new {@link IFarGeneratorRough rough generator} for the given {@link IFarLevelServer world} and {@link IFarTileProvider tile provider}.
     *
     * @param world    the {@link IFarLevelServer world}
     * @param provider the {@link IFarTileProvider tile provider}
     * @return the new {@link IFarGeneratorRough rough generator}, or {@code null} if none is available
     */
    IFarGeneratorRough roughGenerator(@NonNull IFarLevelServer world, @NonNull IFarTileProvider provider);

    /**
     * Creates a new {@link IFarScaler scaler} for the given {@link IFarLevelServer world} and {@link IFarTileProvider tile provider}.
     *
     * @param world    the {@link IFarLevelServer world}
     * @param provider the {@link IFarTileProvider tile provider}
     * @return the new {@link IFarScaler scaler}
     */
    IFarScaler scaler(@NonNull IFarLevelServer world, @NonNull IFarTileProvider provider);

    /**
     * Creates a new {@link IFarServerContext} for the given player in the given world.
     *
     * @param player the player
     * @param world  the world
     * @param config
     * @return the new {@link IFarServerContext}
     */
    IFarServerContext serverContext(@NonNull IFarPlayerServer player, @NonNull IFarLevelServer world, @NonNull FP2Config config);

    /**
     * Creates a new {@link IFarClientContext} for the given level.
     *
     * @param level  the level
     * @param config
     * @return the new {@link IFarClientContext}
     */
    IFarClientContext clientContext(@NonNull IFarLevelClient level, @NonNull FP2Config config);

    /**
     * @return a recycler for tile objects
     */
    Recycler<Tile> tileRecycler();

    /**
     * Creates a {@link IFarCoordLimits} for the given block coordinate limits as defined by the given {@link IntAxisAlignedBB}.
     *
     * @param blockCoordLimits the block coordinate limits
     * @return the {@link IFarCoordLimits}
     */
    IFarCoordLimits tileCoordLimits(@NonNull IntAxisAlignedBB blockCoordLimits);
}
