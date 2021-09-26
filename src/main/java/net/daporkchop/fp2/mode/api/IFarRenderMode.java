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

package net.daporkchop.fp2.mode.api;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.mode.api.ctx.IFarWorldClient;
import net.daporkchop.fp2.mode.api.ctx.IFarWorldServer;
import net.daporkchop.fp2.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.mode.heightmap.HeightmapRenderMode;
import net.daporkchop.fp2.mode.voxel.VoxelRenderMode;
import net.daporkchop.fp2.util.IFarPlayer;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.fp2.util.event.RegisterRenderModesEvent;
import net.daporkchop.fp2.util.registry.LinkedOrderedRegistry;
import net.daporkchop.fp2.util.registry.OrderedRegistry;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author DaPorkchop_
 */
public interface IFarRenderMode<POS extends IFarPos, T extends IFarTile> {
    OrderedRegistry<IFarRenderMode<?, ?>> REGISTRY = new RegisterRenderModesEvent(new LinkedOrderedRegistry<IFarRenderMode<?, ?>>()
            .addLast("voxel", new VoxelRenderMode())
            .addLast("heightmap", new HeightmapRenderMode())).fire().immutableRegistry();

    /**
     * @return this storage version's name
     */
    String name();

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
    IFarTileProvider<POS, T> tileProvider(@NonNull WorldServer world);

    /**
     * Creates a new {@link IFarGeneratorExact} for the given world.
     *
     * @param world the world
     * @return the new {@link IFarGeneratorExact}
     */
    IFarGeneratorExact<POS, T> exactGenerator(@NonNull WorldServer world);

    /**
     * Creates a new {@link IFarGeneratorRough} for the given world.
     *
     * @param world the world
     * @return the new {@link IFarGeneratorRough}
     */
    IFarGeneratorRough<POS, T> roughGenerator(@NonNull WorldServer world);

    /**
     * Creates a new {@link IFarServerContext} for the given player in the given world.
     *
     * @param player the player
     * @param world  the world
     * @param config
     * @return the new {@link IFarServerContext}
     */
    IFarServerContext<POS, T> serverContext(@NonNull IFarPlayer player, @NonNull IFarWorldServer world, @NonNull FP2Config config);

    /**
     * Creates a new {@link IFarClientContext} for the given world.
     *
     * @param world  the world
     * @param config
     * @return the new {@link IFarClientContext}
     */
    @SideOnly(Side.CLIENT)
    IFarClientContext<POS, T> clientContext(@NonNull IFarWorldClient world, @NonNull FP2Config config);

    /**
     * @return a recycler for tile objects
     */
    SimpleRecycler<T> tileRecycler();

    /**
     * @return the {@link IFarDirectPosAccess} used by this render mode
     */
    IFarDirectPosAccess<POS> directPosAccess();

    /**
     * Reads a tile position from the given {@link ByteBuf}.
     *
     * @param buf the {@link ByteBuf} to read from
     * @return the tile position
     */
    POS readPos(@NonNull ByteBuf buf);

    /**
     * @return an array of {@link POS}
     */
    POS[] posArray(int length);

    /**
     * @return an array of {@link T}
     */
    T[] tileArray(int length);
}
