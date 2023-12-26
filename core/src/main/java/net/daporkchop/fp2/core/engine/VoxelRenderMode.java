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

package net.daporkchop.fp2.core.engine;

import lombok.NonNull;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.core.client.world.level.IFarLevelClient;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.engine.ctx.VoxelClientContext;
import net.daporkchop.fp2.core.engine.ctx.VoxelServerContext;
import net.daporkchop.fp2.core.engine.server.scale.VoxelScalerIntersection;
import net.daporkchop.fp2.core.mode.api.IFarCoordLimits;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.core.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.core.mode.common.AbstractFarRenderMode;
import net.daporkchop.fp2.core.server.player.IFarPlayerServer;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;

import static net.daporkchop.fp2.core.engine.EngineConstants.*;

/**
 * Implementation of {@link IFarRenderMode} for the voxel rendering mode.
 *
 * @author DaPorkchop_
 */
@Deprecated
public class VoxelRenderMode extends AbstractFarRenderMode {
    public static final VoxelRenderMode INSTANCE = new VoxelRenderMode();

    private VoxelRenderMode() {
        super(STORAGE_VERSION, MAX_LODS, T_SHIFT);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected AbstractExactGeneratorCreationEvent exactGeneratorCreationEvent(@NonNull IFarLevelServer world, @NonNull IFarTileProvider provider) {
        return new AbstractExactGeneratorCreationEvent(world, provider) {};
    }

    @Override
    protected AbstractRoughGeneratorCreationEvent roughGeneratorCreationEvent(@NonNull IFarLevelServer world, @NonNull IFarTileProvider provider) {
        return new AbstractRoughGeneratorCreationEvent(world, provider) {};
    }

    @Override
    protected AbstractTileProviderCreationEvent tileProviderCreationEvent(@NonNull IFarLevelServer world) {
        return new AbstractTileProviderCreationEvent(world) {};
    }

    @Override
    protected Tile newTile() {
        return new Tile();
    }

    @Override
    public IFarScaler scaler(@NonNull IFarLevelServer world, @NonNull IFarTileProvider provider) {
        return new VoxelScalerIntersection(world, provider);
    }

    @Override
    public IFarServerContext serverContext(@NonNull IFarPlayerServer player, @NonNull IFarLevelServer world, @NonNull FP2Config config) {
        return new VoxelServerContext(player, world, config, this);
    }

    @Override
    public IFarClientContext clientContext(@NonNull IFarLevelClient level, @NonNull FP2Config config) {
        return new VoxelClientContext(level, config, this);
    }

    @Override
    public IFarCoordLimits tileCoordLimits(@NonNull IntAxisAlignedBB blockCoordLimits) {
        return new TileCoordLimits(
                blockCoordLimits.minX(), blockCoordLimits.minY(), blockCoordLimits.minZ(),
                blockCoordLimits.maxX(), blockCoordLimits.maxY(), blockCoordLimits.maxZ());
    }
}
