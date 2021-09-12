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

package net.daporkchop.fp2.mode.common.server;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.compat.vanilla.IBlockHeightAccess;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.tile.ITileHandle;
import net.daporkchop.fp2.mode.api.tile.ITileMetadata;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.fp2.util.threading.futurecache.GenerationNotAllowedException;
import net.daporkchop.fp2.util.threading.scheduler.Scheduler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.fp2.debug.FP2Debug.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractTileTask<POS extends IFarPos, T extends IFarTile> implements Supplier<ITileHandle<POS, T>> {
    protected final AbstractFarWorld<POS, T> world;
    protected final Scheduler<PriorityTask<POS>, ITileHandle<POS, T>> scheduler;

    protected final POS pos;
    protected final ITileHandle<POS, T> handle;

    public AbstractTileTask(@NonNull AbstractFarWorld<POS, T> world, @NonNull Scheduler<PriorityTask<POS>, ITileHandle<POS, T>> scheduler, @NonNull POS pos) {
        this.world = world;
        this.scheduler = scheduler;

        this.pos = pos;
        this.handle = world.storage().handleFor(pos);
    }

    protected abstract long minimumTimestamp();

    protected abstract boolean allowNewGeneration();

    protected abstract PriorityTask<POS> taskFor(@NonNull POS pos);

    @Override
    public ITileHandle<POS, T> get() {
        long minimumTimestamp = this.minimumTimestamp();
        long worldTimestamp = this.world.lastCompletedTick;
        checkState(worldTimestamp >= minimumTimestamp, "worldTimestamp (%d) is less than minimumTimestamp (%d)?!?", worldTimestamp, minimumTimestamp);

        if (this.handle.timestamp() >= minimumTimestamp) { //break out early if already new enough
            return this.handle;
        }

        if (!(FP2_DEBUG && FP2Config.debug.disableExactGeneration) && this.world.anyVanillaTerrainExistsAt(this.pos)) {
            //there's some terrain at the given position, let's try to generate something with it
            if (this.pos.level() == 0) {
                //the position is at detail level 0, do exact generation
                try {
                    this.generateExact(worldTimestamp, false);
                    return this.handle;
                } catch (GenerationNotAllowedException e) {
                    //the terrain existed, but wasn't populated so we don't want to use it
                }
            } else {
                //force the tile to be scaled, which will cause this to be executed recursively
                this.generateScale(worldTimestamp);
                return this.handle;
            }
        }

        if (!this.allowNewGeneration()) { //we aren't allowed to generate any new tiles
            return this.handle;
        }

        if (this.world.canGenerateRough(this.pos)) { //the tile can be generated using the rough generator
            this.generateRough(worldTimestamp);
            return this.handle;
        }

        //rough generation isn't available...
        if (this.pos.level() == 0) {
            //do exact generation, allowing it to generate vanilla terrain if needed
            try {
                this.generateExact(worldTimestamp, true);
                return this.handle;
            } catch (GenerationNotAllowedException e) { //impossible
                throw new IllegalArgumentException("generation blocked while processing tile at " + this.pos, e);
            }
        } else { //this will generate the tile and all tiles below it down to level 0 until the tile can be "generated" from scaled data
            this.generateScale(worldTimestamp);
            return this.handle;
        }
    }

    protected void generateRough(long minimumTimestamp) {
        checkArg(this.pos.level() == 0 || this.world.canGenerateRough(this.pos), "cannot do rough generation at %s!", this.pos);

        SimpleRecycler<T> tileRecycler = this.world.mode().tileRecycler();
        T tile = tileRecycler.allocate();
        try {
            this.world.generatorRough().generate(this.pos, tile);

            if (this.handle.set(ITileMetadata.ofTimestamp(minimumTimestamp), tile)) { //only notify world if the tile was changed
                this.world.tileChanged(this.handle, false);
            }
        } finally {
            tileRecycler.release(tile);
        }
    }

    protected void generateExact(long minimumTimestamp, boolean allowGeneration) throws GenerationNotAllowedException {
        SimpleRecycler<T> tileRecycler = this.world.mode().tileRecycler();
        T tile = tileRecycler.allocate();
        try {
            //prefetch terrain
            Stream<ChunkPos> columns = this.world.generatorExact().neededColumns(this.pos);
            Function<IBlockHeightAccess, Stream<Vec3i>> cubesMappingFunction = world -> this.world.generatorExact().neededCubes(world, this.pos);

            IBlockHeightAccess access = allowGeneration
                    ? this.world.blockAccess().prefetch(columns, cubesMappingFunction)
                    : this.world.blockAccess().prefetchWithoutGenerating(columns, cubesMappingFunction);

            //generate tile
            this.world.generatorExact().generate(access, this.pos, tile);

            if (this.handle.set(ITileMetadata.ofTimestamp(minimumTimestamp), tile)) { //only notify world if the tile was changed
                this.world.tileChanged(this.handle, false);
            }
        } finally {
            tileRecycler.release(tile);
        }
    }

    protected void generateScale(long minimumTimestamp) {
        //generate scale inputs
        List<ITileHandle<POS, T>> srcHandles = this.scheduler.scatterGather(this.world.scaler().inputs(this.pos).map(this::taskFor).collect(Collectors.toList()));

        if (this.handle.timestamp() >= minimumTimestamp) { //break out early if tile is already done
            return;
        }

        //inflate sources
        SimpleRecycler<T> tileRecycler = this.world.mode().tileRecycler();
        T[] srcs = this.world.mode().tileArray(srcHandles.size());
        for (int i = 0; i < srcHandles.size(); i++) {
            srcs[i] = srcHandles.get(i).snapshot().loadTile(tileRecycler);
        }

        if (this.handle.timestamp() >= minimumTimestamp) { //break out early if tile is already done
            return;
        }

        T dst = tileRecycler.allocate();
        try {
            //actually do scaling
            this.world.scaler().scale(srcs, dst);

            if (this.handle.set(ITileMetadata.ofTimestamp(minimumTimestamp), dst)) {
                this.world.tileChanged(this.handle, false);
            }
        } finally {
            tileRecycler.release(dst);
            for (T src : srcs) {
                if (src != null) {
                    tileRecycler.release(src);
                }
            }
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class Load<POS extends IFarPos, T extends IFarTile> extends AbstractTileTask<POS, T> {
        public Load(@NonNull AbstractFarWorld<POS, T> world, @NonNull Scheduler<PriorityTask<POS>, ITileHandle<POS, T>> scheduler, @NonNull POS pos) {
            super(world, scheduler, pos);
        }

        @Override
        protected long minimumTimestamp() {
            return ITileMetadata.TIMESTAMP_GENERATED;
        }

        @Override
        protected boolean allowNewGeneration() {
            return true;
        }

        @Override
        protected PriorityTask<POS> taskFor(@NonNull POS pos) {
            return this.world.loadTaskFor(pos);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class Update<POS extends IFarPos, T extends IFarTile> extends AbstractTileTask<POS, T> {
        @Getter(AccessLevel.PROTECTED)
        protected final long minimumTimestamp;

        public Update(@NonNull AbstractFarWorld<POS, T> world, @NonNull Scheduler<PriorityTask<POS>, ITileHandle<POS, T>> scheduler, @NonNull POS pos) {
            super(world, scheduler, pos);

            long minimumTimestamp = this.handle.dirtyTimestamp();
            this.minimumTimestamp = minimumTimestamp == ITileMetadata.TIMESTAMP_BLANK ? ITileMetadata.TIMESTAMP_GENERATED : minimumTimestamp;
        }

        @Override
        protected boolean allowNewGeneration() {
            return false;
        }

        @Override
        protected PriorityTask<POS> taskFor(@NonNull POS pos) {
            return this.world.updateTaskFor(pos);
        }
    }
}
