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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.compat.vanilla.IBlockHeightAccess;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.mode.api.tile.ITileHandle;
import net.daporkchop.fp2.mode.api.tile.ITileMetadata;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.fp2.util.threading.ThreadingHelper;
import net.daporkchop.fp2.util.threading.futurecache.GenerationNotAllowedException;
import net.daporkchop.fp2.util.threading.scheduler.Scheduler;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.daporkchop.fp2.debug.FP2Debug.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class FarServerWorker<POS extends IFarPos, T extends IFarTile> implements Function<PriorityTask<POS>, ITileHandle<POS, T>> {
    @NonNull
    protected final AbstractFarWorld<POS, T> world;
    @NonNull
    protected final Scheduler<PriorityTask<POS>, ITileHandle<POS, T>> scheduler;

    @Override
    public ITileHandle<POS, T> apply(@NonNull PriorityTask<POS> task) {
        switch (task.stage) {
            case LOAD:
                return this.roughGetTile(task.pos);
            case UPDATE:
                return this.updateTile(task.pos);
            default:
                throw new IllegalArgumentException(task.stage.name());
        }
    }

    //
    //
    // rough tasks
    //
    //

    public ITileHandle<POS, T> roughGetTile(POS pos) {
        ITileHandle<POS, T> handle = this.world.storage().handleFor(pos);
        try {
            if (handle.isInitialized()) {
                return handle;
            }

            if (!(FP2_DEBUG && FP2Config.debug.disableExactGeneration) && this.world.anyVanillaTerrainExistsAt(pos)) {
                //there's some terrain at the given position, let's try to generate something with it
                if (pos.level() == 0) {
                    //the position is at detail level 0, do exact generation
                    try {
                        return this.attemptRoughWithExact(pos, handle, this.world.currentTime - 1L);
                    } catch (GenerationNotAllowedException e) {
                        //the terrain existed, but wasn't populated so we don't want to use it
                    }
                } else {
                    //force the tile to be scaled, which will cause this to be executed recursively
                    return this.roughScaleTile(pos, handle);
                }
            }

            if (pos.level() == 0 || this.world.canGenerateRough(pos)) {
                //the tile can be generated using the rough generator
                return this.roughGenerateTile(pos, handle);
            } else {
                //the tile cannot be generated using the rough generator
                //this will generate the tile and all tiles below it down to level 0 until the tile can be "generated" from scaled data
                return this.roughScaleTile(pos, handle);
            }
        } catch (Throwable t) {
            ThreadingHelper.handle(this.world.world, t);
            PUnsafe.throwException(t);
            throw new AssertionError();
        } finally {
            this.world.tileAvailable(handle);
        }
    }

    public ITileHandle<POS, T> attemptRoughWithExact(POS pos, ITileHandle<POS, T> handle, long newTimestamp) throws GenerationNotAllowedException {
        if (handle.timestamp() >= newTimestamp) { //break out early if tile is already done or newer
            return handle;
        }

        SimpleRecycler<T> tileRecycler = this.world.mode().tileRecycler();
        T tile = tileRecycler.allocate();
        try {
            this.doExactPrefetchAndGenerate(pos, tile, false);

            if (handle.set(ITileMetadata.ofTimestamp(newTimestamp), tile)) { //only notify world if the tile was changed
                this.world.tileChanged(handle, false);
            }
        } finally {
            tileRecycler.release(tile);
        }

        return handle;
    }

    public ITileHandle<POS, T> roughGenerateTile(POS pos, ITileHandle<POS, T> handle) {
        checkArg(pos.level() == 0 || this.world.canGenerateRough(pos), "cannot do rough generation at %s!", pos);

        if (handle.isInitialized()) { //break out early if tile is already done or newer
            return handle;
        }

        SimpleRecycler<T> tileRecycler = this.world.mode().tileRecycler();
        T tile = tileRecycler.allocate();
        try {
            long newTimestamp;
            IFarGeneratorRough<POS, T> generatorRough = this.world.generatorRough();
            if (generatorRough != null) { //generate tile using rough generator
                newTimestamp = ITileMetadata.TIMESTAMP_GENERATED;
                generatorRough.generate(pos, tile);
            } else { //there is no rough generator - we'll have to fall back to using the exact generator
                newTimestamp = this.world.currentTime;
                this.doExactPrefetchAndGenerate(pos, tile, true);
            }

            if (handle.set(ITileMetadata.ofTimestamp(newTimestamp), tile)) { //only notify world if the tile was changed
                this.world.tileChanged(handle, false);
            }
        } catch (GenerationNotAllowedException e) { //impossible
            throw new IllegalStateException(e);
        } finally {
            tileRecycler.release(tile);
        }

        return handle;
    }

    public ITileHandle<POS, T> roughScaleTile(POS pos, ITileHandle<POS, T> handle) {
        //generate scale inputs
        List<ITileHandle<POS, T>> srcHandles = this.scheduler.scatterGather(this.world.scaler().inputs(pos).map(p -> new PriorityTask<>(TaskStage.LOAD, p)).collect(Collectors.toList()));

        //inflate sources
        SimpleRecycler<T> tileRecycler = this.world.mode().tileRecycler();
        T[] srcs = this.world.mode().tileArray(srcHandles.size());
        for (int i = 0; i < srcHandles.size(); i++) {
            srcs[i] = srcHandles.get(i).snapshot().loadTile(tileRecycler);
        }

        T dst = tileRecycler.allocate();
        try {
            if (handle.isInitialized()) { //break out early if tile is already done or newer
                return handle;
            }

            //actually do scaling
            this.world.scaler().scale(srcs, dst);
            if (handle.set(ITileMetadata.ofTimestamp(ITileMetadata.TIMESTAMP_GENERATED), dst)) {
                this.world.tileChanged(handle, false);
            }
        } finally {
            tileRecycler.release(dst);
            for (T src : srcs) {
                if (src != null) {
                    tileRecycler.release(src);
                }
            }
        }

        return handle;
    }

    //
    //
    // update tasks
    //
    //

    public ITileHandle<POS, T> updateTile(POS pos) {
        ITileHandle<POS, T> handle = this.world.storage().handleFor(pos);

        long newTimestamp = this.world.storage.dirtyTracker().dirtyTimestamp(pos);
        if (newTimestamp < 0L) {
            FP2_LOG.warn("Duplicate update task scheduled for tile at {}!", pos);
            return handle;
        }

        try {
            if (FP2_DEBUG && FP2Config.debug.disableExactGeneration) { //updates will always use the exact generator, so don't use them
                return handle;
            }

            if (handle.timestamp() >= newTimestamp) { //break out early if tile is already done or newer
                return handle;
            }

            if (pos.level() == 0) {
                this.updateTileExact(pos, handle, newTimestamp);
            } else {
                this.updateTileScale(pos, handle, newTimestamp);
            }

            return handle;
        } finally {
            //remove tile from tracker again
            this.world.storage.dirtyTracker().clearDirty(pos, newTimestamp);
        }
    }

    protected void updateTileExact(POS pos, ITileHandle<POS, T> handle, long newTimestamp) {
        SimpleRecycler<T> tileRecycler = this.world.mode().tileRecycler();
        T tile = tileRecycler.allocate();
        try {
            this.doExactPrefetchAndGenerate(pos, tile, false);

            if (handle.set(ITileMetadata.ofTimestamp(newTimestamp), tile)) { //only notify world if the tile was changed
                this.world.tileChanged(handle, true);
            }
        } catch (GenerationNotAllowedException e) {
            //silently discard
        } finally {
            tileRecycler.release(tile);
        }
    }

    protected void doExactPrefetchAndGenerate(POS pos, T tile, boolean generationAllowed) throws GenerationNotAllowedException {
        //prefetch terrain
        IBlockHeightAccess access = generationAllowed
                ? this.world.blockAccess().prefetch(this.world.generatorExact().neededColumns(pos), world -> this.world.generatorExact().neededCubes(world, pos))
                : this.world.blockAccess().prefetchWithoutGenerating(this.world.generatorExact().neededColumns(pos), world -> this.world.generatorExact().neededCubes(world, pos));

        //generate tile
        this.world.generatorExact().generate(access, pos, tile);
    }

    protected void updateTileScale(POS pos, ITileHandle<POS, T> handle, long newTimestamp) {
        //generate scale inputs
        List<ITileHandle<POS, T>> srcHandles = this.scheduler.scatterGather(this.world.scaler().inputs(pos).map(p -> new PriorityTask<>(TaskStage.UPDATE, p)).collect(Collectors.toList()));

        //inflate sources
        SimpleRecycler<T> tileRecycler = this.world.mode().tileRecycler();
        T[] srcs = this.world.mode().tileArray(srcHandles.size());
        for (int i = 0; i < srcHandles.size(); i++) {
            srcs[i] = srcHandles.get(i).snapshot().loadTile(tileRecycler);
        }

        T dst = tileRecycler.allocate();
        try {
            if (handle.timestamp() >= newTimestamp) { //break out early if tile is already done or newer
                return;
            }

            //actually do scaling
            this.world.scaler().scale(srcs, dst);
            if (handle.set(ITileMetadata.ofTimestamp(newTimestamp), dst)) {
                this.world.tileChanged(handle, true);
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
}
