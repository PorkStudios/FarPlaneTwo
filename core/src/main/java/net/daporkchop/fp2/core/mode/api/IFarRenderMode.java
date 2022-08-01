/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.OrderedRegistry;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.core.client.world.level.IFarLevelClient;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.event.AbstractRegisterEvent;
import net.daporkchop.fp2.core.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.core.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.core.server.player.IFarPlayerServer;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.fp2.core.util.recycler.Recycler;
import net.daporkchop.fp2.core.util.serialization.variable.IVariableSizeRecyclingCodec;
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.binary.stream.DataOut;

import java.io.IOException;

/**
 * @author DaPorkchop_
 */
public interface IFarRenderMode<POS extends IFarPos, T extends IFarTile> {
    OrderedRegistry<IFarRenderMode<?, ?>> REGISTRY = new AbstractRegisterEvent<IFarRenderMode<?, ?>>() {}.fire().immutableRegistry();

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
    IFarTileProvider<POS, T> tileProvider(@NonNull IFarLevelServer world);

    /**
     * Creates a new {@link IFarGeneratorExact exact generator} for the given {@link IFarLevelServer world} and {@link IFarTileProvider tile provider}.
     *
     * @param world    the {@link IFarLevelServer world}
     * @param provider the {@link IFarTileProvider tile provider}
     * @return the new {@link IFarGeneratorExact exact generator}
     */
    IFarGeneratorExact<POS, T> exactGenerator(@NonNull IFarLevelServer world, @NonNull IFarTileProvider<POS, T> provider);

    /**
     * Creates a new {@link IFarGeneratorRough rough generator} for the given {@link IFarLevelServer world} and {@link IFarTileProvider tile provider}.
     *
     * @param world    the {@link IFarLevelServer world}
     * @param provider the {@link IFarTileProvider tile provider}
     * @return the new {@link IFarGeneratorRough rough generator}, or {@code null} if none is available
     */
    IFarGeneratorRough<POS, T> roughGenerator(@NonNull IFarLevelServer world, @NonNull IFarTileProvider<POS, T> provider);

    /**
     * Creates a new {@link IFarScaler scaler} for the given {@link IFarLevelServer world} and {@link IFarTileProvider tile provider}.
     *
     * @param world    the {@link IFarLevelServer world}
     * @param provider the {@link IFarTileProvider tile provider}
     * @return the new {@link IFarScaler scaler}
     */
    IFarScaler<POS, T> scaler(@NonNull IFarLevelServer world, @NonNull IFarTileProvider<POS, T> provider);

    /**
     * Creates a new {@link IFarServerContext} for the given player in the given world.
     *
     * @param player the player
     * @param world  the world
     * @param config
     * @return the new {@link IFarServerContext}
     */
    IFarServerContext<POS, T> serverContext(@NonNull IFarPlayerServer player, @NonNull IFarLevelServer world, @NonNull FP2Config config);

    /**
     * Creates a new {@link IFarClientContext} for the given level.
     *
     * @param level  the level
     * @param config
     * @return the new {@link IFarClientContext}
     */
    IFarClientContext<POS, T> clientContext(@NonNull IFarLevelClient level, @NonNull FP2Config config);

    /**
     * @return a recycler for tile objects
     */
    Recycler<T> tileRecycler();

    /**
     * @return the {@link IFarDirectPosAccess} used by this render mode
     */
    IFarDirectPosAccess<POS> directPosAccess();

    /**
     * @return the {@link IFarPosCodec} used by this render mode to serialize tile positions for storage
     */
    IFarPosCodec<POS> posCodec();

    /**
     * @return the {@link IVariableSizeRecyclingCodec} used by this render mode to serialize tiles for storage
     */
    IVariableSizeRecyclingCodec<T> tileCodec();

    /**
     * Creates a {@link IFarCoordLimits} for the given block coordinate limits as defined by the given {@link IntAxisAlignedBB}.
     *
     * @param blockCoordLimits the block coordinate limits
     * @return the {@link IFarCoordLimits}
     */
    IFarCoordLimits<POS> tileCoordLimits(@NonNull IntAxisAlignedBB blockCoordLimits);

    /**
     * Reads a tile position from the given {@link ByteBuf}.
     *
     * @param buf the {@link ByteBuf} to read from
     * @return the tile position
     */
    @Deprecated
    POS readPos(@NonNull ByteBuf buf);

    /**
     * Reads a tile position from the given {@link DataIn}.
     *
     * @param in the {@link DataIn} to read from
     * @return the tile position
     */
    POS readPos(@NonNull DataIn in) throws IOException;

    /**
     * Reads a tile position from the given {@code byte[]}.
     *
     * @param arr the {@code byte[]} containing the tile position
     * @return the tile position
     */
    POS readPos(@NonNull byte[] arr);

    /**
     * Writes a tile position to the given {@link DataOut}.
     *
     * @param out the {@link DataOut} to write to
     * @param pos the tile position
     */
    void writePos(@NonNull DataOut out, @NonNull POS pos) throws IOException;

    /**
     * Writes a tile position to a {@code byte[]}.
     *
     * @param pos the tile position
     * @return a {@code byte[]} containing the written data
     */
    byte[] writePos(@NonNull POS pos);

    /**
     * @return an array of {@link POS}
     */
    POS[] posArray(int length);

    /**
     * @return an array of {@link T}
     */
    T[] tileArray(int length);
}
