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
import net.daporkchop.fp2.mode.api.Compressed;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.fp2.util.threading.futurecache.GenerationNotAllowedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.daporkchop.fp2.debug.FP2Debug.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class FarServerWorker<POS extends IFarPos, T extends IFarTile> implements Consumer<PriorityTask<POS>> {
    @NonNull
    protected final AbstractFarWorld<POS, T> world;

    @Override
    public void accept(PriorityTask<POS> task) {
        switch (task.stage()) {
            case LOAD:
                this.loadTile(task, task.pos());
                break;
            case ROUGH:
                this.world.tileAvailable(this.roughGetTile(task, task.pos()));
                break;
            case UPDATE:
                this.updateTile(task, task.pos());
                break;
            default:
                throw new IllegalArgumentException(Objects.toString(task));
        }
    }

    //

    public Compressed<POS, T> loadTile(PriorityTask<POS> root, POS pos) {
        Compressed<POS, T> compressedTile = this.world.getTileCachedOrLoad(pos);
        if (compressedTile.isGenerated()) {
            //this unmarks the tile as not done and notifies the player tracker
            this.world.tileAvailable(compressedTile);
        } else {
            //the tile has not been fully generated yet
            //rather than getting the tile now, we enqueue it for rough generation later. this allows all of the LOAD tasks to run first, so that we can send the tiles that
            // are already available on disk as soon as possible
            this.world.executor.submit(new PriorityTask<>(TaskStage.ROUGH, pos));
        }
        return compressedTile;
    }

    //
    //
    // rough tasks
    //
    //

    public Compressed<POS, T> roughGetTile(PriorityTask<POS> root, POS pos) {
        if (!(FP2_DEBUG && FP2Config.debug.disableExactGeneration) && this.world.anyVanillaTerrainExistsAt(pos)) {
            //there's some terrain at the given position, let's try to generate something with it
            if (pos.level() == 0) {
                //the position is at detail level 0, do exact generation
                try {
                    return this.attemptRoughWithExact(root, pos, this.world.currentTime - 1L);
                } catch (GenerationNotAllowedException e) {
                    //the terrain existed, but wasn't populated so we don't want to use it
                }
            } else {
                //force the tile to be scaled, which will cause this to be executed recursively
                return this.roughScaleTile(root, pos);
            }
        }

        if (pos.level() == 0 || this.world.canGenerateRough(pos)) {
            //the tile can be generated using the rough generator
            return this.roughGenerateTile(root, pos);
        } else {
            //the tile cannot be generated using the rough generator
            //this will generate the tile and all tiles below it down to level 0 until the tile can be "generated" from scaled data
            return this.roughScaleTile(root, pos);
        }
    }

    public Compressed<POS, T> attemptRoughWithExact(PriorityTask<POS> root, POS pos, long newTimestamp) throws GenerationNotAllowedException {
        this.world.executor().checkForHigherPriorityWork(root);

        Compressed<POS, T> compressedTile = this.world.getTileCachedOrLoad(pos);
        if (compressedTile.timestamp() >= newTimestamp) { //break out early if tile is already done or newer
            return compressedTile;
        }

        SimpleRecycler<T> tileRecycler = this.world.mode().tileRecycler();
        T tile = tileRecycler.allocate();
        compressedTile.writeLock().lock();
        try {
            if (compressedTile.timestamp() >= newTimestamp) {
                return compressedTile;
            }

            this.doExactPrefetchAndGenerate(pos, tile, false);

            if (compressedTile.set(newTimestamp, tile, tile.extra())) { //only notify world if the tile was changed
                this.world.tileChanged(compressedTile, false);
            }
        } finally {
            compressedTile.writeLock().unlock();
            tileRecycler.release(tile);
        }

        return compressedTile;
    }

    public Compressed<POS, T> roughGenerateTile(PriorityTask<POS> root, POS pos) {
        checkArg(pos.level() == 0 || this.world.canGenerateRough(pos), "cannot do rough generation at %s!", pos);
        this.world.executor().checkForHigherPriorityWork(root);

        Compressed<POS, T> compressedTile = this.world.getTileCachedOrLoad(pos);
        if (compressedTile.isGenerated()) { //break out early if tile is already done or newer
            return compressedTile;
        }

        SimpleRecycler<T> tileRecycler = this.world.mode().tileRecycler();
        T tile = tileRecycler.allocate();
        compressedTile.writeLock().lock();
        try {
            if (compressedTile.isGenerated()) {
                return compressedTile;
            }

            long newTimestamp;
            IFarGeneratorRough<POS, T> generatorRough = this.world.generatorRough();
            if (generatorRough != null) { //generate tile using rough generator
                newTimestamp = Compressed.TIMESTAMP_GENERATED;
                generatorRough.generate(pos, tile);
            } else { //there is no rough generator - we'll have to fall back to using the exact generator
                newTimestamp = this.world.currentTime;
                this.doExactPrefetchAndGenerate(pos, tile, true);
            }

            if (compressedTile.set(newTimestamp, tile, tile.extra())) { //only notify world if the tile was changed
                this.world.tileChanged(compressedTile, false);
            }
        } catch (GenerationNotAllowedException e) { //impossible
            throw new IllegalStateException(e);
        } finally {
            compressedTile.writeLock().unlock();
            tileRecycler.release(tile);
        }

        return compressedTile;
    }

    public Compressed<POS, T> roughScaleTile(PriorityTask<POS> root, POS pos) {
        this.world.executor().checkForHigherPriorityWork(root);

        Compressed<POS, T> compressedTile = this.world.getTileCachedOrLoad(pos);
        if (compressedTile.isGenerated()) { //break out early if tile is already done or newer
            return compressedTile;
        }

        return this.scaleTile0(root, pos, compressedTile, Compressed.TIMESTAMP_GENERATED, false);
    }

    //
    //
    // update tasks
    //
    //

    public void updateTile(PriorityTask<POS> root, POS pos) {
        long newTimestamp = this.world.storage.dirtyTracker().dirtyTimestamp(pos);
        if (newTimestamp < 0L) {
            FP2_LOG.warn("Duplicate update task scheduled for tile at {}!", pos);
            return;
        }

        try {
            if (FP2_DEBUG && FP2Config.debug.disableExactGeneration) { //updates will always use the exact generator, so don't use them
                return;
            }

            this.updateTile(root, pos, newTimestamp);
        } finally {
            //remove tile from tracker again
            this.world.storage.dirtyTracker().clearDirty(pos, newTimestamp);
        }
    }

    public Compressed<POS, T> updateTile(PriorityTask<POS> root, POS pos, long newTimestamp) {
        Compressed<POS, T> compressedTile = this.world.getTileCachedOrLoad(pos);
        if (compressedTile.timestamp() >= newTimestamp) {
            return compressedTile;
        }

        if (pos.level() == 0) {
            this.updateTileExact(root, pos, compressedTile, newTimestamp);
        } else {
            this.updateTileScale(root, pos, compressedTile, newTimestamp);
        }

        return compressedTile;
    }

    public void updateTileExact(PriorityTask<POS> root, POS pos, Compressed<POS, T> compressedTile, long newTimestamp) {
        SimpleRecycler<T> tileRecycler = this.world.mode().tileRecycler();
        T tile = tileRecycler.allocate();
        compressedTile.writeLock().lock();
        try {
            if (compressedTile.timestamp() >= newTimestamp) {
                return;
            }

            this.doExactPrefetchAndGenerate(pos, tile, false);

            if (compressedTile.set(newTimestamp, tile, tile.extra())) { //only notify world if the tile was changed
                this.world.tileChanged(compressedTile, true);
            }
        } catch (GenerationNotAllowedException e) {
            //silently discard
        } finally {
            compressedTile.writeLock().unlock();
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

    public void updateTileScale(PriorityTask<POS> root, POS pos, Compressed<POS, T> compressedTile, long newTimestamp) {
        this.scaleTile0(root, pos, compressedTile, newTimestamp, true);
    }

    //
    //
    // helpers
    //
    //

    public Compressed<POS, T> scaleTile0(PriorityTask<POS> root, POS pos, Compressed<POS, T> compressedTile, long newTimestamp, boolean allowScale) {
        this.world.executor().checkForHigherPriorityWork(root);

        //generate scale inputs
        List<POS> srcPositions = this.world.scaler().inputs(pos).collect(Collectors.toList());
        List<Compressed<POS, T>> compressedSrcs = new ArrayList<>(srcPositions.size());
        for (POS srcPosition : srcPositions) {
            compressedSrcs.add(this.roughGetTile(root, srcPosition));
        }

        //inflate sources
        SimpleRecycler<T> tileRecycler = this.world.mode().tileRecycler();
        T[] srcs = uncheckedCast(this.world.mode().tileArray(compressedSrcs.size()));
        for (int i = 0; i < compressedSrcs.size(); i++) {
            Compressed<POS, T> compressedSrc = compressedSrcs.get(i);
            compressedSrc.readLock().lock();
            srcs[i] = compressedSrc.inflate(tileRecycler);
        }

        T dst = tileRecycler.allocate();
        compressedTile.writeLock().lock();
        try {
            if (compressedTile.timestamp() >= newTimestamp) {
                return compressedTile;
            }

            //actually do scaling
            long extra = this.world.scaler().scale(srcs, dst);
            if (compressedTile.set(newTimestamp, dst, extra)) {
                this.world.tileChanged(compressedTile, allowScale);
            }
        } finally {
            compressedTile.writeLock().unlock();
            tileRecycler.release(dst);

            for (int i = 0; i < compressedSrcs.size(); i++) {
                compressedSrcs.get(i).readLock().unlock();
                if (srcs[i] != null) {
                    tileRecycler.release(srcs[i]);
                }
            }
        }

        return compressedTile;
    }
}
